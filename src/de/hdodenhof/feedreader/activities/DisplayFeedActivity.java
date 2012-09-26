package de.hdodenhof.feedreader.activities;

import java.util.ArrayList;

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
import de.hdodenhof.feedreader.misc.Helpers;
import de.hdodenhof.feedreader.misc.MarkReadRunnable;
import de.hdodenhof.feedreader.misc.OnArticleChangedListener;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class DisplayFeedActivity extends SherlockFragmentActivity implements FragmentCallback, OnArticleChangedListener, OnItemClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = DisplayFeedActivity.class.getSimpleName();

    private ArticleViewPager mViewPager;
    private SharedPreferences mPreferences;
    private Resources mResources;
    private MenuItem mWebLink;
    private boolean mWebLinkHide = false;
    private boolean mTwoPane = false;
    private boolean mUnreadOnly = true;
    private int mArticleID = -1;
    private int mFeedID = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra("feedid")) {
            mFeedID = getIntent().getIntExtra("feedid", -1);
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUnreadOnly = mPreferences.getBoolean("unreadonly", true);

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
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#onFragmentReady(android.support.v4.app.Fragment) )
     */
    @Override
    public void onFragmentReady(Fragment fragment) {
        if (mTwoPane && fragment instanceof ArticleListFragment) {
            ((ArticleListFragment) fragment).setChoiceModeSingle();
        }
    }

    /**
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#isDualPane()
     */
    @Override
    public boolean isDualPane() {
        return mTwoPane;
    }

    /**
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#isPrimaryFragment(android.support.v4.app.Fragment)
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
                item.setIcon(R.drawable.ab_btn_checkbox_unchecked);
            } else {
                Toast.makeText(this, mResources.getString(R.string.ToastAllArticles), Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.ab_btn_checkbox_checked);
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
            if (!mUnreadOnly) {
                menu.getItem(1).setIcon(R.drawable.ab_btn_checkbox_checked);
            }
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

                ArrayList<String> articles = new ArrayList<String>();

                cursor.moveToFirst();
                do {
                    articles.add(cursor.getString(cursor.getColumnIndex(ArticleDAO._ID)));
                } while (cursor.moveToNext());

                Intent intent = new Intent(this, DisplayArticleActivity.class);
                intent.putExtra("articleid", articleID);
                intent.putExtra("feedid", mFeedID);
                intent.putStringArrayListExtra("articles", articles);
                startActivity(intent);
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
