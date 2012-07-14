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

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.misc.ArticleOnPageChangeListener;
import de.hdodenhof.feedreader.misc.ArticleViewPager;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.MarkReadRunnable;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class DisplayArticleActivity extends FragmentActivity implements FragmentCallback, ArticleOnPageChangeListener {

    @SuppressWarnings("unused")
    private static final String TAG = DisplayArticleActivity.class.getSimpleName();

    private int mArticleID;
    private int mFeedID;

    /**
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {

        }

        setContentView(R.layout.activity_article);

        if (!getIntent().hasExtra("articleid")) {
            Intent mIntent = new Intent(this, HomeActivity.class);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mIntent);
        } else {
            mArticleID = getIntent().getIntExtra("articleid", 0);
            mFeedID = queryFeedID(mArticleID);
        }

        new ArticleViewPager(this);

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
     * 
     * @param articleID
     * @return
     */
    private int queryFeedID(int articleID) {
        Uri mBaseUri = Uri.withAppendedPath(RSSContentProvider.URI_ARTICLES, String.valueOf(articleID));
        String[] mProjection = { ArticleDAO._ID, ArticleDAO.FEEDID };

        Cursor mCursor = getContentResolver().query(mBaseUri, mProjection, null, null, null);
        mCursor.moveToFirst();
        int mFeedID = mCursor.getInt(mCursor.getColumnIndex(ArticleDAO.FEEDID));
        mCursor.close();

        return mFeedID;
    }

    /**
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#onFragmentReady(android.support.v4.app.Fragment)
     */
    public void onFragmentReady(Fragment fragment) {
    }

    /**
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#isDualPane()
     */
    public boolean isDualPane() {
        return false;
    }

    /**
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent mIntent = new Intent(this, DisplayFeedActivity.class);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mIntent.putExtra("feedid", mFeedID);
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
     * @see de.hdodenhof.feedreader.misc.ArticleOnPageChangeListener# onArticleChanged(int)
     */
    public void onArticleChanged(int articleID, int position) {
        new Thread(new MarkReadRunnable((Context) this, articleID)).start();
    }

}
