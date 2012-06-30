package de.hdodenhof.feedreader.fragments;

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
        private boolean mInitialized = false;;

        Handler mMessageHandler = new Handler() {
                public void handleMessage(Message msg) {
                        super.handleMessage(msg);

                        RSSMessage mMessage = (RSSMessage) msg.obj;
                        ArrayList<Article> mArticleList;

                        switch (mMessage.type) {
                        case RSSMessage.INITIALIZE:
                                mArticleList = new ArrayList<Article>();

                                int mPos = 0;
                                int mCurrent = 0;

                                for (Feed mFeed : mMessage.feeds) {
                                        for (Article mArticle : mFeed.getArticles()) {
                                                mArticleList.add(mArticle);
                                                if (mMessage.article != null && mArticle.getId() == mMessage.article.getId()) {
                                                        mCurrent = mPos;
                                                }
                                                mPos++;
                                        }
                                }
                                refreshAdapter(mArticleList);
                                if (mMessage.article != null) {
                                        int mPosition = (mCurrent - 1 < 0) ? 0 : mCurrent - 1;
                                        mArticlesListView.smoothScrollToPositionFromTop(mPosition, 0, 1000);
                                        mArticlesListView.setItemChecked(mCurrent, true);
                                }

                                break;
                        case RSSMessage.FEEDLIST_UPDATED:
                                mArticleList = new ArrayList<Article>();

                                for (Feed mFeed : mMessage.feeds) {
                                        for (Article mArticle : mFeed.getArticles()) {
                                                mArticleList.add(mArticle);
                                        }
                                }

                                refreshAdapter(mArticleList);
                                break;
                        case RSSMessage.FEED_SELECTED:
                                mArticleList = new ArrayList<Article>();

                                for (Article mArticle : mMessage.feed.getArticles()) {
                                        mArticleList.add(mArticle);
                                }

                                refreshAdapter(mArticleList);
                                break;
                        case RSSMessage.POSITION_CHANGED:
                                if (mInitialized) {
                                        if (mArticlesListView.getCheckedItemPosition() != mMessage.position) {
                                                int mPosition = (mMessage.position - 1 < 0) ? 0 : mMessage.position - 1;
                                                mArticlesListView.smoothScrollToPositionFromTop(mPosition, 0, 500);
                                                mArticlesListView.setItemChecked(mMessage.position, true);
                                        }
                                }
                                break;
                        case RSSMessage.CHOICE_MODE_SINGLE_ARTICLE:
                                mArticlesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                                break;
                        default:
                                break;
                        }
                }
        };

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
