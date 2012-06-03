package de.hdodenhof.feedreader;

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import de.hdodenhof.feedreader.adapter.ArticlePagerAdapter;
import de.hdodenhof.feedreader.dao.ArticlesDataSource;
import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;
import de.hdodenhof.feedreader.model.Article;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class DisplayArticleActivity extends FragmentActivity {
    Bundle b;
    Long articleId;
    Long feedId;
    
    private PagerAdapter mPagerAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.viewpager);
             
        b = getIntent().getExtras();
        articleId = getIntent().getLongExtra("articleid", -1);
        feedId = getIntent().getLongExtra("feedid", -1);
        
        this.initialisePaging();  
        
//        if (savedInstanceState == null) {
//            new DisplayArticleFragment();
//            DisplayArticleFragment articleFragment = DisplayArticleFragment.newInstance(articleId);
//            articleFragment.setArguments(b);
//            getSupportFragmentManager().beginTransaction().add(android.R.id.content, articleFragment).commit();
//        }        
        
    }
    
    private void initialisePaging() {

        ArticlesDataSource articlesdatasource;
        List<Fragment> fragments = new Vector<Fragment>();
        List<String> titles = new Vector<String>();
        articlesdatasource = new ArticlesDataSource(this);
        try {
            articlesdatasource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }            
        
        List<Article> articles = articlesdatasource.getAllArticles(feedId);
        int pos = 0;
        int curr = 0;
        
        for (Article article: articles) {
           fragments.add(DisplayArticleFragment.newInstance(article.getId()));
           titles.add(article.getTitle());
           if (article.getId() == articleId){
               curr = pos;
           }
           pos++;
        }
        
        this.mPagerAdapter = new ArticlePagerAdapter(super.getSupportFragmentManager(), fragments, titles);
        
        ViewPager pager = (ViewPager)super.findViewById(R.id.viewpager);
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
