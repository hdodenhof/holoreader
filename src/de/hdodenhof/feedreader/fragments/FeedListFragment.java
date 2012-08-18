package de.hdodenhof.feedreader.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
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

    private ListView mFeedsListView;
    private SimpleCursorAdapter mFeedAdapter;
    private boolean mUnreadOnly = true;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {

        }

        SharedPreferences preferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mUnreadOnly = preferences.getBoolean("unreadonly", true);

        String[] uiBindFrom = { FeedDAO.NAME, FeedDAO.UPDATED, FeedDAO.UNREAD };
        int[] uiBindTo = { R.id.list_item_feed_name, R.id.list_item_feed_updated, R.id.list_item_feed_unread };

        getActivity().getSupportLoaderManager().initLoader(LOADER, null, this);

        mFeedAdapter = new RSSFeedAdapter(getActivity(), R.layout.listitem_feed, null, uiBindFrom, uiBindTo, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        mFeedsListView = getListView();

        View headerView = getActivity().getLayoutInflater().inflate(R.layout.listitem_feed, null);

        TextView updatedView = (TextView) headerView.findViewById(R.id.list_item_feed_updated);
        updatedView.setVisibility(View.GONE);

        TextView titleView = (TextView) headerView.findViewById(R.id.list_item_feed_name);
        titleView.setText(getResources().getString(R.string.AllFeeds));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            TypedArray attributes = getActivity().obtainStyledAttributes(new int[] { android.R.attr.activatedBackgroundIndicator });
            int resource = attributes.getResourceId(0, 0);
            attributes.recycle();

            // setBackgroundResource resets padding
            int paddingLeft = headerView.getPaddingLeft();
            int paddingTop = headerView.getPaddingTop();
            int paddingRight = headerView.getPaddingRight();
            int paddingBottom = headerView.getPaddingBottom();
            headerView.setBackgroundResource(resource);
            headerView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }

        mFeedsListView.addHeaderView(headerView);

        this.setEmptyText(getResources().getString(R.string.LoadingFeeds));
        this.setListAdapter(mFeedAdapter);

        mFeedsListView.setOnItemClickListener((OnItemClickListener) getActivity());

        ((FragmentCallback) getActivity()).onFragmentReady(this);

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
