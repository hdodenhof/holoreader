package de.hdodenhof.feedreader.fragments;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.adapter.ArticleAdapter;
import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;

public class DisplayArticlesFragment extends ListFragment {

    private boolean mDualFragments = false;
    private ListView articleslistview;
    private FeedController feedController;
    private ArticleController articleController;
    private ArticleAdapter articleAdapter;
    private ParameterProvider mParameterProvider;
    private OnArticleSelectedListener mOnArticleSelectedListener;
    private boolean choiceModeSingle = false;

    public interface OnArticleSelectedListener {
        public void articleSelected(int index, Article article);
    }

    public interface ParameterProvider {
        public long getFeedId();
        public int getArticlePosition();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mParameterProvider = (ParameterProvider) activity;
        mOnArticleSelectedListener = (OnArticleSelectedListener) activity;
    }

    public void updateContent(Long id) {
        ArrayList<Article> articleList;

        if (id == null || id == -1) {
            articleList = articleController.getAllArticles();
        } else {
            Feed feed = feedController.getFeed(id);
            articleList = articleController.getAllArticles(feed.getId());
        }

        articleAdapter.clear();

        for (Article article : articleList) {
            articleAdapter.add(article);
        }
        articleAdapter.notifyDataSetChanged();

    }

    public void setChoiceModeSingle() {
        this.choiceModeSingle = true;
    }
    
    public void highlight(int position){
        articleslistview.setItemChecked(position, true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        feedController = new FeedController(getActivity());
        articleController = new ArticleController(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View articleFragment = getActivity().findViewById(R.id.viewpager);
        DisplayFeedsFragment feedsFragment = (DisplayFeedsFragment) getFragmentManager().findFragmentById(R.id.fragment_feeds);
        if (articleFragment != null || feedsFragment != null) {
            mDualFragments = true;
        }

        if (!mDualFragments) {
            ActionBar actionBar = getActivity().getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {

        }

        long feedId = mParameterProvider.getFeedId();

        articleAdapter = new ArticleAdapter(getActivity(), new ArrayList<Article>());

        this.setEmptyText("No articles");
        this.setListAdapter(articleAdapter);

        articleslistview = getListView();
        if (choiceModeSingle) {
            articleslistview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        articleslistview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Article article = (Article) articleslistview.getItemAtPosition(position);
                mOnArticleSelectedListener.articleSelected(position, article);
            }
        });

        updateContent(feedId);
        
        if (mParameterProvider.getArticlePosition() != -1){
            highlight(mParameterProvider.getArticlePosition());
        }
        
    }

}
