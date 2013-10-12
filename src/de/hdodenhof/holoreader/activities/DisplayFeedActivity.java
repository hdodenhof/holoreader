package de.hdodenhof.holoreader.activities;

import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.fragments.ArticleListFragment;
import de.hdodenhof.holoreader.listadapters.RSSAdapter;
import de.hdodenhof.holoreader.listadapters.RSSArticleAdapter;
import de.hdodenhof.holoreader.misc.ArticleViewPager;
import de.hdodenhof.holoreader.misc.Extras;
import de.hdodenhof.holoreader.misc.FragmentCallback;
import de.hdodenhof.holoreader.misc.Helpers;
import de.hdodenhof.holoreader.misc.MarkReadRunnable;
import de.hdodenhof.holoreader.misc.OnArticleChangedListener;
import de.hdodenhof.holoreader.misc.Prefs;
import de.hdodenhof.holoreader.provider.SQLiteHelper.ArticleDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class DisplayFeedActivity extends HoloReaderActivity implements FragmentCallback, OnArticleChangedListener, OnItemClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = DisplayFeedActivity.class.getSimpleName();

    private ArticleViewPager mViewPager;
    private SharedPreferences mPreferences;
    private Resources mResources;
    private MenuItem mWebLink;
    private boolean mWebLinkHide = false;
    private boolean mTwoPane = false;
    private boolean mUnreadOnly = true;
    private Date mUnreadAfter;
    private int mArticleID = -1;
    private int mFeedID = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra(Extras.FEEDID)) {
            mFeedID = getIntent().getIntExtra(Extras.FEEDID, -1);
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUnreadOnly = mPreferences.getBoolean(Prefs.UNREAD_ONLY, true);
        mUnreadAfter = new Date();

        mResources = getResources();

        setContentView(R.layout.activity_feed);

        if (findViewById(R.id.viewpager_article) != null) {
            mTwoPane = true;
            mViewPager = new ArticleViewPager(this);
        }

        ActionBar actionBar = getSupportActionBar();
        if (mFeedID != -1) {
            actionBar.setTitle(Helpers.queryFeedName(getContentResolver(), mFeedID));
        } else {
            actionBar.setTitle(mResources.getText(R.string.AllFeeds));
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // dual pane only
        if (mArticleID != -1) {
            MarkReadRunnable markReadRunnable = new MarkReadRunnable((Context) this);
            markReadRunnable.setArticle(mArticleID);
            new Thread(markReadRunnable).start();
        }
    }

    /**
     * @see de.hdodenhof.holoreader.misc.FragmentCallback#onFragmentReady(android.support.v4.app.Fragment) )
     */
    @Override
    public void onFragmentReady(Fragment fragment) {
        if (mTwoPane && fragment instanceof ArticleListFragment) {
            ((ArticleListFragment) fragment).setChoiceModeSingle();
        }
    }

    /**
     * @see de.hdodenhof.holoreader.misc.FragmentCallback#isDualPane()
     */
    @Override
    public boolean isDualPane() {
        return mTwoPane;
    }

    /**
     * @see de.hdodenhof.holoreader.misc.FragmentCallback#isPrimaryFragment(android.support.v4.app.Fragment)
     */
    @Override
    public boolean isPrimaryFragment(Fragment fragment) {
        return fragment instanceof ArticleListFragment;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent homeActivityIntent = new Intent(this, HomeActivity.class);
        homeActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        switch (item.getItemId()) {
        case android.R.id.home:
            startActivity(homeActivityIntent);
            return true;
        case R.id.item_toggle:
            mUnreadOnly = !mUnreadOnly;

            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean("unreadonly", mUnreadOnly);
            editor.commit();

            if (mUnreadOnly) {
                Toast.makeText(this, mResources.getString(R.string.ToastUnreadArticles), Toast.LENGTH_SHORT).show();
                mUnreadAfter = new Date();
            } else {
                Toast.makeText(this, mResources.getString(R.string.ToastAllArticles), Toast.LENGTH_SHORT).show();
            }
            ArticleListFragment articleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
            articleListFragment.setUnreadOnly(mUnreadOnly);
            return true;
        case R.id.item_markread:
            if (mFeedID == -1) {
                new Thread(new MarkReadRunnable((Context) this)).start();
            } else {
                MarkReadRunnable markReadRunnable = new MarkReadRunnable((Context) this);
                markReadRunnable.setFeed(mFeedID);
                new Thread(markReadRunnable).start();
            }
            startActivity(homeActivityIntent);
            return true;
        case R.id.item_web:
            String url = mViewPager.getCurrentLink();
            Intent webintent = new Intent(Intent.ACTION_VIEW);
            webintent.setData(Uri.parse(url));
            startActivity(webintent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getSupportMenuInflater();
        menuInflater.inflate(R.menu.feed, menu);
        mWebLink = menu.getItem(0);

        if (mTwoPane) {
            menu.removeItem(R.id.item_toggle);
            if (mWebLinkHide) {
                mWebLink.setVisible(false);
            }
        } else {
            menu.removeItem(R.id.item_web);
        }

        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        RSSAdapter adapter = (RSSAdapter) parent.getAdapter();

        switch (adapter.getType()) {
        case RSSAdapter.TYPE_ARTICLE:
            if (mTwoPane) {
                mViewPager.changePosition(position);
            } else {
                Cursor cursor = ((RSSArticleAdapter) adapter).getCursor();
                cursor.moveToPosition(position);
                int articleID = cursor.getInt(cursor.getColumnIndex(ArticleDAO._ID));

                Intent intent = new Intent(this, DisplayArticleActivity.class);
                intent.putExtra(Extras.ARTICLEID, articleID);
                intent.putExtra(Extras.FEEDID, mFeedID);
                intent.putExtra(Extras.UNREAD_AFTER, mUnreadAfter);
                startActivity(intent);
            }
            break;
        default:
            break;
        }
    }

    /**
     * @see de.hdodenhof.holoreader.misc.OnArticleChangedListener#onArticleChanged(int)
     */
    public void onArticleChanged(int oldArticle, int currentArticle, int position) {
        ArticleListFragment articleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
        if (articleListFragment != null) {
            articleListFragment.changePosition(position);
        }

        if (oldArticle != -1) {
            MarkReadRunnable markReadRunnable = new MarkReadRunnable((Context) this);
            markReadRunnable.setArticle(oldArticle);
            new Thread(markReadRunnable).start();
        }

        if (mViewPager.getCurrentLink() == null) {
            if (mWebLink != null) {
                mWebLink.setVisible(false);
            } else {
                mWebLinkHide = true;
            }
        } else {
            if (mWebLink != null) {
                mWebLink.setVisible(true);
            }
        }

        mArticleID = currentArticle;
    }
}
