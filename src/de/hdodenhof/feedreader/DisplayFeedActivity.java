package de.hdodenhof.feedreader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import de.hdodenhof.feedreader.adapter.ArticlePagerAdapter;
import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;
import de.hdodenhof.feedreader.fragments.DisplayArticlesFragment;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;

public class DisplayFeedActivity extends FragmentActivity implements DisplayArticlesFragment.OnArticleSelectedListener,
        DisplayArticlesFragment.ParameterProvider, DisplayArticleFragment.ParameterProvider, OnPageChangeListener {

    private boolean mDualFragments = false;
    private long feedId;
    private long articleId;
    private ArticlePagerAdapter mPagerAdapter;
    private ViewPager pager;
    private Map<Long, Integer> articleMap;
    private boolean feedViewReady = false;

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

        View articleFragment = findViewById(R.id.viewpager);
        if (articleFragment != null) {
            mDualFragments = true;
        }

        if (mDualFragments) {
            this.initialisePaging();

            DisplayArticlesFragment displayArticlesFragment = (DisplayArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_feed);
            displayArticlesFragment.setChoiceModeSingle();

            // FIXME
            feedViewReady = true;
        }

        FeedController feedController = new FeedController(this);
        Feed feed = feedController.getFeed(feedId);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(feed.getName());
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    private void initialisePaging() {

        List<DisplayArticleFragment> fragments = new Vector<DisplayArticleFragment>();
        List<String> titles = new Vector<String>();

        ArticleController articleController = new ArticleController(this);

        List<Article> articles = articleController.getAllArticles(feedId);
        int pos = 0;
        int curr = 0;
        articleMap = new HashMap<Long, Integer>();

        for (Article article : articles) {
            fragments.add(DisplayArticleFragment.newInstance(article.getId()));
            titles.add(article.getTitle());
            if (article.getId() == articleId) {
                curr = pos;
            }
            articleMap.put(article.getId(), pos);
            pos++;
        }

        this.mPagerAdapter = new ArticlePagerAdapter(getSupportFragmentManager(), fragments, titles);

        pager = (ViewPager) findViewById(R.id.viewpager);
        pager.setAdapter(this.mPagerAdapter);
        pager.setOnPageChangeListener(this);
        pager.setCurrentItem(curr);
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

    public int getArticlePosition() {
        if (mDualFragments) {
            return (Integer) articleMap.get(this.articleId);
        } else {
            return -1;
        }
    }

    public void articleSelected(int index, Article article) {

        if (mDualFragments) {
            pager.setCurrentItem((Integer) articleMap.get(article.getId()));

        } else {
            Intent intent = new Intent(this, DisplayArticleActivity.class);
            intent.putExtra("articleid", article.getId());
            startActivity(intent);
        }

    }

    public void onPageScrollStateChanged(int state) {
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageSelected(int position) {
        if (feedViewReady) {
            DisplayArticlesFragment displayArticlesFragment = (DisplayArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_feed);
            displayArticlesFragment.articleChoosen(position);
        }
        
        ArticleController articleController = new ArticleController(this);
        articleController.setRead(getKeyByValue(articleMap, position));
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
