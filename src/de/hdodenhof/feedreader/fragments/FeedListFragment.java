package de.hdodenhof.feedreader.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.listadapters.RSSFeedAdapter;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.Helpers;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class FeedListFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {

    @SuppressWarnings("unused")
    private static final String TAG = FeedListFragment.class.getSimpleName();
    private static final String PREFS_NAME = "Feedreader";
    private static final int LOADER = 10;

    private SimpleCursorAdapter mFeedAdapter;
    private ListView mFeedsListView;
    private boolean mUnreadOnly = true;

    @SuppressLint("NewApi")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SharedPreferences preferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mUnreadOnly = preferences.getBoolean("unreadonly", true);

        getActivity().getSupportLoaderManager().initLoader(LOADER, null, this);

        String[] uiBindFrom = { FeedDAO.NAME, FeedDAO.UPDATED, FeedDAO.UNREAD };
        int[] uiBindTo = { R.id.list_item_feed_name, R.id.list_item_feed_updated, R.id.list_item_feed_unread };
        mFeedAdapter = new RSSFeedAdapter(getActivity(), R.layout.listitem_feed, null, uiBindFrom, uiBindTo, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        mFeedsListView = getListView();
        mFeedsListView.addHeaderView(getHeaderView());
        mFeedsListView.setOnItemClickListener((OnItemClickListener) getActivity());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            mFeedsListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        this.setEmptyText(getResources().getString(R.string.LoadingFeeds));
        this.setListAdapter(mFeedAdapter);

        ((FragmentCallback) getActivity()).onFragmentReady(this);

    }

    private View getHeaderView() {
        View headerView = getActivity().getLayoutInflater().inflate(R.layout.listitem_feed, null);
        TextView updatedView = (TextView) headerView.findViewById(R.id.list_item_feed_updated);
        TextView titleView = (TextView) headerView.findViewById(R.id.list_item_feed_name);

        titleView.setText(getResources().getString(R.string.AllFeeds));
        updatedView.setVisibility(View.GONE);
        return Helpers.addBackgroundIndicator(getActivity(), headerView, R.attr.customActivatedBackgroundIndicator);
    }

    public void setChoiceModeSingle() {
        mFeedsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    public void setUnreadOnly(boolean unreadOnly) {
        mUnreadOnly = unreadOnly;
        getActivity().getSupportLoaderManager().restartLoader(LOADER, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL, FeedDAO.UPDATED, FeedDAO.UNREAD };
        String selection = null;
        if (mUnreadOnly) {
            selection = FeedDAO.UNREAD + " > 0";
        }
        // for some reason using SelectionArgs in this query won't work
        CursorLoader cursorLoader = new CursorLoader(getActivity(), RSSContentProvider.URI_FEEDS, projection, selection, null, FeedDAO.UPDATED + " DESC");
        return cursorLoader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFeedAdapter.swapCursor(data);
        this.setEmptyText(getResources().getString(R.string.NoUnreadFeeds));
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mFeedAdapter.swapCursor(null);
    }

}
