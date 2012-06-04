package de.hdodenhof.feedreader.fragments;

import java.sql.SQLException;
import java.util.ArrayList;

import de.hdodenhof.feedreader.DisplayArticleActivity;
import de.hdodenhof.feedreader.adapter.ArticleAdapter;
import de.hdodenhof.feedreader.dao.ArticlesDataSource;
import de.hdodenhof.feedreader.dao.FeedsDataSource;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;
import de.hdodenhof.feedreader.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class DisplayFeedFragment extends Fragment {
    public String rssResult = "";

    private ArticleAdapter articleAdapter;
    private FeedsDataSource feedsdatasource;
    private ArticlesDataSource articlesdatasource;
    private ListView articlelistview;
    private Feed feed;

    boolean mDualPane;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.feed_fragment, container, false);

        feedsdatasource = new FeedsDataSource(getActivity());
        try {
            feedsdatasource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        long b = getActivity().getIntent().getLongExtra("feedid", -1);
        feed = feedsdatasource.getFeed(b);
        feedsdatasource.close();

        articleAdapter = new ArticleAdapter(getActivity(), new ArrayList<Article>());

        articlelistview = (ListView) contentView.findViewById(R.id.article_listview);

        articlelistview.setAdapter(articleAdapter);
        articlelistview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                
                Article article = (Article) parent.getItemAtPosition(position);
                
                if (mDualPane) {
                    
                    DisplayArticleFragment articleFragment = (DisplayArticleFragment) getFragmentManager().findFragmentById(R.id.article_fragment);
                    if (articleFragment == null || articleFragment.getShownIndex() != article.getId()) {
                        articleFragment = DisplayArticleFragment.newInstance(article.getId());

                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.replace(R.id.article_fragment, articleFragment);
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                        ft.commit();
                    }

                } else {
                    Intent intent = new Intent(getActivity().getApplicationContext(), DisplayArticleActivity.class);

                    intent.putExtra("articleid", article.getId());
                    intent.putExtra("feedid", feed.getId());
                    startActivity(intent);
                }

            }

        });

        articlesdatasource = new ArticlesDataSource(getActivity());
        try {
            articlesdatasource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }            
        
        ArrayList<Article> al = (ArrayList<Article>) articlesdatasource.getAllArticles(feed.getId());
        articleAdapter.clear();

        for (Article article : al) {
            articleAdapter.add(article);        
        
        }
        return contentView;

    }
    
    @Override
    public void onActivityCreated(Bundle savedState){
        super.onActivityCreated(savedState);
        
        View articleFrame = getActivity().findViewById(R.id.article_fragment);
        mDualPane = articleFrame != null && articleFrame.getVisibility() == View.VISIBLE;

        if (mDualPane) {
            articlelistview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
        
//        ActionBar actionBar = getActivity().getActionBar();
//        actionBar.setTitle(feed.getName());
//        actionBar.setDisplayHomeAsUpEnabled(true);         
    }

}
