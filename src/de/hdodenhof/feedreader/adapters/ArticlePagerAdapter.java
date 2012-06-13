package de.hdodenhof.feedreader.adapters;

import java.util.List;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.hdodenhof.feedreader.fragments.ArticleFragment;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class ArticlePagerAdapter extends FragmentPagerAdapter {

        @SuppressWarnings("unused")
        private static final String TAG = ArticlePagerAdapter.class.getSimpleName();        
        
        private List<ArticleFragment> mFragments;
        private List<String> mTitles;

        public ArticlePagerAdapter(FragmentManager fm, List<ArticleFragment> fragments, List<String> titles) {
                super(fm);
                this.mFragments = fragments;
                this.mTitles = titles;
        }

        @Override
        public ArticleFragment getItem(int position) {
                return this.mFragments.get(position);
        }

        @Override
        public int getCount() {
                return this.mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
                final int LENGTH = 25;

                if (mTitles.get(position).length() > LENGTH) {
                        return mTitles.get(position).substring(0, LENGTH) + "...";
                } else {
                        return mTitles.get(position);
                }
        }

}
