package de.hdodenhof.feedreader.activities;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.fragments.ArticleListFragment;
import de.hdodenhof.feedreader.helpers.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.helpers.SQLiteHelper.FeedDAO;
import de.hdodenhof.feedreader.listadapters.RSSAdapter;
import de.hdodenhof.feedreader.listadapters.RSSArticleAdapter;
import de.hdodenhof.feedreader.misc.ArticleOnPageChangeListener;
import de.hdodenhof.feedreader.misc.ArticleViewPager;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.MarkReadRunnable;
import de.hdodenhof.feedreader.providers.RSSContentProvider;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class DisplayFeedActivity extends FragmentActivity implements FragmentCallback, ArticleOnPageChangeListener, OnItemClickListener {

        @SuppressWarnings("unused")
        private static final String TAG = DisplayFeedActivity.class.getSimpleName();

        private boolean mTwoPane = false;
        private ArticleViewPager mArticlePagerFragment = null;

        /**
         * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
         */
        @SuppressLint("NewApi")
        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                int mFeedID = 0;

                if (savedInstanceState != null) {

                }

                if (!getIntent().hasExtra("feedid")) {
                        Intent mIntent = new Intent(this, HomeActivity.class);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mIntent);
                } else {
                        mFeedID = getIntent().getIntExtra("feedid", 0);
                }

                setContentView(R.layout.activity_feed);

                if (findViewById(R.id.viewpager_article) != null) {
                        mTwoPane = true;
                        mArticlePagerFragment = new ArticleViewPager(this);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        ActionBar mActionBar = getActionBar();
                        mActionBar.setTitle(queryFeedName(mFeedID));
                        mActionBar.setDisplayHomeAsUpEnabled(true);
                } else {
                        setTitle(queryFeedName(mFeedID));
                }
        }

        /**
         * 
         * @param feedID
         * @return
         */
        private String queryFeedName(int feedID) {
                Uri mBaseUri = Uri.withAppendedPath(RSSContentProvider.URI_FEEDS, String.valueOf(feedID));
                String[] mProjection = { FeedDAO._ID, FeedDAO.NAME };

                Cursor mCursor = getContentResolver().query(mBaseUri, mProjection, null, null, null);
                mCursor.moveToFirst();
                String mFeedName = mCursor.getString(mCursor.getColumnIndex(FeedDAO.NAME));
                mCursor.close();

                return mFeedName;
        }

        /**
         * @see de.hdodenhof.feedreader.misc.FragmentCallback#onFragmentReady(android.support.v4.app.Fragment) )
         */
        public void onFragmentReady(Fragment fragment) {
                if (mTwoPane && fragment instanceof ArticleListFragment) {
                        ((ArticleListFragment) fragment).setChoiceModeSingle();
                }
        }

        /**
         * @see de.hdodenhof.feedreader.misc.FragmentCallback#isDualPane()
         */
        public boolean isDualPane() {
                return mTwoPane;
        }

        /**
         * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
         */
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                case android.R.id.home:
                        Intent mIntent = new Intent(this, HomeActivity.class);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mIntent);
                        return true;
                default:
                        return super.onOptionsItemSelected(item);
                }
        }

        /**
         * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
         */
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                MenuInflater mMenuInflater = getMenuInflater();
                mMenuInflater.inflate(R.menu.settings, menu);
                return true;
        }

        /**
         * Updates all fragments or launches a new activity (depending on the activities current layout) whenever an article in the ArticleListFragment has been
         * clicked
         * 
         * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
         */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RSSAdapter mAdapter = (RSSAdapter) parent.getAdapter();

                switch (mAdapter.getType()) {
                case RSSAdapter.TYPE_ARTICLE:
                        if (mTwoPane) {
                                mArticlePagerFragment.changePosition(position);
                        } else {
                                Cursor mCursor = ((RSSArticleAdapter) mAdapter).getCursor();
                                mCursor.moveToPosition(position);
                                int mArticleID = mCursor.getInt(mCursor.getColumnIndex(ArticleDAO._ID));

                                Intent mIntent = new Intent(this, DisplayArticleActivity.class);
                                mIntent.putExtra("articleid", mArticleID);
                                startActivity(mIntent);
                        }
                        break;
                default:
                        break;
                }
        }

        /**
         * @see de.hdodenhof.feedreader.misc.ArticleOnPageChangeListener#onArticleChanged(int)
         */
        public void onArticleChanged(int articleID, int position) {
                ArticleListFragment mArticleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
                mArticleListFragment.changePosition(position);

                new Thread(new MarkReadRunnable((Context) this, articleID)).start();
        }
}
