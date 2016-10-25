/*
 * MIT License
 *
 * Copyright (c) 2016 GLodi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package giuliolodi.gitnav;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.vstechlab.easyfonts.EasyFonts;

import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.ViewPagerHelper;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.CommonPagerTitleView;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.UserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserFragment extends Fragment {

    @BindView(R.id.user_fragment_layout) RelativeLayout relativeLayout;
    @BindView(R.id.user_fragment_name) TextView username;
    @BindView(R.id.user_fragment_description) TextView user_bio;
    @BindView(R.id.user_fragment_image) CircleImageView user_image;
    @BindView(R.id.user_fragment_progress_bar) ProgressBar progressBar;
    @BindView(R.id.user_fragment_login) TextView login;

    @BindString(R.string.network_error) String network_error;
    @BindString(R.string.user_followed) String user_followed;
    @BindString(R.string.user_unfollowed) String user_unfollowed;
    @BindString(R.string.profile) String profile;

    public User user;
    private ViewPager mViewPager;
    private Context context;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private List<Integer> views;
    private UserService userService;

    private boolean IS_FOLLOWED = false;
    private boolean IS_AUTHD_USER_CALLED = false;

    private View v;

    /*
        Most of UI items are created onPostExecute() in getUser()
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.user_fragment, container, false);
        ButterKnife.bind(this, v);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(profile);

        sp = PreferenceManager.getDefaultSharedPreferences(getContext());

        mViewPager = (ViewPager) v.findViewById(R.id.vp);

        username.setTypeface(EasyFonts.robotoRegular(getContext()));
        user_bio.setTypeface(EasyFonts.robotoRegular(getContext()));
        login.setTypeface(EasyFonts.robotoRegular(getContext()));

        // Setup adapter for ViewPager. It will handle the three fragments below.
        views = new ArrayList<>();
        views.add(R.layout.user_fragment_repos);
        views.add(R.layout.user_fragment_followers);
        views.add(R.layout.user_fragment_following);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public boolean isViewFromObject(final View view, final Object object) {
                return view.equals(object);
            }

            @Override
            public void destroyItem(View container, int position, Object object) {
                ((ViewPager) container).removeView((View) object);
            }

            @Override
            public Object instantiateItem(final ViewGroup container, final int position) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                ViewGroup layout = (ViewGroup) inflater.inflate(views.get(position), container, false);
                container.addView(layout);
                return layout;
            }
        });

        // If user goes back to UserFragment from another fragment, the tile is changed accordingly
        getFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    public void onBackStackChanged() {
                        if (((AppCompatActivity) getActivity()) != null)
                            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(profile);
                    }
                });

        if (Constants.isNetworkAvailable(getContext())) {
            if (!IS_AUTHD_USER_CALLED)
                new getUser().execute();
        }
        else
            Toast.makeText(getContext(), network_error, Toast.LENGTH_LONG).show();

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        /*
            If setAuthdUser was called, prevent from getting StarredFragment menu item reference:
            wouldn't exist, throws NullPointer.
          */
        if (!IS_AUTHD_USER_CALLED && menu.findItem(R.id.sort_icon) != null || user.getLogin().equals(Constants.getUsername(getContext()))) {
            if (menu.findItem(R.id.sort_icon) != null && menu.findItem(R.id.sort_icon).isVisible())
                menu.findItem(R.id.sort_icon).setVisible(false);
            if (menu.findItem(R.id.unfollow) != null && menu.findItem(R.id.unfollow).isVisible())
                menu.findItem(R.id.unfollow).setVisible(false);
        }

        // Show follow button only if user is not followed by authenticated user
        if (!IS_FOLLOWED && !IS_AUTHD_USER_CALLED
                && !user.getLogin().equals(Constants.getUsername(getContext()))
                && menu.findItem(R.id.follow_icon) == null) {
            inflater.inflate(R.menu.user_fragment_follow_button, menu);
            menu.findItem(R.id.unfollow).setVisible(false);
        }

        // Show unfollow button
        if (IS_FOLLOWED && !IS_AUTHD_USER_CALLED
                && menu.findItem(R.id.follow_icon) == null) {
            menu.findItem(R.id.unfollow).setVisible(true);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Constants.isNetworkAvailable(getContext())) {
            switch (item.getItemId()) {
                case R.id.follow_icon:
                    new followUser().execute();
                    return true;
                case R.id.unfollow:
                    new unfollowUser().execute();
                    return true;
            }
        }
        else
            Toast.makeText(context, network_error, Toast.LENGTH_LONG).show();

        return super.onOptionsItemSelected(item);
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setAuthdUser(Context context) {
        this.context = context;
        if (Constants.isNetworkAvailable(context))
            new getAuthdUser().execute();
        else
            Toast.makeText(context, network_error, Toast.LENGTH_LONG).show();
    }

    // Get User object after setUser(User user) is called
    class getUser extends AsyncTask<String,String,String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Show progress bar while information is downloaded
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            userService = new UserService();
            userService.getClient().setOAuth2Token(Constants.getToken(getContext()));
            try {
                user = userService.getUser(user.getLogin());
                // Check if authdUser is following user
                IS_FOLLOWED = userService.isFollowing(user.getLogin());
            } catch (IOException e) {e.printStackTrace();}

            /*
                Notify 3 fragments.
                This will pass to each fragment the User object that
                they will use to get info about Repos, Followers and Following.

             */
            UserFragmentRepos userFragmentRepos = new UserFragmentRepos();
            userFragmentRepos.populate(user.getLogin(), getContext(), getView());

            UserFragmentFollowers userFragmentFollowers = new UserFragmentFollowers();
            userFragmentFollowers.populate(user.getLogin(), getContext(), getView(), getFragmentManager());

            UserFragmentFollowing userFragmentFollowing = new UserFragmentFollowing();
            userFragmentFollowing.populate(user.getLogin(), getContext(), getView(), getFragmentManager());

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            // Download and show information
            Picasso.with(getContext()).load(user.getAvatarUrl()).resize(150, 150).centerCrop().into(user_image);

            // If user's name is available show both login and username. Otherwise show only login
            if (user.getName() != null) {
                username.setText(user.getName());
                login.setText("@" + user.getLogin());
            }
            else {
                username.setText(user.getLogin());
                login.setVisibility(View.GONE);
            }
            if (user.getBio() == null || user.getBio().equals(""))
                user_bio.setVisibility(View.GONE);
            user_bio.setText(user.getBio());

            // Setup lists for tabs
            final List<String> mTitleDataList = new ArrayList<>();
            mTitleDataList.add("REPOSITORIES");
            mTitleDataList.add("FOLLOWERS");
            mTitleDataList.add("FOLLOWING");

            final List<String> mNumberDataList = new ArrayList<>();
            mNumberDataList.add(String.valueOf(user.getPublicRepos()));
            mNumberDataList.add(String.valueOf(user.getFollowers()));
            mNumberDataList.add(String.valueOf(user.getFollowing()));

            // Setup navigator for tabs. It will set a view to the corresponding tab (Repos, Followers and Following).
            MagicIndicator magicIndicator = (MagicIndicator) v.findViewById(R.id.magic_indicator);
            CommonNavigator commonNavigator = new CommonNavigator(getContext());
            commonNavigator.setAdjustMode(true);
            commonNavigator.setAdapter(new CommonNavigatorAdapter() {
                @Override
                public int getCount() {
                    return mTitleDataList == null ? 0 : mTitleDataList.size();
                }

                @Override
                public IPagerTitleView getTitleView(Context context, final int index) {
                    CommonPagerTitleView commonPagerTitleView = new CommonPagerTitleView(context);

                    /*
                        Get reference of custom layout for three tabs.
                        This is due to the fact that default Pager doesn't allow multi-line
                        tabs. user_fragment_tab includes 2 TextViews one on top of the other.
                     */
                    View customLayout = LayoutInflater.from(context).inflate(R.layout.user_fragment_tab, null);
                    commonPagerTitleView.setContentView(customLayout);
                    final TextView user_fragment_tab_n = (TextView) customLayout.findViewById(R.id.user_fragment_tab_n);
                    final TextView user_fragment_tab_title = (TextView) customLayout.findViewById(R.id.user_fragment_tab_title);
                    user_fragment_tab_n.setText(mNumberDataList.get(index));
                    user_fragment_tab_title.setText(mTitleDataList.get(index));
                    user_fragment_tab_n.setTypeface(EasyFonts.robotoRegular(context));
                    user_fragment_tab_title.setTypeface(EasyFonts.robotoRegular(context));
                    commonPagerTitleView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mViewPager.setCurrentItem(index);
                        }
                    });
                    return commonPagerTitleView;
                }

                @Override
                public IPagerIndicator getIndicator(Context context) {
                    LinePagerIndicator indicator = new LinePagerIndicator(context);
                    indicator.setMode(LinePagerIndicator.MODE_MATCH_EDGE);
                    indicator.setStartInterpolator(new AccelerateInterpolator());
                    indicator.setColors(Color.parseColor("#448AFF"));
                    return indicator;
                }
            });

            // These two lines are required (see GitHub MagicIndicator
            magicIndicator.setNavigator(commonNavigator);
            ViewPagerHelper.bind(magicIndicator, mViewPager);

            // This allows to edit menu
            setHasOptionsMenu(true);

            // This will trigger onCreateOptionsMenu to create Follow icon
            getActivity().invalidateOptionsMenu();

            // Make progress bar invisible and layout visible
            progressBar.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.VISIBLE);
        }
    }

    /*
        AsyncTask for authenticated user. Used when user clicks on its own picture in nav drawer.
        After getting user info, checks if retrieved info equals what is stored in SharedPreferences.
        If that's not the case it updates the info.
     */
    class getAuthdUser extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            User authdUser;
            userService = new UserService();
            userService.getClient().setOAuth2Token(Constants.getToken(context));
            try {
                authdUser = userService.getUser();
                setUser(authdUser);
            } catch (IOException e) {e.printStackTrace();}

            editor = sp.edit();
            if (user.getName() != null && !user.getName().equals("") && !Constants.getFullName(context).equals(user.getName())) {
                editor.putString((Constants.getFullNameKey(context)), user.getName());
            }
            else if (user.getLogin() != null && !user.getLogin().equals("") && !Constants.getUsername(context).equals(user.getLogin())) {
                editor.putString((Constants.getUserKey(context)), user.getName());
            }
            else if (user.getEmail() != null && !user.getEmail().equals("") && !Constants.getEmail(context).equals(user.getEmail())) {
                editor.putString((Constants.getEmailKey(context)), user.getEmail());
            }
            editor.commit();
            Bitmap thumbnail = new ImageSaver(context)
                    .setFileName("thumbnail.png")
                    .setDirectoryName("images")
                    .load();
            try {
                Bitmap profile_picture = Picasso.with(context).load(user.getAvatarUrl()).get();
                if (!thumbnail.sameAs(profile_picture)) {
                    new ImageSaver(context)
                            .setFileName("thumbnail.png")
                            .setDirectoryName("images")
                            .save(profile_picture);
                }
            } catch (IOException e) {e.printStackTrace();}

            // Set IS_AUTHD_USER_CALLED so that fragment doesn't try to kill StarredFragment menu item
            IS_AUTHD_USER_CALLED = true;

            return null;
        }
    }

    class followUser extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                userService.follow(user.getLogin());
            } catch (IOException e) {e.printStackTrace();}
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(getContext(), user_followed, Toast.LENGTH_LONG).show();
        }
    }

    class unfollowUser extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                userService.unfollow(user.getLogin());
            } catch (IOException e) {e.printStackTrace();}
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String g = login.getText().toString();
            Toast.makeText(getContext(), user_unfollowed, Toast.LENGTH_LONG).show();
        }
    }

}
