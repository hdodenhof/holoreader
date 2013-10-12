package de.hdodenhof.holoreader.fragments;

import java.util.Date;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.listadapters.RSSArticleAdapter;
import de.hdodenhof.holoreader.misc.Extras;
import de.hdodenhof.holoreader.misc.FragmentCallback;
import de.hdodenhof.holoreader.misc.Helpers;
import de.hdodenhof.holoreader.misc.Prefs;
import de.hdodenhof.holoreader.provider.RSSContentProvider;
import de.hdodenhof.holoreader.provider.SQLiteHelper;
import de.hdodenhof.holoreader.provider.SQLiteHelper.ArticleDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class ArticleListFragment extends CustomListFragment implements LoaderCallbacks<Cursor> {

    @SuppressWarnings("unused")
    private static final String TAG = ArticleListFragment.class.getSimpleName();
    private static final int LOADER = 20;
    private static final int STATE_LOADING = 1;
    private static final int STATE_LOADED = 2;

    private static final String BUNDLE_SELECTED_FEED = "selectedFeed";

    private ListView mArticlesListView;
    private RSSArticleAdapter mArticleAdapter;
    private boolean mUnreadOnly = true;
    private boolean mScrollTop = false;
    private boolean mIsLargeDevice = false;
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
                int mPosition = (position - 1 < 0) ? 0 : (mIsLargeDevice ? position : position - 1);
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
            mFeedID = savedInstanceState.getInt(BUNDLE_SELECTED_FEED);
        } else if (getActivity().getIntent().hasExtra(Extras.FEEDID)) {
            mFeedID = getActivity().getIntent().getIntExtra(Extras.FEEDID, mFeedID);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUnreadOnly = preferences.getBoolean(Prefs.UNREAD_ONLY, true);

        boolean isPrimaryFragment = ((FragmentCallback) getActivity()).isPrimaryFragment(this);
        boolean isTwoPane = ((FragmentCallback) getActivity()).isDualPane();

        String layoutSize = getString(R.string.LayoutSize);
        mIsLargeDevice = layoutSize.equals("large") || layoutSize.equals("xlarge");

        String[] uiBindFrom = { ArticleDAO.TITLE, ArticleDAO.IMAGE, ArticleDAO.READ };
        int[] uiBindTo = { R.id.list_item_entry_title, R.id.list_item_entry_image, R.layout.listitem_article };

        getActivity().getSupportLoaderManager().initLoader(LOADER, null, this);

        boolean isLargeDeviceInPortrait = (mIsLargeDevice && (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT));
        mArticleAdapter = new RSSArticleAdapter(getActivity(), null, uiBindFrom, uiBindTo, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER, (isTwoPane
                && !isPrimaryFragment && !isLargeDeviceInPortrait) ? RSSArticleAdapter.MODE_EXTENDED : RSSArticleAdapter.MODE_COMPACT, isTwoPane);

        this.setListAdapter(mArticleAdapter);
        this.setLoadingText(getString(R.string.LoadingArticles));

        mArticlesListView = getListView();
        mArticlesListView.setOnItemClickListener((OnItemClickListener) getActivity());

        ((FragmentCallback) getActivity()).onFragmentReady(this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(BUNDLE_SELECTED_FEED, mFeedID);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        String selectionArgs[] = null;
        String[] projection = { ArticleDAO._ID, ArticleDAO.FEEDID, ArticleDAO.FEEDNAME, ArticleDAO.TITLE, ArticleDAO.SUMMARY, ArticleDAO.IMAGE,
                ArticleDAO.PUBDATE, ArticleDAO.READ };

        selection = ArticleDAO.ISDELETED + " = ?";
        selectionArgs = new String[] { "0" };

        if (mUnreadOnly) {
            selection = selection + " AND (" + ArticleDAO.READ + " > ? OR " + ArticleDAO.READ + " IS NULL)";
            selectionArgs = Helpers.addSelectionArg(selectionArgs, SQLiteHelper.fromDate(new Date()));
        }
        if (mFeedID != -1) {
            selection = selection + " AND " + ArticleDAO.FEEDID + " = ?";
            selectionArgs = Helpers.addSelectionArg(selectionArgs, String.valueOf(mFeedID));
        }
        return new CursorLoader(getActivity(), RSSContentProvider.URI_ARTICLES, projection, selection, selectionArgs, ArticleDAO.PUBDATE + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mArticleAdapter.swapCursor(data);
        if (mScrollTop) {
            mArticlesListView.setSelection(0);
            mScrollTop = false;
        }

        if (mUnreadOnly) {
            setEmptyText(getString(R.string.NoUnreadArticles));
        } else {
            setEmptyText(getString(R.string.NoArticles));
        }

        setLoadingFinished();

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
