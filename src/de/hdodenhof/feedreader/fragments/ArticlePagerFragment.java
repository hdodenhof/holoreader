package de.hdodenhof.feedreader.fragments;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.adapters.ArticlePagerAdapter;
import de.hdodenhof.feedreader.listeners.ArticleOnPageChangeListener;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.RSSMessage;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 * 
 */

@SuppressLint("ValidFragment")
public class ArticlePagerFragment implements OnPageChangeListener {

        @SuppressWarnings("unused")
        private static final String TAG = ArticlePagerFragment.class.getSimpleName();

        private FragmentActivity mContext;
        private ArticlePagerAdapter mPagerAdapter;
        private ViewPager mPager;

        Handler mMessageHandler = new MyHandler(this);

        private static class MyHandler extends Handler {
                private final WeakReference<ArticlePagerFragment> mTargetReference;

                MyHandler(ArticlePagerFragment target) {
                        mTargetReference = new WeakReference<ArticlePagerFragment>(target);
                }

                public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        ArticlePagerFragment mTarget = mTargetReference.get();

                        RSSMessage mMessage = (RSSMessage) msg.obj;

                        switch (mMessage.type) {
                        case RSSMessage.INITIALIZE:
                                mTarget.initialize(mMessage.feed, mMessage.article);
                                break;
                        case RSSMessage.POSITION_CHANGED:
                                mTarget.changePosition(mMessage.position);
                                break;
                        default:
                                break;
                        }
                }
        }

        private void initialize(Feed feed, Article article) {
                List<Article> mArticles = new ArrayList<Article>();

                mArticles.addAll(feed.getArticles());

                int mPos = 0;
                int mCurrent = 0;

                for (Article mArticle : mArticles) {
                        if (mArticle.getId() == article.getId()) {
                                mCurrent = mPos;
                        }
                        mPos++;
                }

                mPagerAdapter = new ArticlePagerAdapter(mContext.getSupportFragmentManager(), mArticles);

                mPager = (ViewPager) mContext.findViewById(R.id.viewpager_article);
                mPager.setAdapter(mPagerAdapter);
                mPager.setOnPageChangeListener(this);
                mPager.setCurrentItem(mCurrent);
        }

        private void changePosition(int position) {
                if (mPager.getCurrentItem() != position) {
                        mPager.setCurrentItem(position);
                }
        }
        
        public ArticlePagerFragment(FragmentActivity context) {
                this.mContext = context;

                ((FragmentCallback) mContext).onFragmentReady(mMessageHandler);
        }

        public void onPageScrollStateChanged(int state) {
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
                Article mArticle = mPagerAdapter.getArticleAtPosition(position);
                ((ArticleOnPageChangeListener) mContext).onArticleChanged(mArticle, position);
        }

}
