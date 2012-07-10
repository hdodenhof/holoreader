package de.hdodenhof.feedreader.misc;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.fragments.ArticleFragment;
import de.hdodenhof.feedreader.helpers.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.providers.RSSContentProvider;

/**
 * 
 * @author Henning Dodenhof
 * 
 */

public class ArticleViewPager implements OnPageChangeListener, LoaderCallbacks<Cursor> {

        @SuppressWarnings("unused")
        private static final String TAG = ArticleViewPager.class.getSimpleName();
        private static final int LOADER = 30;  

        private FragmentActivity mContext;
        private ArticlePagerAdapter mPagerAdapter;
        private ViewPager mPager;
        private int mSelectedArticleID = -1;
        private int mFeedID;
        private String[] mProjection = { ArticleDAO._ID, ArticleDAO.FEEDID, ArticleDAO.TITLE, ArticleDAO.PUBDATE, ArticleDAO.CONTENT };

        public void changePosition(int position) {
                if (mPager.getCurrentItem() != position) {
                        mPager.setCurrentItem(position);
                }
        }

        public ArticleViewPager(FragmentActivity context) {
                this.mContext = context;

                int mArticleID = mContext.getIntent().getIntExtra("articleid", 0);
                mFeedID = queryFeedID(mArticleID);

                mContext.getSupportLoaderManager().initLoader(LOADER, null, this);

                mPagerAdapter = new ArticlePagerAdapter(mContext.getSupportFragmentManager(), mProjection, null);

                mPager = (ViewPager) mContext.findViewById(R.id.viewpager_article);
                mPager.setAdapter(mPagerAdapter);
                mPager.setOnPageChangeListener(this);

                mSelectedArticleID = mArticleID;
        }

        /**
         * 
         * @param articleID
         * @return
         */
        private int queryFeedID(int articleID) {
                Uri mBaseUri = Uri.withAppendedPath(RSSContentProvider.URI_ARTICLES, String.valueOf(articleID));
                String[] mProjection = { ArticleDAO._ID, ArticleDAO.FEEDID };

                Cursor mCursor = mContext.getContentResolver().query(mBaseUri, mProjection, null, null, null);
                mCursor.moveToFirst();
                int mFeedID = mCursor.getInt(mCursor.getColumnIndex(ArticleDAO.FEEDID));
                mCursor.close();

                return mFeedID;
        }

        public void onPageScrollStateChanged(int state) {
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
                ((ArticleOnPageChangeListener) mContext).onArticleChanged(mPagerAdapter.getArticleID(position), position);
        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                Uri mBaseUri = Uri.withAppendedPath(RSSContentProvider.URI_ARTICLES, "feed/" + mFeedID);
                CursorLoader mCursorLoader = new CursorLoader(mContext, mBaseUri, mProjection, null, null, null);
                return mCursorLoader;
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                mPagerAdapter.swapCursor(data);
                if (mSelectedArticleID != -1) {
                        mPager.setCurrentItem(queryPosition(data, mSelectedArticleID), false);
                        mSelectedArticleID = -1;
                }
        }

        public void onLoaderReset(Loader<Cursor> loader) {
                mPagerAdapter.swapCursor(null);
        }

        private int queryPosition(Cursor cursor, int articleID) {
                int mCursorPosition = cursor.getPosition();
                int mPosition = 0;
                int mIterator = 0;
                
                cursor.moveToFirst();
                do {
                        if(articleID == cursor.getInt(cursor.getColumnIndex(ArticleDAO._ID))){
                                mPosition = mIterator;
                                break;
                        }
                        mIterator++;
                } while (cursor.moveToNext());
                
                cursor.moveToPosition(mCursorPosition);
                
                return mPosition;
        }

        private class ArticlePagerAdapter extends FragmentStatePagerAdapter {

                private final String[] mProjection;
                private Cursor mCursor;

                public ArticlePagerAdapter(FragmentManager fm, String[] projection, Cursor cursor) {
                        super(fm);
                        this.mProjection = projection;
                        this.mCursor = cursor;
                }

                @Override
                public Fragment getItem(int position) {
                        if (mCursor == null) {
                                return null;
                        }

                        mCursor.moveToPosition(position);
                        Fragment mFragment;
                        try {
                                mFragment = ArticleFragment.newInstance();
                        } catch (Exception mException) {
                                throw new RuntimeException(mException);
                        }
                        Bundle args = new Bundle();
                        for (int i = 0; i < mProjection.length; ++i) {
                                args.putString(mProjection[i], mCursor.getString(i));
                        }
                        mFragment.setArguments(args);
                        return mFragment;
                }

                @Override
                public int getCount() {
                        if (mCursor == null) {
                                return 0;
                        } else {
                                return mCursor.getCount();
                        }
                }

                public void swapCursor(Cursor cursor) {
                        if (mCursor == cursor) {
                                return;
                        }

                        this.mCursor = cursor;
                        notifyDataSetChanged();
                }
                
                public int getArticleID(int position){
                        int mCursorPosition = mCursor.getPosition();
                        
                        mCursor.moveToPosition(position);
                        int mArticleID = mCursor.getInt(mCursor.getColumnIndex(ArticleDAO._ID));
                        mCursor.moveToPosition(mCursorPosition);
                        
                        return mArticleID;                        
                }

        }

}
