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

import de.hdodenhof.feedreader.adapter.ArticlePagerAdapter;
import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;

public class DisplayArticleActivity extends FragmentActivity implements DisplayArticleFragment.ParameterProvider, OnPageChangeListener {

    private Long articleId;
    private Long feedId;

    private ArticlePagerAdapter mPagerAdapter;
    private ArticleController articleController;
    private Map<Long, Integer> articleMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_article);

        articleController = new ArticleController(this);

        articleId = getIntent().getLongExtra("articleid", -1);
        feedId = articleController.getArticle(articleId).getFeedId();

        this.initialisePaging();

        FeedController feedController = new FeedController(this);
        Feed feed = feedController.getFeed(feedId);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(feed.getName());
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    private void initialisePaging() {

        List<DisplayArticleFragment> fragments = new Vector<DisplayArticleFragment>();
        List<String> titles = new Vector<String>();

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

        this.mPagerAdapter = new ArticlePagerAdapter(super.getSupportFragmentManager(), fragments, titles);

        ViewPager pager = (ViewPager) super.findViewById(R.id.viewpager);
        pager.setAdapter(this.mPagerAdapter);
        pager.setOnPageChangeListener(this);
        pager.setCurrentItem(curr);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, DisplayFeedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("feedid", feedId);
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

    public long getArticleId() {
        return this.articleId;
    }

    public void onPageScrollStateChanged(int state) {
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageSelected(int position) {
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
