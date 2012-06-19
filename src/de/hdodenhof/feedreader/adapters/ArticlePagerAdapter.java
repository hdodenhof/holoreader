package de.hdodenhof.feedreader.adapters;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import de.hdodenhof.feedreader.fragments.ArticleFragment;
import de.hdodenhof.feedreader.models.Article;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class ArticlePagerAdapter extends FragmentStatePagerAdapter {

        @SuppressWarnings("unused")
        private static final String TAG = ArticlePagerAdapter.class.getSimpleName();

        private List<Article> mArticles;

        public ArticlePagerAdapter(FragmentManager fm, List<Article> articles) {
                super(fm);
                this.mArticles = articles;
        }

        @Override
        public Fragment getItem(int position) {
                return ArticleFragment.newInstance(this.mArticles.get(position));
        }

        @Override
        public int getCount() {
                return this.mArticles.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
                final int LENGTH = 25;

                String mTitle = mArticles.get(position).getTitle();
                if (mTitle.length() > LENGTH) {
                        return mTitle.substring(0, LENGTH) + "...";
                } else {
                        return mTitle;
                }
        }
        
        public Article getArticleAtPosition(int position){
                return mArticles.get(position);
        }

}
