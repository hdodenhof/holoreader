package de.hdodenhof.feedreader;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;
import de.hdodenhof.feedreader.fragments.DisplayArticlesFragment;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;

public class DisplayFeedActivity extends FragmentActivity implements DisplayArticlesFragment.OnArticleSelectedListener, DisplayArticlesFragment.ParameterProvider, DisplayArticleFragment.ParameterProvider {

    private boolean mDualFragments = false;
    private long feedId;
    private long articleId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {

        }

        if (!getIntent().hasExtra("feedid")) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);           
        } else {
            feedId = getIntent().getLongExtra("feedid", -1);
        }
        
        if (getIntent().hasExtra("articleid")) {
            articleId = getIntent().getLongExtra("articleid", -1);       
        }

        setContentView(R.layout.activity_feed);

        DisplayArticleFragment articleFragment = (DisplayArticleFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_article);
        if (articleFragment != null) {
            mDualFragments = true;
        }
        
        FeedController feedController = new FeedController(this);
        Feed feed = feedController.getFeed(feedId);
        
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(feed.getName());
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app, menu);
        return true;
    }

    public long getFeedId() {
        return this.feedId;
    }
    
    public long getArticleId() {
        return this.articleId;
    }
    

    public void articleSelected(int index, Article article) {

        if (mDualFragments) {
            DisplayArticleFragment articleFragment = (DisplayArticleFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_article);
            if (articleFragment == null || articleFragment.getShownIndex() != article.getId()) {
                articleFragment = DisplayArticleFragment.newInstance(article.getId());

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_article, articleFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

        } else {
            Intent intent = new Intent(this, DisplayArticleActivity.class);
            intent.putExtra("articleid", article.getId());
            startActivity(intent);
        }

    }
}
