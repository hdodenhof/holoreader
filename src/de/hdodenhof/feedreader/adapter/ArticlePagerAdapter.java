package de.hdodenhof.feedreader.adapter;

import java.util.List;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;

public class ArticlePagerAdapter extends FragmentPagerAdapter {

        private List<DisplayArticleFragment> mFragments;
        private List<String> mTitles;

        public ArticlePagerAdapter(FragmentManager fm, List<DisplayArticleFragment> fragments, List<String> titles) {
                super(fm);
                this.mFragments = fragments;
                this.mTitles = titles;
        }

        @Override
        public DisplayArticleFragment getItem(int position) {
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
