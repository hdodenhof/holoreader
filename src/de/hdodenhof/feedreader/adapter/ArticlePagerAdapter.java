package de.hdodenhof.feedreader.adapter;

import java.util.List;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;

public class ArticlePagerAdapter extends FragmentPagerAdapter {

    private List<DisplayArticleFragment> fragments;
    private List<String> titles;

    public ArticlePagerAdapter(FragmentManager fm, List<DisplayArticleFragment> fragments, List<String> titles) {
        super(fm);
        this.fragments = fragments;
        this.titles = titles;
    }

    @Override
    public DisplayArticleFragment getItem(int position) {
        return this.fragments.get(position);
    }

    @Override
    public int getCount() {
        return this.fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        final int LENGTH = 25;

        if (titles.get(position).length() > LENGTH) {
            return titles.get(position).substring(0, LENGTH) + "...";
        } else {
            return titles.get(position);
        }
    }

}
