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

        SharedPreferences mPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mUnreadOnly = mPreferences.getBoolean("unreadonly", true);

        String[] uiBindFrom = { FeedDAO.NAME, FeedDAO.UPDATED, FeedDAO.UNREAD };
        int[] uiBindTo = { R.id.list_item_feed_name, R.id.list_item_feed_updated, R.id.list_item_feed_unread };

        getActivity().getSupportLoaderManager().initLoader(LOADER, null, this);

        mFeedAdapter = new RSSFeedAdapter(getActivity(), R.layout.listitem_feed, null, uiBindFrom, uiBindTo, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        mFeedsListView = getListView();

        View mHeaderView = getActivity().getLayoutInflater().inflate(R.layout.listitem_feed, null);
        TextView mUpdatedView = (TextView) mHeaderView.findViewById(R.id.list_item_feed_updated);
        TextView mTitleView = (TextView) mHeaderView.findViewById(R.id.list_item_feed_name);
        mUpdatedView.setVisibility(View.GONE);
        mTitleView.setText(getResources().getString(R.string.all_feeds));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            TypedArray mAttributes = getActivity().obtainStyledAttributes(new int[] { android.R.attr.activatedBackgroundIndicator });
            int mResource = mAttributes.getResourceId(0, 0);
            mAttributes.recycle();

            // setBackgroundResource resets padding
            int mLeftPadding = mHeaderView.getPaddingLeft();
            int mTopPadding = mHeaderView.getPaddingTop();
            int mRightPadding = mHeaderView.getPaddingRight();
            int mBottomPadding = mHeaderView.getPaddingBottom();
            mHeaderView.setBackgroundResource(mResource);
            mHeaderView.setPadding(mLeftPadding, mTopPadding, mRightPadding, mBottomPadding);
        }

        mFeedsListView.addHeaderView(mHeaderView);

        this.setEmptyText("Loading feeds...");
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
        String[] mProjection = { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL, FeedDAO.UPDATED, FeedDAO.UNREAD };
        String mSelection = null;
        if (mUnreadOnly) {
            mSelection = FeedDAO.UNREAD + " > 0";
        }
        // for some reason using SelectionArgs in this query won't work
        CursorLoader mCursorLoader = new CursorLoader(getActivity(), RSSContentProvider.URI_FEEDS, mProjection, mSelection, null, FeedDAO.UPDATED + " DESC");
        return mCursorLoader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFeedAdapter.swapCursor(data);
        this.setEmptyText("No unread feeds");
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mFeedAdapter.swapCursor(null);
    }

}
