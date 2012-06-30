package de.hdodenhof.feedreader.fragments;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import de.hdodenhof.feedreader.adapters.RSSArticleAdapter;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.RSSMessage;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class ArticleListFragment extends ListFragment {

        @SuppressWarnings("unused")
        private static final String TAG = ArticleListFragment.class.getSimpleName();

        private ListView mArticlesListView;
        private RSSArticleAdapter mArticleAdapter;
        private boolean mInitialized = false;

        Handler mMessageHandler = new MyHandler(this);

        private static class MyHandler extends Handler {
                private final WeakReference<ArticleListFragment> mTargetReference;

                MyHandler(ArticleListFragment target) {
                        mTargetReference = new WeakReference<ArticleListFragment>(target);
                }

                public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        ArticleListFragment mTarget = mTargetReference.get();

                        RSSMessage mMessage = (RSSMessage) msg.obj;
                        switch (mMessage.type) {
                        case RSSMessage.INITIALIZE:
                                mTarget.initialize(mMessage.feeds, mMessage.article);
                                break;
                        case RSSMessage.FEEDLIST_UPDATED:
                                mTarget.updateFeedlist(mMessage.feeds);
                                break;
                        case RSSMessage.FEED_SELECTED:
                                mTarget.selectFeed(mMessage.feed);
                                break;
                        case RSSMessage.POSITION_CHANGED:
                                mTarget.changePosition(mMessage.position);
                                break;
                        case RSSMessage.CHOICE_MODE_SINGLE_ARTICLE:
                                mTarget.setChoiceModeSingle();
                                break;
                        default:
                                break;
                        }
                }
        }

        private void initialize(ArrayList<Feed> feeds, Article article) {
                ArrayList<Article> mArticleList = new ArrayList<Article>();

                int mPos = 0;
                int mCurrent = 0;

                for (Feed mFeed : feeds) {
                        for (Article mArticle : mFeed.getArticles()) {
                                mArticleList.add(mArticle);
                                if (article != null && mArticle.getId() == article.getId()) {
                                        mCurrent = mPos;
                                }
                                mPos++;
                        }
                }
                refreshAdapter(mArticleList);
                if (article != null) {
                        int mPosition = (mCurrent - 1 < 0) ? 0 : mCurrent - 1;
                        mArticlesListView.smoothScrollToPositionFromTop(mPosition, 0, 1000);
                        mArticlesListView.setItemChecked(mCurrent, true);
                }
        }

        private void updateFeedlist(ArrayList<Feed> feeds) {
                ArrayList<Article> mArticleList = new ArrayList<Article>();

                for (Feed mFeed : feeds) {
                        for (Article mArticle : mFeed.getArticles()) {
                                mArticleList.add(mArticle);
                        }
                }

                refreshAdapter(mArticleList);
        }

        private void selectFeed(Feed feed) {
                ArrayList<Article> mArticleList = new ArrayList<Article>();

                for (Article mArticle : feed.getArticles()) {
                        mArticleList.add(mArticle);
                }

                refreshAdapter(mArticleList);
        }

        private void changePosition(int position) {
                if (mInitialized) {
                        if (mArticlesListView.getCheckedItemPosition() != position) {
                                int mPosition = (position - 1 < 0) ? 0 : position - 1;
                                mArticlesListView.smoothScrollToPositionFromTop(mPosition, 0, 500);
                                mArticlesListView.setItemChecked(position, true);
                        }
                }
        }

        private void setChoiceModeSingle() {
                mArticlesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        private void refreshAdapter(ArrayList<Article> articles) {
                mArticleAdapter.clear();
                for (Article mArticle : articles) {
                        mArticleAdapter.add(mArticle);
                }
                mArticleAdapter.notifyDataSetChanged();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
                super.onActivityCreated(savedInstanceState);

                if (savedInstanceState != null) {

                }

                mArticleAdapter = new RSSArticleAdapter(getActivity(), new ArrayList<Article>());

                this.setEmptyText("No articles");
                this.setListAdapter(mArticleAdapter);
                mArticlesListView = getListView();

                mArticlesListView.setOnItemClickListener((OnItemClickListener) getActivity());

                mInitialized = true;
                ((FragmentCallback) getActivity()).onFragmentReady(mMessageHandler);

        }

}
