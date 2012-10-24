package de.hdodenhof.holoreader.fragments;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
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
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.listadapters.EditFeedAdapter;
import de.hdodenhof.holoreader.provider.RSSContentProvider;
import de.hdodenhof.holoreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.holoreader.provider.SQLiteHelper.FeedDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class EditFeedsFragment extends CustomListFragment implements LoaderCallbacks<Cursor> {

    @SuppressWarnings("unused")
    private static final String TAG = EditFeedsFragment.class.getSimpleName();
    private static final int LOADER = 10;
    private static final String BUNDLE_CHECKEDITEMS = "checkeditems";

    private ProgressDialog mSpinner;
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

        this.setListAdapter(mFeedAdapter);
        this.setLoadingText(getResources().getString(R.string.LoadingFeeds));

        if (savedInstanceState != null) {
            if (savedInstanceState.getBooleanArray(BUNDLE_CHECKEDITEMS) != null) {
                boolean[] checkedItems = savedInstanceState.getBooleanArray("checkeditems");
                int checkedItemsCount = 0;

                for (int i = 0; i < checkedItems.length; i++) {
                    mFeedsListView.setItemChecked(i, checkedItems[i]);
                    if (checkedItems[i]) {
                        checkedItemsCount++;
                    }
                }
                if (checkedItemsCount > 0) {
                    updateActionMode(checkedItemsCount);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        SparseBooleanArray checkedItemPositions = mFeedsListView.getCheckedItemPositions();
        boolean[] checkedItems = new boolean[mFeedsListView.getCount()];
        for (int i = 0; i < mFeedsListView.getCount(); i++) {
            checkedItems[i] = checkedItemPositions.get(i, false);
        }

        savedInstanceState.putBooleanArray(BUNDLE_CHECKEDITEMS, checkedItems);
    }

    /*
     * For some reason getCheckedItemIds returns an empty array on some API levels when items where checked using setItemChecked(), so we need to provide the
     * checkedCount (this is only the case when called from onCreate() to restore state, all other calls are done using updateActionMode() without parameters)
     */
    private void updateActionMode(int checkedCount) {
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
    }

    private void updateActionMode() {
        updateActionMode(getCheckedItemCount());
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL, FeedDAO.UPDATED, FeedDAO.UNREAD };
        CursorLoader cursorLoader = new CursorLoader(getActivity(), RSSContentProvider.URI_FEEDS, projection, null, null, FeedDAO.NAME + " ASC");
        return cursorLoader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFeedAdapter.swapCursor(data);

        setEmptyText(getResources().getString(R.string.NoFeeds));
        setLoadingFinished();
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mFeedAdapter.swapCursor(null);
    }

    private class FeedOnItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (getCheckedItemCount() > 0) {
                updateActionMode();
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
            return mFeedsListView.getCheckedItemIds().length;
        }
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
            long[] checkedFeedIDs = mFeedsListView.getCheckedItemIds();
            switch (item.getItemId()) {
            case R.id.item_delete:
                mSpinner = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.EditFeedDeleteSpinner), true);
                (new DeleteFeedsTask()).execute(checkedFeedIDs);
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

    private class DeleteFeedsTask extends AsyncTask<long[], Void, Void> {
        @Override
        protected Void doInBackground(long[]... params) {
            ContentResolver contentResolver = getActivity().getContentResolver();
            for (long feedID : params[0]) {
                contentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.FEEDID + " = ?", new String[] { String.valueOf(feedID) });
                contentResolver.delete(RSSContentProvider.URI_FEEDS, FeedDAO._ID + " = ?", new String[] { String.valueOf(feedID) });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mSpinner.dismiss();
            mActionMode.finish();
        }

    }
}
