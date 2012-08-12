package de.hdodenhof.feedreader.activities;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.fragments.ArticleListFragment;
import de.hdodenhof.feedreader.listadapters.RSSAdapter;
import de.hdodenhof.feedreader.listadapters.RSSArticleAdapter;
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
public class DisplayFeedActivity extends SherlockFragmentActivity implements FragmentCallback, OnArticleChangedListener, OnItemClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = DisplayFeedActivity.class.getSimpleName();
    private static final String PREFS_NAME = "Feedreader";

    private boolean mTwoPane = false;
    private boolean mUnreadOnly;
    private int mCurrentArticle = -1;
    private int mCurrentFeed = -1;
    private ArticleViewPager mArticlePagerFragment;
    private SharedPreferences mPreferences;
    private Resources mResources;

    /**
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {

        }

        mResources = getResources();

        if (getIntent().hasExtra("feedid")) {
            mCurrentFeed = getIntent().getIntExtra("feedid", -1);
        }

        mPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mUnreadOnly = mPreferences.getBoolean("unreadonly", true);

        setContentView(R.layout.activity_feed);

        if (findViewById(R.id.viewpager_article) != null) {
            mTwoPane = true;
            mArticlePagerFragment = new ArticleViewPager(this);
        }

        ActionBar mActionBar = getSupportActionBar();
        if (mCurrentFeed != -1) {
            mActionBar.setTitle(queryFeedName(mCurrentFeed));
        } else {
            mActionBar.setTitle(mResources.getText(R.string.AllFeeds));
        }
        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * @see android.support.v4.app.FragmentActivity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        // dual pane only
        if (mCurrentArticle != -1) {
            MarkReadRunnable mMarkReadRunnable = new MarkReadRunnable((Context) this);
            mMarkReadRunnable.setArticle(mCurrentArticle);
            new Thread(mMarkReadRunnable).start();
        }
    }

    /**
     * 
     * @param feedID
     * @return
     */
    private String queryFeedName(int feedID) {
        if (feedID != 0) {
            Uri mBaseUri = Uri.withAppendedPath(RSSContentProvider.URI_FEEDS, String.valueOf(feedID));
            String[] mProjection = { FeedDAO._ID, FeedDAO.NAME };

            Cursor mCursor = getContentResolver().query(mBaseUri, mProjection, null, null, null);
            mCursor.moveToFirst();
            String mFeedName = mCursor.getString(mCursor.getColumnIndex(FeedDAO.NAME));
            mCursor.close();

            return mFeedName;
        } else {
            return "";
        }
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
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#isPrimaryFragment(android.support.v4.app.Fragment)
     */
    public boolean isPrimaryFragment(Fragment fragment) {
        return fragment instanceof ArticleListFragment;
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
        case R.id.item_toggle:
            mUnreadOnly = !mUnreadOnly;

            SharedPreferences.Editor mEditor = mPreferences.edit();
            mEditor.putBoolean("unreadonly", mUnreadOnly);
            mEditor.commit();

            if (mUnreadOnly) {
                Toast.makeText(this, mResources.getString(R.string.ToastUnreadArticles), Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.checkbox_unchecked);
            } else {
                Toast.makeText(this, mResources.getString(R.string.ToastAllArticles), Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.checkbox_checked);
            }
            ArticleListFragment mArticleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
            mArticleListFragment.setUnreadOnly(mUnreadOnly);
            return true;
        case R.id.item_markread:
            if (!mTwoPane) {
                if (mCurrentFeed == -1) {
                    new Thread(new MarkReadRunnable((Context) this)).start();
                } else {
                    MarkReadRunnable mMarkReadRunnable = new MarkReadRunnable((Context) this);
                    mMarkReadRunnable.setFeed(mCurrentFeed);
                    new Thread(mMarkReadRunnable).start();
                }
            }
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
        MenuInflater mMenuInflater = getSupportMenuInflater();
        mMenuInflater.inflate(R.menu.feed, menu);

        if (mTwoPane) {
            menu.removeItem(R.id.item_toggle);
        } else if (!mUnreadOnly) {
            menu.getItem(0).setIcon(R.drawable.checkbox_checked);
        }

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

                ArrayList<String> mArticles = new ArrayList<String>();

                mCursor.moveToFirst();
                do {
                    mArticles.add(mCursor.getString(mCursor.getColumnIndex(ArticleDAO._ID)));
                } while (mCursor.moveToNext());

                Intent mIntent = new Intent(this, DisplayArticleActivity.class);
                mIntent.putExtra("articleid", mArticleID);
                mIntent.putExtra("feedid", mCurrentFeed);
                mIntent.putStringArrayListExtra("articles", mArticles);
                startActivity(mIntent);
            }
            break;
        default:
            break;
        }
    }

    /**
     * @see de.hdodenhof.feedreader.misc.OnArticleChangedListener#onArticleChanged(int)
     */
    public void onArticleChanged(int oldArticle, int currentArticle, int position) {
        ArticleListFragment mArticleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
        mArticleListFragment.changePosition(position);

        if (oldArticle != -1) {
            MarkReadRunnable mMarkReadRunnable = new MarkReadRunnable((Context) this);
            mMarkReadRunnable.setArticle(oldArticle);
            new Thread(mMarkReadRunnable).start();
        }
        mCurrentArticle = currentArticle;
    }
}
