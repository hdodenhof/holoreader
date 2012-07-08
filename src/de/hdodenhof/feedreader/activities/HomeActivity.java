package de.hdodenhof.feedreader.activities;

import java.lang.ref.WeakReference;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.fragments.ArticleListFragment;
import de.hdodenhof.feedreader.fragments.FeedListFragment;
import de.hdodenhof.feedreader.helpers.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.helpers.SQLiteHelper.FeedDAO;
import de.hdodenhof.feedreader.listadapters.RSSAdapter;
import de.hdodenhof.feedreader.listadapters.RSSArticleAdapter;
import de.hdodenhof.feedreader.listadapters.RSSFeedAdapter;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.providers.RSSContentProvider;
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

        private boolean mTwoPane = false;
        private ProgressDialog mSpinner;
        private ProgressDialog mProgresBar;
        private ArticleListFragment mArticleListFragment;
        private FeedListFragment mFeedListFragment;

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
                                mTarget.callbackFeedAdded();
                                break;
                        case 3:
                                // refreshed feeds
                                mTarget.callbackFeedsRefreshed();
                                break;
                        case 9:
                                // refresh progress bar
                                mTarget.callbackUpdateProgress(msg.arg1);
                                break;
                        default:
                                break;
                        }
                }
        };

        /**
         * Update feed list and dismiss spinner after new feed has been added
         */
        private void callbackFeedAdded() {
             // TODO refresh seems unnecessary
                mFeedListFragment.refreshList();
                if (mTwoPane){
                        mArticleListFragment.refreshList();
                }
                mSpinner.dismiss();
        }

        /**
         * Update feed list and dismiss progress bar after feeds have been refreshed
         */
        private void callbackFeedsRefreshed() {
                // TODO refresh seems unnecessary
                mFeedListFragment.refreshList();
                if (mTwoPane){
                        mArticleListFragment.refreshList();
                }
                mProgresBar.dismiss();
        }

        /**
         * Update progress bar during feeds refresh
         */
        private void callbackUpdateProgress(int progress) {
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

                mFeedListFragment = (FeedListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_feedlist);
                mArticleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
                if (mArticleListFragment != null) {
                        mTwoPane = true;
                }

        }

        /**
         * @see de.hdodenhof.feedreader.misc.FragmentCallback#onFragmentReady(android.support.v4.app.Fragment)
         */
        public void onFragmentReady(Fragment fragment) {
                if (mTwoPane && fragment instanceof FeedListFragment) {
                        ((FeedListFragment) fragment).setChoiceModeSingle();
                }
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
                mProgresBar.setMax(queryFeedCount());
                mProgresBar.show();

                RefreshFeedsTask mRefreshFeedsTask = new RefreshFeedsTask(mAsyncHandler, this);
                mRefreshFeedsTask.execute();
        }
        
        private int queryFeedCount(){
                String[] mProjection = { FeedDAO._ID };

                Cursor mCursor = getContentResolver().query(RSSContentProvider.URI_FEEDS, mProjection, null, null, null);
                int mCount = mCursor.getCount();
                mCursor.close();

                return mCount;                
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
         * Updates all fragments or launches a new activity (depending on the activities current layout) whenever a feed or article in one of the fragments has
         * been clicked on
         * 
         * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
         */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RSSAdapter mAdapter = (RSSAdapter) parent.getAdapter();
                Cursor mCursor;
                int mFeedID;

                switch (mAdapter.getType()) {
                case RSSAdapter.TYPE_FEED:
                        mCursor = ((RSSFeedAdapter) mAdapter).getCursor();
                        mCursor.moveToPosition(position);
                        mFeedID = mCursor.getInt(mCursor.getColumnIndex(FeedDAO._ID));

                        if (mTwoPane) {
                                mArticleListFragment.selectFeed(mFeedID);
                        } else {
                                Intent mIntent = new Intent(this, DisplayFeedActivity.class);
                                mIntent.putExtra("feedid", mFeedID);
                                startActivity(mIntent);
                        }
                        break;

                case RSSAdapter.TYPE_ARTICLE:
                        mCursor = ((RSSArticleAdapter) mAdapter).getCursor();
                        mCursor.moveToPosition(position);
                        int mArticleID = mCursor.getInt(mCursor.getColumnIndex(ArticleDAO._ID));
                        mFeedID = mCursor.getInt(mCursor.getColumnIndex(ArticleDAO.FEEDID));

                        Intent mIntent = new Intent(this, DisplayFeedActivity.class);
                        mIntent.putExtra("articleid", mArticleID);
                        mIntent.putExtra("feedid", mFeedID);
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