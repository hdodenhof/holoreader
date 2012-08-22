package de.hdodenhof.feedreader.fragments;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.SparseArray;
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

    private SimpleCursorAdapter mFeedAdapter;
    private ListView mFeedsListView;
    private ActionMode mActionMode;
    private boolean mActionViewVisible = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String[] uiBindFrom = { FeedDAO.NAME, FeedDAO.URL };
        int[] uiBindTo = { R.id.list_item_editfeed_name, R.id.list_item_editfeed_url };

        getActivity().getSupportLoaderManager().initLoader(LOADER, null, this);

        mFeedAdapter = new EditFeedAdapter(getActivity(), R.layout.listitem_editfeed, null, uiBindFrom, uiBindTo, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        mFeedsListView = getListView();
        mFeedsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mFeedsListView.setOnItemClickListener(new FeedOnItemClickListener());

        this.setEmptyText(getResources().getString(R.string.LoadingFeeds));
        this.setListAdapter(mFeedAdapter);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL, FeedDAO.UPDATED, FeedDAO.UNREAD };
        CursorLoader cursorLoader = new CursorLoader(getActivity(), RSSContentProvider.URI_FEEDS, projection, null, null, FeedDAO.NAME + " ASC");
        return cursorLoader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFeedAdapter.swapCursor(data);
        this.setEmptyText(getResources().getString(R.string.NoFeeds));
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mFeedAdapter.swapCursor(null);
    }

    private class FeedOnItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final int checkedCount = getCheckedItemCount();

            if (checkedCount > 0) {
                if (mActionMode == null || mActionViewVisible == false) {
                    mActionMode = ((SherlockFragmentActivity) getActivity()).startActionMode(new FeedCallback());
                    mActionViewVisible = true;
                }

                MenuItem edit = mActionMode.getMenu().getItem(0);

                String feedsFound = getResources().getQuantityString(R.plurals.numberOfFeedsSelected, checkedCount, checkedCount);
                mActionMode.setSubtitle(feedsFound);

                if (checkedCount == 1) {
                    if (!edit.isVisible()) {
                        edit.setVisible(true);
                    }
                } else {
                    if (edit.isVisible()) {
                        edit.setVisible(false);
                    }
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
            SparseBooleanArray checkedPositions = mFeedsListView.getCheckedItemPositions();
            long[] ids = new long[checkedPositions.size()];
            for (int i = 0; i < checkedPositions.size(); i++) {
                ids[i] = mFeedsListView.getItemIdAtPosition(checkedPositions.keyAt(i));
            }
            return ids;
        }
    }

    protected void updateFeedName(long id, String name) {

    }

    private class FeedCallback implements Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.editfeed_context, menu);

            mode.setTitle(getResources().getString(R.string.EditFeedsCABTitle));

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            long[] checkFeedIDs = getCheckedItemIds();
            switch (item.getItemId()) {
            case R.id.item_delete:
                ContentResolver contentResolver = getActivity().getContentResolver();
                for (long feedID : checkFeedIDs) {
                    contentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.FEEDID + " = ?", new String[] { String.valueOf(feedID) });
                    contentResolver.delete(RSSContentProvider.URI_FEEDS, FeedDAO._ID + " = ?", new String[] { String.valueOf(feedID) });
                }
                mode.finish();
                return true;
            case R.id.item_edit:
                Cursor feed = (Cursor) mFeedsListView.getItemAtPosition(mFeedsListView.getCheckedItemPositions().keyAt(0));
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

                SparseArray<String> initialValues = new SparseArray<String>();
                initialValues.put(R.id.txt_feedname, feed.getString(feed.getColumnIndex(FeedDAO.NAME)));

                DynamicDialogFragment dialogFragment = DynamicDialogFragment.Factory.getInstance(getActivity());
                dialogFragment.setLayout(R.layout.fragment_dialog_edit);
                dialogFragment.setTitle(getResources().getString(R.string.EditFeedDialogTitle));
                dialogFragment.setInitialValues(initialValues);
                dialogFragment.setTag(feed.getString(feed.getColumnIndex(FeedDAO._ID)));
                dialogFragment.setPositiveButtonListener(new DynamicDialogFragment.OnClickListener() {
                    @Override
                    public void onClick(DialogFragment df, String tag, SparseArray<String> fieldMap) {
                        ContentResolver contentResolver = getActivity().getContentResolver();
                        ContentValues contentValues = new ContentValues();

                        contentValues.put(FeedDAO.NAME, fieldMap.get(R.id.txt_feedname));
                        contentResolver.update(RSSContentProvider.URI_FEEDS, contentValues, FeedDAO._ID + " = ?", new String[] { tag });
                        df.dismiss();
                    }
                });
                dialogFragment.show(fragmentManager, "dialog");
                mode.finish();
                return true;
            default:
                return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            SparseBooleanArray checkedPositions = mFeedsListView.getCheckedItemPositions();
            for (int i = 0; i < checkedPositions.size(); i++) {
                mFeedsListView.setItemChecked(checkedPositions.keyAt(i), false);
            }
            mActionViewVisible = false;
        }
    }
}
