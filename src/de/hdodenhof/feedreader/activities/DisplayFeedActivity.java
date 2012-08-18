package de.hdodenhof.feedreader.activities;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
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
    private static final String PREFS_NAME = "Feedreader";

    private boolean mTwoPane = false;
    private boolean mUnreadOnly;
    private int mCurrentArticle = -1;
    private int mCurrentFeed = -1;
    private ArticleViewPager mArticlePagerFragment;
    private SharedPreferences mPreferences;
    private Resources resources;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        resources = getResources();

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

        ActionBar actionBar = getSupportActionBar();
        if (mCurrentFeed != -1) {
            actionBar.setTitle(Helpers.queryFeedName(getContentResolver(), mCurrentFeed));
        } else {
            actionBar.setTitle(resources.getText(R.string.AllFeeds));
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // dual pane only
        if (mCurrentArticle != -1) {
            MarkReadRunnable markReadRunnable = new MarkReadRunnable((Context) this);
            markReadRunnable.setArticle(mCurrentArticle);
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
        Intent intent;
        switch (item.getItemId()) {
        case android.R.id.home:
            intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        case R.id.item_toggle:
            mUnreadOnly = !mUnreadOnly;

            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean("unreadonly", mUnreadOnly);
            editor.commit();

            if (mUnreadOnly) {
                Toast.makeText(this, resources.getString(R.string.ToastUnreadArticles), Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.checkbox_unchecked);
            } else {
                Toast.makeText(this, resources.getString(R.string.ToastAllArticles), Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.checkbox_checked);
            }
            ArticleListFragment articleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
            articleListFragment.setUnreadOnly(mUnreadOnly);
            return true;
        case R.id.item_markread:
            if (!mTwoPane) {
                if (mCurrentFeed == -1) {
                    new Thread(new MarkReadRunnable((Context) this)).start();
                } else {
                    MarkReadRunnable markReadRunnable = new MarkReadRunnable((Context) this);
                    markReadRunnable.setFeed(mCurrentFeed);
                    new Thread(markReadRunnable).start();
                }
                intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getSupportMenuInflater();
        menuInflater.inflate(R.menu.feed, menu);

        if (mTwoPane) {
            menu.removeItem(R.id.item_toggle);
        } else if (!mUnreadOnly) {
            menu.getItem(0).setIcon(R.drawable.checkbox_checked);
        }

        return true;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        RSSAdapter adapter = (RSSAdapter) parent.getAdapter();

        switch (adapter.getType()) {
        case RSSAdapter.TYPE_ARTICLE:
            if (mTwoPane) {
                mArticlePagerFragment.changePosition(position);
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
                intent.putExtra("feedid", mCurrentFeed);
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
        articleListFragment.changePosition(position);

        if (oldArticle != -1) {
            MarkReadRunnable markReadRunnable = new MarkReadRunnable((Context) this);
            markReadRunnable.setArticle(oldArticle);
            new Thread(markReadRunnable).start();
        }
        mCurrentArticle = currentArticle;
    }
}
