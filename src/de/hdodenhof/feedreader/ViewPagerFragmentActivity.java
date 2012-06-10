package de.hdodenhof.feedreader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import de.hdodenhof.feedreader.adapter.ArticlePagerAdapter;
import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;
import de.hdodenhof.feedreader.fragments.DisplayArticlesFragment;
import de.hdodenhof.feedreader.model.Article;

public class ViewPagerFragmentActivity extends FragmentActivity implements OnPageChangeListener {
        protected ArticlePagerAdapter mPagerAdapter;
        protected ViewPager mPager;
        protected Map<Long, Integer> mArticleMap;
        protected boolean mTwoPane = false;
        protected boolean mFragmentsReady = false;

        protected Long mArticleID = (long) -1;
        protected Long mFeedID = (long) -1;

        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
        }

        protected void initialisePaging() {
                ArticleController mArticleController = new ArticleController(this);

                List<DisplayArticleFragment> mArticleFragments = new Vector<DisplayArticleFragment>();
                List<String> mTitles = new Vector<String>();
                List<Article> mArticles = mArticleController.getAllArticles(mFeedID);

                int mPos = 0;
                int mCurrent = 0;
                mArticleMap = new HashMap<Long, Integer>();

                for (Article mArticle : mArticles) {
                        mArticleFragments.add(DisplayArticleFragment.newInstance(mArticle.getId()));
                        mTitles.add(mArticle.getTitle());
                        if (mArticle.getId() == mArticleID) {
                                mCurrent = mPos;
                        }
                        mArticleMap.put(mArticle.getId(), mPos);
                        mPos++;
                }

                this.mPagerAdapter = new ArticlePagerAdapter(getSupportFragmentManager(), mArticleFragments, mTitles);

                mPager = (ViewPager) findViewById(R.id.viewpager);
                mPager.setAdapter(this.mPagerAdapter);
                mPager.setOnPageChangeListener(this);
                mPager.setCurrentItem(mCurrent);
        }

        public long getArticleId() {
                return this.mArticleID;
        }

        public long getFeedId() {
                return this.mFeedID;
        }

        public void onPageScrollStateChanged(int state) {
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
                ArticleController mArticleController = new ArticleController(this);
                mArticleController.setRead(getKeyByValue(mArticleMap, position));

                if (mFragmentsReady) {
                        DisplayArticlesFragment mArticlesFragment = (DisplayArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_feed);
                        mArticlesFragment.articleChoosen(position);
                }

        }

        protected static <T, E> T getKeyByValue(Map<T, E> map, E value) {
                for (Entry<T, E> mEntry : map.entrySet()) {
                        if (value.equals(mEntry.getValue())) {
                                return mEntry.getKey();
                        }
                }
                return null;
        }
}
