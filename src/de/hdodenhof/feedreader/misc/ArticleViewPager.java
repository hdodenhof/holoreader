package de.hdodenhof.feedreader.misc;

import java.util.ArrayList;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.fragments.ArticleFragment;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */

public class ArticleViewPager implements OnPageChangeListener, LoaderCallbacks<Cursor> {

    @SuppressWarnings("unused")
    private static final String TAG = ArticleViewPager.class.getSimpleName();
    private static final int LOADER = 30;
    private static final int STATE_LOADING = 1;
    private static final int STATE_LOADED = 2;

    private SherlockFragmentActivity mContext;
    private ArticlePagerAdapter mPagerAdapter;
    private ViewPager mPager;
    private int mPreselectedArticleID = -1;
    private int mCurrentArticleID = -1;
    private int mCurrentState;
    private ArrayList<String> mArticles = new ArrayList<String>();
    private String[] mProjection = { ArticleDAO._ID, ArticleDAO.FEEDID, ArticleDAO.FEEDNAME, ArticleDAO.TITLE, ArticleDAO.PUBDATE, ArticleDAO.CONTENT };

    public void changePosition(int position) {
        if (mPager.getCurrentItem() != position) {
            mPager.setCurrentItem(position);
        }
    }

    public ArticleViewPager(SherlockFragmentActivity context) {
        this.mContext = context;
        this.mCurrentState = STATE_LOADING;

        mArticles = mContext.getIntent().getStringArrayListExtra("articles");
        mPreselectedArticleID = mContext.getIntent().getIntExtra("articleid", 0);

        mContext.getSupportLoaderManager().initLoader(LOADER, null, this);

        mPagerAdapter = new ArticlePagerAdapter(mContext.getSupportFragmentManager(), mProjection, null);

        mPager = (ViewPager) mContext.findViewById(R.id.viewpager_article);
        mPager.setAdapter(mPagerAdapter);
        mPager.setOnPageChangeListener(this);
    }

    public void onPageScrollStateChanged(int state) {
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageSelected(int position) {
        int newArticleID = mPagerAdapter.getArticleID(position);
        ((OnArticleChangedListener) mContext).onArticleChanged(mCurrentArticleID, newArticleID, position);
        mCurrentArticleID = newArticleID;
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = ArticleDAO._ID + " IN (";
        for (int i = 0; i < mArticles.size() - 1; i++) {
            selection = selection + "?, ";
        }
        selection = selection + "?)";
        String[] selectionArgs = mArticles.toArray(new String[mArticles.size()]);

        CursorLoader cursorLoader = new CursorLoader(mContext, RSSContentProvider.URI_ARTICLES, mProjection, selection, selectionArgs, ArticleDAO.PUBDATE
                + " DESC");
        return cursorLoader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mPagerAdapter.swapCursor(data);
        if (mCurrentState == STATE_LOADING) {
            if (mPreselectedArticleID != -1) {
                int mPreselectedPosition = queryPosition(data, mPreselectedArticleID);
                if (mPreselectedPosition != 0) {
                    mPager.setCurrentItem(mPreselectedPosition, false);
                } else {
                    onPageSelected(0);
                }
            } else {
                onPageSelected(0);
            }
            mCurrentState = STATE_LOADED;
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mPagerAdapter.swapCursor(null);
    }

    private int queryPosition(Cursor cursor, int articleID) {
        int cursorPosition = cursor.getPosition();
        int position = 0;
        int iterator = 0;

        cursor.moveToFirst();
        do {
            if (articleID == cursor.getInt(cursor.getColumnIndex(ArticleDAO._ID))) {
                position = iterator;
                break;
            }
            iterator++;
        } while (cursor.moveToNext());

        cursor.moveToPosition(cursorPosition);

        return position;
    }

    private class ArticlePagerAdapter extends FragmentStatePagerAdapter {

        private final String[] mProjection;
        private Cursor mCursor;

        public ArticlePagerAdapter(FragmentManager fm, String[] projection, Cursor cursor) {
            super(fm);
            mProjection = projection;
            mCursor = cursor;
        }

        @Override
        public SherlockFragment getItem(int position) {
            if (mCursor == null) {
                return null;
            }

            mCursor.moveToPosition(position);
            SherlockFragment fragment;
            try {
                fragment = ArticleFragment.newInstance();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
            Bundle args = new Bundle();
            for (int i = 0; i < mProjection.length; ++i) {
                args.putString(mProjection[i], mCursor.getString(i));
            }
            fragment.setArguments(args);
            return fragment;
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

        public int getArticleID(int position) {
            int cursorPosition = mCursor.getPosition();

            mCursor.moveToPosition(position);
            int articleID = mCursor.getInt(mCursor.getColumnIndex(ArticleDAO._ID));
            mCursor.moveToPosition(cursorPosition);

            return articleID;
        }

    }

}
