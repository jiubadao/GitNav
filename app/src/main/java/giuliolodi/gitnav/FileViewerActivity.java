/*
 * Copyright 2016 GLodi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package giuliolodi.gitnav;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.ContentsService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FileViewerActivity extends BaseDrawerActivity {

    @BindView(R.id.file_viewer_activity_textview) TextView textView;
    @BindView(R.id.file_viewer_activity_progressbar) ProgressBar progressBar;
    @BindString(R.string.network_error) String network_error;
    @BindString(R.string.file) String file;

    private Menu menu;
    private Intent intent;
    private String owner, repo, path, fileDecoded;
    private ContentsService contentsService = new ContentsService();

    private Observable<List<RepositoryContents>> observable;
    private Observer<List<RepositoryContents>> observer;
    private Subscription subscription;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.file_viewer_activity, frameLayout);

        ButterKnife.bind(this);

        getSupportActionBar().setTitle(file);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        intent = getIntent();
        owner = intent.getExtras().getString("owner");
        repo = intent.getExtras().getString("repo");
        path = intent.getExtras().getString("path");

        progressBar.setVisibility(View.VISIBLE);
        contentsService.getClient().setOAuth2Token(Constants.getToken(getApplicationContext()));

        observable = observable.create(new Observable.OnSubscribe<List<RepositoryContents>>() {
            @Override
            public void call(Subscriber<? super List<RepositoryContents>> subscriber) {
                try {
                    subscriber.onNext(contentsService.getContents(new RepositoryId(owner, repo), path));
                } catch (IOException e) {e.printStackTrace();}
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());

        observer = new Observer<List<RepositoryContents>>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(List<RepositoryContents> repositoryContentsList) {
                progressBar.setVisibility(View.INVISIBLE);
                try {
                    fileDecoded = new String(Base64.decode(repositoryContentsList.get(0).getContent(), Base64.DEFAULT), "UTF-8");
                } catch (UnsupportedEncodingException e) {e.printStackTrace();}
                textView.setText(fileDecoded);
            }
        };

        subscription = observable.subscribe(observer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (subscription != null && !subscription.isUnsubscribed())
            subscription.unsubscribe();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        return true;
    }

    private void createOptionsMenu() {
        getMenuInflater().inflate(R.menu.file_viewer_activity_menu, menu);
        super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Constants.isNetworkAvailable(getApplicationContext())) {
            switch (item.getItemId()) {
                case R.id.action_options:
                    startActivity(new Intent(getApplicationContext(), OptionActivity.class));
                    overridePendingTransition(0,0);
                    return true;
                case R.id.open_in_broswer:
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(""));
                    startActivity(browserIntent);
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        } else
            Toast.makeText(getApplicationContext(), network_error, Toast.LENGTH_LONG).show();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0,0);
    }
}
