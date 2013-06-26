package de.hdodenhof.holoreader.misc;

import java.util.Date;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.viewpagerindicator.UnderlinePageIndicator;

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.fragments.ArticleFragment;
import de.hdodenhof.holoreader.provider.RSSContentProvider;
import de.hdodenhof.holoreader.provider.SQLiteHelper;
import de.hdodenhof.holoreader.provider.SQLiteHelper.ArticleDAO;

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
    private String[] mProjection = { ArticleDAO._ID, ArticleDAO.FEEDID, ArticleDAO.FEEDNAME, ArticleDAO.TITLE, ArticleDAO.PUBDATE, ArticleDAO.LINK,
            ArticleDAO.CONTENT };
    private boolean mUnreadOnly = true;
    private Date mUnreadAfter;
    private int mPreselectedArticleID = -1;
    private int mCurrentArticleID = -1;
    private int mCurrentPosition = -1;
    private int mFeedID = -1;
    private int mCurrentState;

    public void changePosition(int position) {
        if (mPager.getCurrentItem() != position) {
            mPager.setCurrentItem(position);
        }
    }

    @SuppressLint("NewApi")
    public ArticleViewPager(SherlockFragmentActivity context) {
        mContext = context;
        mCurrentState = STATE_LOADING;

        mPreselectedArticleID = mContext.getIntent().getIntExtra("articleid", 0);
        mFeedID = mContext.getIntent().getIntExtra("feedid", 0);
        mUnreadAfter = (Date) mContext.getIntent().getSerializableExtra("unreadAfter");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mUnreadOnly = preferences.getBoolean("unreadonly", true);

        mContext.getSupportLoaderManager().initLoader(LOADER, null, this);

        mPagerAdapter = new ArticlePagerAdapter(mContext.getSupportFragmentManager(), mProjection, null);

        mPager = (ViewPager) mContext.findViewById(R.id.viewpager_article);
        mPager.setAdapter(mPagerAdapter);

        ((TextView) ((View) mPager.getParent()).findViewById(R.id.loading)).setText(R.string.LoadingArticle);

        UnderlinePageIndicator pageIndicator = (UnderlinePageIndicator) mContext.findViewById(R.id.titles);
        pageIndicator.setViewPager(mPager);
        pageIndicator.setOnPageChangeListener(this);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        int oldArticleID = mCurrentArticleID;

        mCurrentPosition = position;
        mCurrentArticleID = mPagerAdapter.getArticleID(position);

        ((OnArticleChangedListener) mContext).onArticleChanged(oldArticleID, mCurrentArticleID, position);
    }

    public String getCurrentLink() {
        return mPagerAdapter.getArticleLink(mCurrentPosition);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        String selectionArgs[] = null;

        selection = ArticleDAO.ISDELETED + " = ?";
        selectionArgs = new String[] { "0" };

        if (mUnreadOnly) {
            selection = selection + " AND (" + ArticleDAO.READ + " > ? OR " + ArticleDAO.READ + " IS NULL)";
            selectionArgs = Helpers.addSelectionArg(selectionArgs, SQLiteHelper.fromDate(mUnreadAfter));
        }

        if (mFeedID != -1) {
            selection = selection + " AND " + ArticleDAO.FEEDID + " = ?";
            selectionArgs = Helpers.addSelectionArg(selectionArgs, String.valueOf(mFeedID));
        }

        CursorLoader cursorLoader = new CursorLoader(mContext, RSSContentProvider.URI_ARTICLES, mProjection, selection, selectionArgs, ArticleDAO.PUBDATE
                + " DESC");
        return cursorLoader;
    }

    @Override
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
        ((LinearLayout) mContext.findViewById(R.id.loadingContainer)).setVisibility(View.GONE);
    }

    @Override
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

        public String getArticleLink(int position) {
            int cursorPosition = mCursor.getPosition();

            mCursor.moveToPosition(position);
            String articleLink = mCursor.getString(mCursor.getColumnIndex(ArticleDAO.LINK));
            mCursor.moveToPosition(cursorPosition);

            return "".equals(articleLink) ? null : articleLink;
        }

    }

}
