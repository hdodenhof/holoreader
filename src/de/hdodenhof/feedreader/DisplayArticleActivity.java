package de.hdodenhof.feedreader;

import java.util.List;
import java.util.Vector;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.hdodenhof.feedreader.adapter.ArticlePagerAdapter;
import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;
import de.hdodenhof.feedreader.model.Article;

public class DisplayArticleActivity extends FragmentActivity {
    Bundle b;
    Long articleId;
    Long feedId;

    private ArticlePagerAdapter mPagerAdapter;
    ArticleController articleController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.viewpager);

        articleController = new ArticleController(this);

        b = getIntent().getExtras();
        articleId = getIntent().getLongExtra("articleid", -1);
        feedId = getIntent().getLongExtra("feedid", -1);

        this.initialisePaging();

    }

    private void initialisePaging() {

        List<DisplayArticleFragment> fragments = new Vector<DisplayArticleFragment>();
        List<String> titles = new Vector<String>();

        List<Article> articles = articleController.getAllArticles(feedId);
        int pos = 0;
        int curr = 0;

        for (Article article : articles) {
            fragments.add(DisplayArticleFragment.newInstance(article.getId()));
            titles.add(article.getTitle());
            if (article.getId() == articleId) {
                curr = pos;
            }
            pos++;
        }

        this.mPagerAdapter = new ArticlePagerAdapter(super.getSupportFragmentManager(), fragments, titles);

        ViewPager pager = (ViewPager) super.findViewById(R.id.viewpager);
        pager.setAdapter(this.mPagerAdapter);
        pager.setCurrentItem(curr);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, DisplayFeedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("feedid", b.getLong("feedid"));
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
}
