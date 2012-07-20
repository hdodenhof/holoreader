package de.hdodenhof.feedreader.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.misc.ArticleViewPager;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.MarkReadRunnable;
import de.hdodenhof.feedreader.misc.OnArticleChangedListener;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class DisplayArticleActivity extends SherlockFragmentActivity implements FragmentCallback, OnArticleChangedListener {

    @SuppressWarnings("unused")
    private static final String TAG = DisplayArticleActivity.class.getSimpleName();

    private int mArticleID;
    private int mFeedID;
    private int mCurrentArticle;

    /**
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
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

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setTitle(queryFeedName(mFeedID));
        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * @see android.support.v4.app.FragmentActivity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mCurrentArticle != -1) {
            new Thread(new MarkReadRunnable((Context) this, mCurrentArticle)).start();
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
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#isPrimaryFragment(android.support.v4.app.Fragment)
     */
    public boolean isPrimaryFragment(Fragment fragment) {
        return true;
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
        // MenuInflater mMenuInflater = getSupportMenuInflater();
        // mMenuInflater.inflate(R.menu.settings, menu);
        // return true;
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * @see de.hdodenhof.feedreader.misc.OnArticleChangedListener
     */
    public void onArticleChanged(int oldArticle, int currentArticle, int position) {
        if (oldArticle != -1) {
            new Thread(new MarkReadRunnable((Context) this, oldArticle)).start();
        }

        mCurrentArticle = currentArticle;
    }

}
