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

        private boolean mTwoPane = false;
        private ListView mArticlesListView;
        private FeedController mFeedController;
        private ArticleController mArticleController;
        private ArticleAdapter mArticleAdapter;
        private ActivityConnector mActivityConnector;
        private boolean mChoiceModeSingle = false;

        public interface ActivityConnector {
                public long getFeedId();

                public int getArticlePosition();

                public void articleSelected(int index, Article article);
        }

        @Override
        public void onAttach(Activity activity) {
                super.onAttach(activity);
                mActivityConnector = (ActivityConnector) activity;
        }

        public void updateContent(Long id) {
                ArrayList<Article> mArticleList;

                if (id == null || id == -1) {
                        mArticleList = mArticleController.getAllArticles();
                } else {
                        Feed mFeed = mFeedController.getFeed(id);
                        mArticleList = mArticleController.getAllArticles(mFeed.getId());
                }

                mArticleAdapter.clear();

                for (Article mArticle : mArticleList) {
                        mArticleAdapter.add(mArticle);
                }
                mArticleAdapter.notifyDataSetChanged();

        }

        public void setChoiceModeSingle() {
                this.mChoiceModeSingle = true;
        }

        public void articleChoosen(int position) {
                mArticlesListView.setItemChecked(position, true);
                this.refreshView();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                mFeedController = new FeedController(getActivity());
                mArticleController = new ArticleController(getActivity());
        }

        @Override
        public void onResume() {
                super.onResume();
                refreshView();
        }

        private void refreshView() {
                long mFeedID = mActivityConnector.getFeedId();

                int mIndex = mArticlesListView.getFirstVisiblePosition();
                View mView = mArticlesListView.getChildAt(0);
                int mTop = (mView == null) ? 0 : mView.getTop();

                updateContent(mFeedID);

                mArticlesListView.setSelectionFromTop(mIndex, mTop);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
                super.onActivityCreated(savedInstanceState);

                // FIXME
                View mArticleFragment = getActivity().findViewById(R.id.viewpager);
                DisplayFeedsFragment mFeedsFragment = (DisplayFeedsFragment) getFragmentManager().findFragmentById(R.id.fragment_feeds);
                
                if (mArticleFragment != null || mFeedsFragment != null) {
                        mTwoPane = true;
                }

                if (!mTwoPane) {
                        ActionBar mActionBar = getActivity().getActionBar();
                        mActionBar.setDisplayHomeAsUpEnabled(true);
                }
                // END

                if (savedInstanceState != null) {

                }

                long mFeedID = mActivityConnector.getFeedId();

                mArticleAdapter = new ArticleAdapter(getActivity(), new ArrayList<Article>());

                this.setEmptyText("No articles");
                this.setListAdapter(mArticleAdapter);

                mArticlesListView = getListView();
                if (mChoiceModeSingle) {
                        mArticlesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                }

                mArticlesListView.setOnItemClickListener(new OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Article mArticle = (Article) mArticlesListView.getItemAtPosition(position);
                                mActivityConnector.articleSelected(position, mArticle);
                        }
                });

                updateContent(mFeedID);

                if (mActivityConnector.getArticlePosition() != -1) {
                        this.articleChoosen(mActivityConnector.getArticlePosition());
                }

        }

}
