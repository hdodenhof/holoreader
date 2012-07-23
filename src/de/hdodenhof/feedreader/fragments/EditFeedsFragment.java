package de.hdodenhof.feedreader.fragments;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.listadapters.EditFeedAdapter;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class EditFeedsFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {

    @SuppressWarnings("unused")
    private static final String TAG = EditFeedsFragment.class.getSimpleName();
    private static final int LOADER = 10;

    private ListView mFeedsListView;
    private SimpleCursorAdapter mFeedAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {

        }

        String[] uiBindFrom = { FeedDAO.NAME, FeedDAO.URL };
        int[] uiBindTo = { R.id.list_item_editfeed_title, R.id.list_item_editfeed_url };

        getActivity().getSupportLoaderManager().initLoader(LOADER, null, this);

        mFeedAdapter = new EditFeedAdapter(getActivity(), R.layout.listitem_editfeed, null, uiBindFrom, uiBindTo, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        mFeedsListView = getListView();
        mFeedsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mFeedsListView.setOnItemClickListener(new FeedOnItemClickListener());

        this.setEmptyText("Loading feeds...");
        this.setListAdapter(mFeedAdapter);

        // Setting this programmatic to be able to handle API level differences
        mFeedsListView.setSelector(R.drawable.listview_selector);

        ((FragmentCallback) getActivity()).onFragmentReady(this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] mProjection = { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL, FeedDAO.UPDATED, FeedDAO.UNREAD };
        CursorLoader mCursorLoader = new CursorLoader(getActivity(), RSSContentProvider.URI_FEEDS, mProjection, null, null, FeedDAO.NAME + " ASC");
        return mCursorLoader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFeedAdapter.swapCursor(data);
        this.setEmptyText("No feeds");
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mFeedAdapter.swapCursor(null);
    }

    private ActionMode mActionMode;
    private boolean mActionViewVisible = false;

    private class FeedOnItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final int checkedCount = getCheckedItemCount();

            if (checkedCount > 0) {
                if (mActionMode == null || mActionViewVisible == false) {
                    mActionMode = ((SherlockFragmentActivity) getActivity()).startActionMode(new FeedCallback());
                    mActionViewVisible = true;
                }

                MenuItem mEdit = mActionMode.getMenu().getItem(0);

                switch (checkedCount) {
                case 1:
                    mActionMode.setSubtitle("1 feed selected");
                    if (!mEdit.isVisible()) {
                        mActionMode.getMenu().getItem(0).setVisible(true);
                    }
                    break;
                default:
                    mActionMode.setSubtitle("" + checkedCount + " feeds selected");
                    if (mEdit.isVisible()) {
                        mActionMode.getMenu().getItem(0).setVisible(false);
                    }
                    break;
                }
            } else {
                mActionMode.finish();
                mActionViewVisible = false;
            }

        }

    }

    @SuppressLint("NewApi")
    private int getCheckedItemCount() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return mFeedsListView.getCheckedItemCount();
        } else {
            return getCheckedItemIds().length;
        }
    }

    @SuppressLint("NewApi")
    private long[] getCheckedItemIds() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            return mFeedsListView.getCheckedItemIds();
        } else {
            SparseBooleanArray mCheckedPositions = mFeedsListView.getCheckedItemPositions();
            long[] mIDs = new long[mCheckedPositions.size()];
            for (int i = 0; i < mCheckedPositions.size(); i++) {
                mIDs[i] = mFeedsListView.getItemIdAtPosition(mCheckedPositions.keyAt(i));
            }
            return mIDs;
        }
    }

    private class FeedCallback implements Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.editfeed_context, menu);

            mode.setTitle("Select feeds to delete");

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            long[] mCheckItems = getCheckedItemIds();
            switch (item.getItemId()) {
            case R.id.item_delete:
                // TODO Loading animation
                ContentResolver mContentResolver = getActivity().getContentResolver();
                for (long mItem : mCheckItems) {
                    mContentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.FEEDID + " = ?", new String[] { String.valueOf(mItem) });
                    mContentResolver.delete(RSSContentProvider.URI_FEEDS, FeedDAO._ID + " = ?", new String[] { String.valueOf(mItem) });
                }
                mode.finish();
                return true;
            default:
                return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            SparseBooleanArray mCheckedPositions = mFeedsListView.getCheckedItemPositions();
            for (int i = 0; i < mCheckedPositions.size(); i++) {
                mFeedsListView.setItemChecked(mCheckedPositions.keyAt(i), false);
            }
            mActionViewVisible = false;
        }
    }
}
