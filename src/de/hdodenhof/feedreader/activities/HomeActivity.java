package de.hdodenhof.feedreader.activities;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.HeaderViewListAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.fragments.ArticleListFragment;
import de.hdodenhof.feedreader.fragments.FeedListFragment;
import de.hdodenhof.feedreader.listadapters.RSSAdapter;
import de.hdodenhof.feedreader.listadapters.RSSArticleAdapter;
import de.hdodenhof.feedreader.listadapters.RSSFeedAdapter;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.Helpers;
import de.hdodenhof.feedreader.misc.MarkReadRunnable;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;
import de.hdodenhof.feedreader.tasks.AddFeedTask;
import de.hdodenhof.feedreader.tasks.RefreshFeedTask;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class HomeActivity extends SherlockFragmentActivity implements FragmentCallback, OnItemClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String PREFS_NAME = "Feedreader";

    private SharedPreferences mPreferences;
    private Resources mResources;
    private ArticleListFragment mArticleListFragment;
    private FeedListFragment mFeedListFragment;
    private ProgressDialog mSpinner;
    private HashSet<Integer> mFeedsUpdating;
    private MenuItem mRefreshItem;
    private boolean mTwoPane = false;
    private boolean mUnreadOnly;

    /**
     * Handles messages from AsyncTasks started within this activity
     */
    private Handler mAsyncHandler = new AsynHandler(this);

    private static class AsynHandler extends Handler {
        private final WeakReference<HomeActivity> targetReference;

        AsynHandler(HomeActivity target) {
            targetReference = new WeakReference<HomeActivity>(target);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            HomeActivity target = targetReference.get();

            switch (msg.what) {
            case 1:
                // added feed
                target.callbackFeedAdded(msg.arg1);
                break;
            case 2:
                // feed refresh
                target.callbackFeedRefresh(msg.arg1);
                break;
            case 9:
                // something went wrong while adding a feed
                target.callbackError();
                break;
            default:
                break;
            }
        }
    };

    /**
     * Update feed list and dismiss spinner after new feed has been added
     */
    private void callbackFeedAdded(int feedID) {
        mSpinner.dismiss();
        refreshFeed(feedID);
    }

    /**
     * Update list of running tasks and dismiss spinner when all tasks are done
     */
    @SuppressLint("NewApi")
    private void callbackFeedRefresh(int feedID) {
        mFeedsUpdating.remove(feedID);
        if (mFeedsUpdating.size() == 0) {
            mRefreshItem.getActionView().clearAnimation();
            mRefreshItem.setActionView(null);
        }
    }

    /**
     * Show error message when adding feed went wrong
     */
    private void callbackError() {
        mSpinner.dismiss();
        Helpers.showDialog(this, "An error occured", "Something went wrong while adding the feed");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mUnreadOnly = mPreferences.getBoolean("unreadonly", true);

        mResources = getResources();
        mFeedsUpdating = new HashSet<Integer>();

        mFeedListFragment = (FeedListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_feedlist);
        mArticleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
        if (mArticleListFragment != null) {
            mTwoPane = true;
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        if (action == Intent.ACTION_VIEW) {
            Uri data = intent.getData();
            addFeed(data.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUnreadOnly = mPreferences.getBoolean("unreadonly", true);
        invalidateOptionsMenu();

        mFeedListFragment.setUnreadOnly(mUnreadOnly);
        if (mTwoPane) {
            mArticleListFragment.setUnreadOnly(mUnreadOnly);
        }

        if (mFeedsUpdating.size() != 0) {
            mRefreshItem.setActionView(R.layout.actionview_refresh);
        }

        long mRefreshed = mPreferences.getLong("refreshed", (new Date(0)).getTime());
        if (mRefreshed < (new Date()).getTime() - 3600000) {
            refreshFeeds(false);
        }
    }

    /**
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#onFragmentReady(android.support.v4.app.Fragment)
     */
    @Override
    public void onFragmentReady(Fragment fragment) {
        if (mTwoPane && fragment instanceof FeedListFragment) {
            ((FeedListFragment) fragment).setChoiceModeSingle();
        }
    }

    /**
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#isDualPane()
     */
    @Override
    public boolean isDualPane() {
        return mTwoPane;
    }

    /**
     * @see de.hdodenhof.feedreader.misc.FragmentCallback#isPrimaryFragment(android.support.v4.app.Fragment)
     */
    @Override
    public boolean isPrimaryFragment(Fragment fragment) {
        return fragment instanceof FeedListFragment;
    }

    /**
     * Starts an AsyncTask to fetch a new feed and add it to the database
     * 
     * @param feedUrl
     *            URL of the feed to fetch
     */
    private void addFeed(String feedUrl) {
        mSpinner = ProgressDialog.show(this, "", mResources.getString(R.string.PleaseWait), true);
        AddFeedTask addFeedTask = new AddFeedTask(mAsyncHandler, this);
        addFeedTask.execute(feedUrl);
    }

    /**
     * Spawns AsyncTasks to refresh all feeds
     * 
     * @param item
     *            MenuItem that holds the refresh animation
     */
    private void refreshFeeds(boolean forced) {
        boolean isConnected = Helpers.isConnected(this);

        if (isConnected) {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putLong("refreshed", (new Date()).getTime());
            editor.commit();

            for (Integer mFeedID : queryFeeds()) {
                refreshFeed(mFeedID);
            }
        } else {
            if (forced) {
                Helpers.showDialog(this, mResources.getString(R.string.NoConnectionTitle), mResources.getString(R.string.NoConnectionText));
            }
        }
    }

    /**
     * Refreshes a single feed
     * 
     * @param feedID
     */
    @SuppressLint("NewApi")
    private void refreshFeed(int feedID) {
        if (mFeedsUpdating.size() == 0) {
            mRefreshItem.setActionView(R.layout.actionview_refresh);
        }
        if (mFeedsUpdating.contains(feedID)) {
            return;
        }
        mFeedsUpdating.add(feedID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new RefreshFeedTask(mAsyncHandler, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, feedID);
        } else {
            new RefreshFeedTask(mAsyncHandler, this).execute(feedID);
        }
    }

    /**
     * Queries all feed ids
     * 
     * @return HashMap of all feed ids
     */
    private HashSet<Integer> queryFeeds() {
        HashSet<Integer> feeds = new HashSet<Integer>();

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(RSSContentProvider.URI_FEEDS, new String[] { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL }, null, null, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                feeds.add(cursor.getInt(cursor.getColumnIndex(FeedDAO._ID)));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return feeds;
    }

    /**
     * Shows a dialog to add a new feed URL
     */
    private void showAddDialog() {
        boolean isConnected = Helpers.isConnected(this);

        if (isConnected) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

            alertDialog.setTitle(mResources.getString(R.string.AddFeedDialogTitle));
            alertDialog.setMessage(mResources.getString(R.string.AddFeedDialogText));

            final EditText input = new EditText(this);
            input.setText("http://t3n.de/news/feed");
            alertDialog.setView(input);

            alertDialog.setPositiveButton(mResources.getString(R.string.PositiveButton), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input.getText().toString();
                    addFeed(value);
                }
            });

            alertDialog.setNegativeButton(mResources.getString(R.string.NegativeButton), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
            alertDialog.show();
        } else {
            Helpers.showDialog(this, mResources.getString(R.string.NoConnectionTitle), mResources.getString(R.string.NoConnectionText));
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        RSSAdapter adapter;
        Cursor cursor;

        if (parent.getAdapter() instanceof HeaderViewListAdapter) {
            HeaderViewListAdapter wrappingAdapter = (HeaderViewListAdapter) parent.getAdapter();
            adapter = (RSSAdapter) wrappingAdapter.getWrappedAdapter();
        } else {
            adapter = (RSSAdapter) parent.getAdapter();
        }

        switch (adapter.getType()) {
        case RSSAdapter.TYPE_FEED:
            int feedID;

            if (position == 0) {
                feedID = -1;
            } else {
                cursor = ((RSSFeedAdapter) adapter).getCursor();
                cursor.moveToPosition(position - 1);
                feedID = cursor.getInt(cursor.getColumnIndex(FeedDAO._ID));
            }

            if (mTwoPane) {
                mArticleListFragment.selectFeed(feedID);
            } else {
                Intent intent = new Intent(this, DisplayFeedActivity.class);
                intent.putExtra("feedid", feedID);
                startActivity(intent);
            }

            break;

        // DualPane layout only
        case RSSAdapter.TYPE_ARTICLE:
            cursor = ((RSSArticleAdapter) adapter).getCursor();
            cursor.moveToPosition(position);

            int articleID = cursor.getInt(cursor.getColumnIndex(ArticleDAO._ID));
            ArrayList<String> articles = new ArrayList<String>();

            cursor.moveToFirst();
            do {
                articles.add(cursor.getString(cursor.getColumnIndex(ArticleDAO._ID)));
            } while (cursor.moveToNext());

            Intent intent = new Intent(this, DisplayFeedActivity.class);
            intent.putExtra("articleid", articleID);
            intent.putStringArrayListExtra("articles", articles);
            startActivity(intent);
            break;

        default:
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getSupportMenuInflater();
        menuInflater.inflate(R.menu.main, menu);

        if (!mUnreadOnly) {
            menu.getItem(2).setIcon(R.drawable.checkbox_checked);
        }
        mRefreshItem = menu.getItem(0);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.item_refresh:
            refreshFeeds(true);
            return true;
        case R.id.item_add:
            showAddDialog();
            return true;
        case R.id.item_toggle:
            mUnreadOnly = !mUnreadOnly;

            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean("unreadonly", mUnreadOnly);
            editor.commit();

            if (mUnreadOnly) {
                Toast.makeText(this, mResources.getString(R.string.ToastUnreadArticles), Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.checkbox_unchecked);
            } else {
                Toast.makeText(this, mResources.getString(R.string.ToastAllArticles), Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.checkbox_checked);
            }
            mFeedListFragment.setUnreadOnly(mUnreadOnly);
            if (mTwoPane) {
                mArticleListFragment.setUnreadOnly(mUnreadOnly);
            }
            return true;
        case R.id.item_markread:
            if (!mTwoPane) {
                new Thread(new MarkReadRunnable((Context) this)).start();
            }
            return true;
        case R.id.item_editfeeds:
            startActivity(new Intent(this, EditFeedsActivity.class));
            return true;
        case R.id.item_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}