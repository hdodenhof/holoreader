package de.hdodenhof.feedreader.fragments;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.listadapters.RSSArticleAdapter;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class ArticleListFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {

    @SuppressWarnings("unused")
    private static final String TAG = ArticleListFragment.class.getSimpleName();
    private static final String PREFS_NAME = "Feedreader";
    private static final int LOADER = 20;
    private static final int STATE_LOADING = 1;
    private static final int STATE_LOADED = 2;

    private ListView mArticlesListView;
    private RSSArticleAdapter mArticleAdapter;
    private boolean mUnreadOnly = true;
    private boolean mTwoPane = false;
    private boolean mThisIsPrimaryFragment = false;
    private int mCurrentState;
    private int mChangeToPosition = -1;

    private int mSelectedFeed = 0;
    private ArrayList<String> mArticles;
    private boolean mScrollTop = false;

    public void selectFeed(int feedID) {
        mSelectedFeed = feedID;
        mScrollTop = true;
        getActivity().getSupportLoaderManager().restartLoader(LOADER, null, this);
    }

    @SuppressLint("NewApi")
    public void changePosition(int position) {
        if (mCurrentState == STATE_LOADED) {
            if (mArticlesListView.getCheckedItemPosition() != position) {
                int mPosition = (position - 1 < 0) ? 0 : position - 1;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    mArticlesListView.smoothScrollToPositionFromTop(mPosition, 0, 500);
                    mArticlesListView.setItemChecked(position, true);
                } else {
                    mArticlesListView.setSelection(position);
                }
            }
        } else {
            mChangeToPosition = position;
        }
    }

    public void setChoiceModeSingle() {
        mArticlesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    public void setUnreadOnly(boolean unreadOnly) {
        mUnreadOnly = unreadOnly;
        getActivity().getSupportLoaderManager().restartLoader(LOADER, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCurrentState = STATE_LOADING;

        if (savedInstanceState != null) {

        }

        if (getActivity().getIntent().hasExtra("feedid")) {
            mSelectedFeed = getActivity().getIntent().getIntExtra("feedid", mSelectedFeed);
        }

        if (getActivity().getIntent().hasExtra("articles")) {
            mArticles = getActivity().getIntent().getStringArrayListExtra("articles");
        }

        SharedPreferences mPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mUnreadOnly = mPreferences.getBoolean("unreadonly", true);

        mThisIsPrimaryFragment = ((FragmentCallback) getActivity()).isPrimaryFragment(this);
        mTwoPane = ((FragmentCallback) getActivity()).isDualPane();

        String[] uiBindFrom = { ArticleDAO.TITLE, ArticleDAO.IMAGE, ArticleDAO.READ };
        int[] uiBindTo = { R.id.list_item_entry_title, R.id.list_item_entry_image, R.layout.listitem_article };

        getActivity().getSupportLoaderManager().initLoader(LOADER, null, this);

        mArticleAdapter = new RSSArticleAdapter(getActivity(), null, uiBindFrom, uiBindTo, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER,
                (mTwoPane && !mThisIsPrimaryFragment) ? RSSArticleAdapter.MODE_EXTENDED : RSSArticleAdapter.MODE_COMPACT, mTwoPane ? true : false);

        this.setEmptyText(getResources().getString(R.string.LoadingArticles));
        this.setListAdapter(mArticleAdapter);
        mArticlesListView = getListView();

        mArticlesListView.setOnItemClickListener((OnItemClickListener) getActivity());

        ((FragmentCallback) getActivity()).onFragmentReady(this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String mSelection = null;
        String mSelectionArgs[] = null;

        String[] mProjection = { ArticleDAO._ID, ArticleDAO.FEEDID, ArticleDAO.FEEDNAME, ArticleDAO.TITLE, ArticleDAO.SUMMARY, ArticleDAO.IMAGE,
                ArticleDAO.PUBDATE, ArticleDAO.READ };
        CursorLoader mCursorLoader = null;

        if (mTwoPane) {
            // DualPane
            if (mThisIsPrimaryFragment) {
                // FeedListActivity
                // articles in Intent

                mSelection = ArticleDAO._ID + " IN (";
                for (int i = 0; i < mArticles.size() - 1; i++) {
                    mSelection = mSelection + "?, ";
                }
                mSelection = mSelection + "?)";
                mSelectionArgs = mArticles.toArray(new String[mArticles.size()]);

                mCursorLoader = new CursorLoader(getActivity(), RSSContentProvider.URI_ARTICLES, mProjection, mSelection, mSelectionArgs, ArticleDAO.PUBDATE
                        + " DESC");

            } else {
                // HomeActivity
                if (mSelectedFeed == 0) {
                    // first call no feedID in Intent

                    if (mUnreadOnly) {
                        mSelection = "read = 0";
                    }

                    mCursorLoader = new CursorLoader(getActivity(), RSSContentProvider.URI_ARTICLES, mProjection, mSelection, mSelectionArgs,
                            ArticleDAO.PUBDATE + " DESC");

                } else {
                    // feedID in Intent

                    mSelection = ArticleDAO.FEEDID + " = ?";
                    mSelectionArgs = new String[] { String.valueOf(mSelectedFeed) };

                    if (mUnreadOnly) {
                        mSelection = mSelection + " AND read = 0";
                    }

                    mCursorLoader = new CursorLoader(getActivity(), RSSContentProvider.URI_ARTICLES, mProjection, mSelection, mSelectionArgs,
                            ArticleDAO.PUBDATE + " DESC");

                }
            }
        } else {
            // SinglePane
            // feedID in Intent

            if (mSelectedFeed != 0) {
                mSelection = ArticleDAO.FEEDID + " = ?";
                mSelectionArgs = new String[] { String.valueOf(mSelectedFeed) };
            }

            if (mUnreadOnly) {
                if (mSelection == null) {
                    mSelection = "read = 0";
                } else {
                    mSelection = mSelection + " AND read = 0";
                }
            }

            mArticles = new ArrayList<String>();

            ContentResolver mContentResolver = getActivity().getContentResolver();
            Cursor mCursor = mContentResolver.query(RSSContentProvider.URI_ARTICLES, new String[] { ArticleDAO._ID }, mSelection, mSelectionArgs, null);

            if (mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                do {
                    mArticles.add(mCursor.getString(mCursor.getColumnIndex(ArticleDAO._ID)));
                } while (mCursor.moveToNext());
            }
            mCursor.close();

            mSelection = ArticleDAO._ID + " IN (";
            for (int i = 0; i < mArticles.size() - 1; i++) {
                mSelection = mSelection + "?, ";
            }
            mSelection = mSelection + "?)";
            mSelectionArgs = mArticles.toArray(new String[mArticles.size()]);

            mCursorLoader = new CursorLoader(getActivity(), RSSContentProvider.URI_ARTICLES, mProjection, mSelection, mSelectionArgs, ArticleDAO.PUBDATE
                    + " DESC");

        }

        return mCursorLoader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mArticleAdapter.swapCursor(data);
        if (mScrollTop) {
            mArticlesListView.scrollTo(0, 0);
            mScrollTop = false;
        }
        this.setEmptyText(getResources().getString(R.string.NoUnreadArticles));

        mCurrentState = STATE_LOADED;

        if (mChangeToPosition != -1) {
            changePosition(mChangeToPosition);
            mChangeToPosition = -1;
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mArticleAdapter.swapCursor(null);
    }
}
