package de.hdodenhof.feedreader.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

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
public class FeedListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    @SuppressWarnings("unused")
    private static final String TAG = FeedListFragment.class.getSimpleName();
    private static final int LOADER = 10;

    private ListView mFeedsListView;
    private SimpleCursorAdapter mFeedAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {

        }

        String[] uiBindFrom = { FeedDAO.NAME, FeedDAO.URL, FeedDAO.UPDATED, FeedDAO.UNREAD };
        int[] uiBindTo = { R.id.list_item_feed_title, R.id.list_item_feed_summary, R.id.list_item_feed_updated, R.id.list_item_feed_unread };

        getActivity().getSupportLoaderManager().initLoader(LOADER, null, this);

        mFeedAdapter = new RSSFeedAdapter(getActivity(), R.layout.listitem_feed, null, uiBindFrom, uiBindTo, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        this.setEmptyText("No feeds");
        this.setListAdapter(mFeedAdapter);
        mFeedsListView = getListView();

        mFeedsListView.setOnItemClickListener((OnItemClickListener) getActivity());

        ((FragmentCallback) getActivity()).onFragmentReady(this);

    }

    public void setChoiceModeSingle() {
        mFeedsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] mProjection = { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL, FeedDAO.UPDATED, FeedDAO.UNREAD };
        CursorLoader mCursorLoader = new CursorLoader(getActivity(), RSSContentProvider.URI_FEEDS, mProjection, null, null, null);
        return mCursorLoader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFeedAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mFeedAdapter.swapCursor(null);
    }

}
