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
import android.view.View;
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
    private ArrayList<String> mArticles;
    private boolean mUnreadOnly = true;
    private boolean mTwoPane = false;
    private boolean mThisIsPrimaryFragment = false;
    private boolean mScrollTop = false;
    private int mChangeToPosition = -1;
    private int mFeedID = -1;
    private int mCurrentState;

    public void selectFeed(int feedID) {
        mFeedID = feedID;
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

    @SuppressLint("NewApi")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCurrentState = STATE_LOADING;

        if (savedInstanceState != null) {

        }

        if (getActivity().getIntent().hasExtra("feedid")) {
            mFeedID = getActivity().getIntent().getIntExtra("feedid", mFeedID);
        }

        if (getActivity().getIntent().hasExtra("articles")) {
            mArticles = getActivity().getIntent().getStringArrayListExtra("articles");
        }

        SharedPreferences preferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mUnreadOnly = preferences.getBoolean("unreadonly", true);

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            mArticlesListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        ((FragmentCallback) getActivity()).onFragmentReady(this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        String selectionArgs[] = null;
        String[] projection = { ArticleDAO._ID, ArticleDAO.FEEDID, ArticleDAO.FEEDNAME, ArticleDAO.TITLE, ArticleDAO.SUMMARY, ArticleDAO.IMAGE,
                ArticleDAO.PUBDATE, ArticleDAO.READ };

        if (mTwoPane) {
            // DualPane
            if (mThisIsPrimaryFragment) {
                // FeedListActivity, articleIDs passed in Intent
                selection = ArticleDAO._ID + " IN (";
                for (int i = 0; i < mArticles.size() - 1; i++) {
                    selection = selection + "?, ";
                }
                selection = selection + "?)";
                selectionArgs = mArticles.toArray(new String[mArticles.size()]);
            } else {
                // HomeActivity
                if (mFeedID == -1) {
                    // first call no feedID in Intent
                    if (mUnreadOnly) {
                        selection = "read = 0";
                    }
                } else {
                    // feedID passed in Intent
                    selection = ArticleDAO.FEEDID + " = ?";
                    selectionArgs = new String[] { String.valueOf(mFeedID) };
                    if (mUnreadOnly) {
                        selection = selection + " AND read = 0";
                    }
                }
            }
        } else {
            // SinglePane, feedID passed in Intent
            if (mFeedID != -1) {
                selection = ArticleDAO.FEEDID + " = ?";
                selectionArgs = new String[] { String.valueOf(mFeedID) };
            }
            if (mUnreadOnly) {
                if (selection == null) {
                    selection = "read = 0";
                } else {
                    selection = selection + " AND read = 0";
                }
            }
            mArticles = new ArrayList<String>();

            ContentResolver contentResolver = getActivity().getContentResolver();
            Cursor cursor = contentResolver.query(RSSContentProvider.URI_ARTICLES, new String[] { ArticleDAO._ID }, selection, selectionArgs, null);

            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    mArticles.add(cursor.getString(cursor.getColumnIndex(ArticleDAO._ID)));
                } while (cursor.moveToNext());
            }
            cursor.close();

            selection = ArticleDAO._ID + " IN (";
            for (int i = 0; i < mArticles.size() - 1; i++) {
                selection = selection + "?, ";
            }
            selection = selection + "?)";
            selectionArgs = mArticles.toArray(new String[mArticles.size()]);
        }

        return new CursorLoader(getActivity(), RSSContentProvider.URI_ARTICLES, projection, selection, selectionArgs, ArticleDAO.PUBDATE + " DESC");
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
