package giuliolodi.githubnav;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.egit.github.core.Authorization;
import org.eclipse.egit.github.core.service.OAuthService;

import java.io.IOException;
import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    public OAuthService oAuthService;
    public SharedPreferences sp;
    public SharedPreferences.Editor editor;

    public String inputUser;
    public String inputPass;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        sp = PreferenceManager.getDefaultSharedPreferences(this);

        final EditText input_user = (EditText) findViewById(R.id.input_user);
        final EditText input_password = (EditText) findViewById(R.id.input_password);
        final Button button = (Button) findViewById(R.id.btn_login);

        input_password.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    inputUser = input_user.getText().toString();
                    inputPass = input_password.getText().toString();

                    new newAccess().execute();

                    // TODO: Intent to Main Activity

                    return true;
                }
                return false;
            }
        });

        // TODO: check if old credentials work and Intent to Main Activity

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputUser = input_user.getText().toString();
                inputPass = input_password.getText().toString();

                new newAccess().execute();

                // TODO: Intent to Main Activity
            }
        });

    }

    class newAccess extends AsyncTask<String, String, String> {

        ProgressDialog progDailog = new ProgressDialog(LoginActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progDailog.setMessage("Logging in");
            progDailog.setIndeterminate(false);
            progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDailog.setCancelable(true);
            progDailog.show();
        }
        @Override
        protected String doInBackground(String... back) {
            oAuthService = new OAuthService();
            oAuthService.getClient().setCredentials(inputUser, inputPass);

            Authorization auth = new Authorization();
            auth.setScopes(Arrays.asList("repo", "gist", "user"));
            auth.setNote("GitHubNav");

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            String error = "";
            editor = sp.edit();

            try {
                auth = oAuthService.createAuthorization(auth);
                if (auth.getToken() != "") {
                    editor.putString(Constants.tokenKey(getApplicationContext()), auth.getToken());
                    editor.commit();
                }
            } catch (IOException e) { error = e.getMessage(); }

            if (error == "") {
                editor.putString(Constants.userKey(getApplicationContext()), inputUser);
                editor.putBoolean(Constants.authdKey(getApplicationContext()), true);
                editor.commit();
                return "Logged in";
            }
            else {
                return error;
            }
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progDailog.dismiss();
            Toast toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG);
            toast.show();
        }

    }

}