package de.hdodenhof.feedreader.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.adapters.ArticlePagerAdapter;
import de.hdodenhof.feedreader.listeners.ArticleOnPageChangeListener;
import de.hdodenhof.feedreader.listeners.OnFragmentReadyListener;
import de.hdodenhof.feedreader.misc.RSSMessage;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class ArticlePagerFragment implements RSSFragment, OnPageChangeListener {

        @SuppressWarnings("unused")
        private static final String TAG = ArticlePagerFragment.class.getSimpleName();        
        
        private FragmentActivity mContext;
        private ArticlePagerAdapter mPagerAdapter;
        private ViewPager mPager;
        private Map<Article, Integer> mArticleMap;

        public ArticlePagerFragment(FragmentActivity context) {
                this.mContext = context;

                ((OnFragmentReadyListener) mContext).onFragmentReady(this);
        }

        public void handleMessage(RSSMessage message) {
                switch (message.type) {
                case RSSMessage.INITIALIZE:
                        initialisePaging(message.feed, message.article);
                        break;
                case RSSMessage.POSITION_CHANGED:
                        if (mPager.getCurrentItem() != message.position) {
                                mPager.setCurrentItem(message.position);
                        }
                        break;
                default:
                        break;
                }
        }

        private void initialisePaging(Feed feed, Article article) {

                List<ArticleFragment> mArticleFragments = new Vector<ArticleFragment>();
                List<String> mTitles = new Vector<String>();
                List<Article> mArticles = new ArrayList<Article>();

                mArticles.addAll(feed.getArticles());

                int mPos = 0;
                int mCurrent = 0;
                mArticleMap = new HashMap<Article, Integer>();

                for (Article mArticle : mArticles) {
                        mArticleFragments.add(ArticleFragment.newInstance(mArticle));
                        mTitles.add(mArticle.getTitle());
                        if (mArticle.getId() == article.getId()) {
                                mCurrent = mPos;
                        }
                        mArticleMap.put(mArticle, mPos);
                        mPos++;
                }

                mPagerAdapter = new ArticlePagerAdapter(mContext.getSupportFragmentManager(), mArticleFragments, mTitles);

                mPager = (ViewPager) mContext.findViewById(R.id.viewpager_article);
                mPager.setAdapter(mPagerAdapter);
                mPager.setOnPageChangeListener(this);
                mPager.setCurrentItem(mCurrent);
        }

        public void onPageScrollStateChanged(int state) {
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
                Article mArticle = getKeyByValue(mArticleMap, position);
                ((ArticleOnPageChangeListener) mContext).onArticleChanged(mArticle, position);
        }

        private static <T, E> T getKeyByValue(Map<T, E> map, E value) {
                for (Entry<T, E> mEntry : map.entrySet()) {
                        if (value.equals(mEntry.getValue())) {
                                return mEntry.getKey();
                        }
                }
                return null;
        }

}
