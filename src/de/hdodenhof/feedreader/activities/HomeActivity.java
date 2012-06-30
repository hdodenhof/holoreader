package de.hdodenhof.feedreader.activities;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.adapters.RSSAdapter;
import de.hdodenhof.feedreader.controllers.RSSController;
import de.hdodenhof.feedreader.fragments.ArticleListFragment;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.RSSMessage;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.models.Feed;
import de.hdodenhof.feedreader.runnables.SendMessageRunnable;
import de.hdodenhof.feedreader.tasks.AddFeedTask;
import de.hdodenhof.feedreader.tasks.RefreshFeedsTask;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class HomeActivity extends FragmentActivity implements FragmentCallback, OnItemClickListener {

        @SuppressWarnings("unused")
        private static final String TAG = FragmentActivity.class.getSimpleName();

        private ArrayList<Handler> mHandlers = new ArrayList<Handler>();
        private boolean mTwoPane = false;
        private ProgressDialog mSpinner;
        private ProgressDialog mProgresBar;
        private RSSController mController;
        private ArrayList<Feed> mFeeds;

        /**
         * Handles messages from AsyncTasks started within this activity
         */
        Handler mAsyncHandler = new AsynHandler(this);

        private static class AsynHandler extends Handler {
                private final WeakReference<HomeActivity> mTargetReference;

                AsynHandler(HomeActivity target) {
                        mTargetReference = new WeakReference<HomeActivity>(target);
                }

                public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        HomeActivity mTarget = mTargetReference.get();

                        switch (msg.what) {
                        case 1:
                                // added feed
                                mTarget.feedAdded();
                                break;
                        case 2:
                                // updated single feed
                                mTarget.feedUpdated();
                                break;
                        case 3:
                                // refreshed feeds
                                mTarget.feedsRefreshed();
                        case 9:
                                // refresh progress bar
                                mTarget.updateProgress(msg.arg1);
                        default:
                                break;
                        }
                }
        };

        /**
         * Update feed list and dismiss spinner after new feed has been added
         */
        private void feedAdded() {
                reloadFeeds();
                mSpinner.dismiss();
        }

        /**
         * Dismiss spinner after feed has been updated
         */
        private void feedUpdated() {
                mSpinner.dismiss();
        }

        /**
         * Update feed list and dismiss progress bar after feeds have been
         * refreshed
         */
        private void feedsRefreshed() {
                reloadFeeds();
                mProgresBar.dismiss();
        }

        /**
         * Update progress bar during feeds refresh
         */
        private void updateProgress(int progress) {
                mProgresBar.setProgress(progress);
        }

        /**
         * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                if (savedInstanceState != null) {

                }

                setContentView(R.layout.activity_home);

                mController = new RSSController(this);
                mFeeds = mController.getFeeds();

                ArticleListFragment mArticleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
                if (mArticleListFragment != null) {
                        mTwoPane = true;
                }

        }

        /**
         * Reloads all feeds from the database and sends them to all fragments
         */
        private void reloadFeeds() {
                mFeeds = mController.getFeeds();

                RSSMessage mMessage = new RSSMessage();
                mMessage.type = RSSMessage.FEEDLIST_UPDATED;
                mMessage.feeds = mFeeds;

                new Thread(new SendMessageRunnable(mHandlers, mMessage, 0)).start();
        }

        /**
         * @see de.hdodenhof.feedreader.misc.FragmentCallback#onFragmentReady(android.os.Handler)
         */
        public void onFragmentReady(Handler handler) {
                mHandlers.add(handler);
                RSSMessage mMessage;

                if (mTwoPane) {
                        mMessage = new RSSMessage();
                        mMessage.type = RSSMessage.CHOICE_MODE_SINGLE_FEED;

                        // TODO only set CHOICE_MODE_SINGLE on feed list and not
                        // on article list
                        new Thread(new SendMessageRunnable(mHandlers, mMessage, 0)).start();
                }

                mMessage = new RSSMessage();
                mMessage.type = RSSMessage.INITIALIZE;
                mMessage.feeds = mFeeds;

                new Thread(new SendMessageRunnable(mHandlers, mMessage, 0)).start();
        }

        /**
         * @see de.hdodenhof.feedreader.misc.FragmentCallback#isDualPane()
         */
        public boolean isDualPane() {
                return mTwoPane;
        }

        /**
         * Starts an AsyncTask to fetch a new feed and add it to the database
         * 
         * @param feedUrl
         *                URL of the feed to fetch
         */
        private void addFeed(String feedUrl) {
                mSpinner = ProgressDialog.show(this, "", "Please wait...", true);
                AddFeedTask mAddFeedTask = new AddFeedTask(mAsyncHandler, this);
                mAddFeedTask.execute(feedUrl);
        }

        /**
         * Starts an AsyncTask to refresh all feeds currently in the database
         */
        private void refreshFeeds() {
                mProgresBar = new ProgressDialog(this);
                mProgresBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgresBar.setMessage("Loading...");
                mProgresBar.setCancelable(false);
                mProgresBar.setProgress(0);
                mProgresBar.setMax(mFeeds.size());
                mProgresBar.show();

                RefreshFeedsTask mRefreshFeedsTask = new RefreshFeedsTask(mAsyncHandler, this);
                mRefreshFeedsTask.execute();
        }

        /**
         * Shows a dialog to add a new feed URL
         */
        private void showAddDialog() {
                AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);

                mAlertDialog.setTitle("Add feed");
                mAlertDialog.setMessage("Input Feed URL");

                final EditText mInput = new EditText(this);
                mInput.setText("http://t3n.de/news/feed");
                mAlertDialog.setView(mInput);

                mAlertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                                String value = mInput.getText().toString();
                                addFeed(value);
                        }
                });

                mAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                });

                mAlertDialog.show();
        }

        /**
         * Updates all fragments or launches a new activity (depending on the
         * activities current layout) whenever a feed or article in one of the
         * fragments has been clicked on
         * 
         * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView,
         *      android.view.View, int, long)
         */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RSSAdapter adapter = (RSSAdapter) parent.getAdapter();

                switch (adapter.getType()) {
                case RSSAdapter.TYPE_FEED:
                        Feed mFeed = (Feed) parent.getItemAtPosition(position);

                        if (!mTwoPane) {
                                Intent mIntent = new Intent(this, DisplayFeedActivity.class);
                                mIntent.putExtra("feedid", mFeed.getId());
                                startActivity(mIntent);
                        } else {
                                RSSMessage mMessage = new RSSMessage();
                                mMessage.type = RSSMessage.FEED_SELECTED;
                                mMessage.feeds = mFeeds;
                                mMessage.feed = mFeed;

                                new Thread(new SendMessageRunnable(mHandlers, mMessage, 0)).start();
                        }
                        break;

                case RSSAdapter.TYPE_ARTICLE:
                        Article mArticle = (Article) parent.getItemAtPosition(position);

                        Intent mIntent = new Intent(this, DisplayFeedActivity.class);
                        mIntent.putExtra("articleid", mArticle.getId());
                        mIntent.putExtra("feedid", mArticle.getFeedId());
                        startActivity(mIntent);

                        break;

                default:
                        break;
                }

        }

        /**
         * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
         */
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                MenuInflater mMenuInflater = getMenuInflater();
                mMenuInflater.inflate(R.menu.main, menu);
                return true;
        }

        /**
         * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
         */
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                case R.id.item_refresh:
                        refreshFeeds();
                        return true;
                case R.id.item_add:
                        showAddDialog();
                        return true;
                default:
                        return super.onOptionsItemSelected(item);
                }
        }
}