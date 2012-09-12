package de.hdodenhof.feedreader.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.misc.ArticleViewPager;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.Helpers;
import de.hdodenhof.feedreader.misc.MarkReadRunnable;
import de.hdodenhof.feedreader.misc.OnArticleChangedListener;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class DisplayArticleActivity extends SherlockFragmentActivity implements FragmentCallback, OnArticleChangedListener {

    @SuppressWarnings("unused")
    private static final String TAG = DisplayArticleActivity.class.getSimpleName();

    ArticleViewPager mViewPager;
    private MenuItem mWebLink;
    private boolean mWebLinkHide = false;
    private int mFeedID;
    private int mArticleID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_article);

        if (!getIntent().hasExtra("articleid")) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            mFeedID = getIntent().getIntExtra("feedid", -1);
        }

        mViewPager = new ArticleViewPager(this);

        ActionBar actionBar = getSupportActionBar();
        if (mFeedID != -1) {
            actionBar.setTitle(Helpers.queryFeedName(getContentResolver(), mFeedID));
        } else {
            actionBar.setTitle(getResources().getText(R.string.AllFeeds));
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mArticleID != -1) {
            MarkReadRunnable markReadRunnable = new MarkReadRunnable((Context) this);
            markReadRunnable.setArticle(mArticleID);
            new Thread(markReadRunnable).start();
        }
    }

    /**
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#onFragmentReady(android.support.v4.app.Fragment)
     */
    @Override
    public void onFragmentReady(Fragment fragment) {
    }

    /**
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#isDualPane()
     */
    @Override
    public boolean isDualPane() {
        return false;
    }

    /**
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#isPrimaryFragment(android.support.v4.app.Fragment)
     */
    @Override
    public boolean isPrimaryFragment(Fragment fragment) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, DisplayFeedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("feedid", mFeedID);
            startActivity(intent);
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
        menuInflater.inflate(R.menu.article, menu);
        mWebLink = menu.getItem(0);
        if (mWebLinkHide) {
            mWebLink.setVisible(false);
        }

        return true;
    }

    /**
     * @see de.hdodenhof.feedreader.misc.OnArticleChangedListener
     */
    public void onArticleChanged(int oldArticle, int currentArticle, int position) {
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
