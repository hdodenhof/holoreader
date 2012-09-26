package de.hdodenhof.feedreader.activities;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HeaderViewListAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.fragments.ArticleListFragment;
import de.hdodenhof.feedreader.fragments.DynamicDialogFragment;
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
import de.hdodenhof.feedreader.services.RefreshFeedService;
import de.hdodenhof.feedreader.tasks.AddFeedTask;

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
    private MenuItem mRefreshItem;
    private boolean mTwoPane = false;
    private boolean mUnreadOnly;
    private int mSelectedFeed = -1;

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
                // return from adding a feed
                target.callbackFeedAdded(msg.arg1);
                break;
            case 8:
                // return from adding a feed with error condition
                target.callbackFeedAddedError(msg.arg1);
                break;
            default:
                break;
            }
        }
    };

    /**
     * 
     */
    private void callbackFeedAdded(int feedID) {
        mSpinner.dismiss();
        refreshFeed(feedID);
    }

    /**
     * 
     */
    private void callbackFeedAddedError(int returnCondition) {
        mSpinner.dismiss();
        switch (returnCondition) {
        case AddFeedTask.ERROR_NOFEED:
            Helpers.showDialog(this, "An error occured", "No feed found at supplied URL");
            break;
        case AddFeedTask.ERROR_NOCONTENT:
            Helpers.showDialog(this, "An error occured", "The supplied feed is not compatible");
            break;
        case AddFeedTask.ERROR_IOEXCEPTION:
        case AddFeedTask.ERROR_XMLPULLPARSEREXCEPTION:
            Helpers.showDialog(this, "An error occured", "Something went wrong while adding the feed");
            break;
        default:
            break;
        }
    }

    private BroadcastReceiver mFeedsRefreshedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mRefreshItem.getActionView().clearAnimation();
            mRefreshItem.setActionView(null);

            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putLong("refreshed", (new Date()).getTime());
            editor.commit();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mUnreadOnly = mPreferences.getBoolean("unreadonly", true);

        mResources = getResources();

        mFeedListFragment = (FeedListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_feedlist);
        mArticleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articlelist);
        if (mArticleListFragment != null) {
            mTwoPane = true;
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        if (action == Intent.ACTION_VIEW) {
            String url = intent.getData().toString();
            URL parsedUrl = parseUrl(url);

            if (parsedUrl != null) {
                addFeed(parsedUrl);
            } else {
                Helpers.showDialog(HomeActivity.this, "Error adding feed", "Invalid URL");
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("selectedFeed", mSelectedFeed);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mSelectedFeed = savedInstanceState.getInt("selectedFeed");
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("de.hdodenhof.feedreader.FEEDS_REFRESHED");
        registerReceiver(mFeedsRefreshedReceiver, filter);

        mUnreadOnly = mPreferences.getBoolean("unreadonly", true);
        invalidateOptionsMenu();

        mFeedListFragment.setUnreadOnly(mUnreadOnly);
        if (mTwoPane) {
            mArticleListFragment.setUnreadOnly(mUnreadOnly);
        }

        boolean refreshing = mPreferences.getBoolean("refreshing", false);
        if (refreshing) {
            mRefreshItem.setActionView(R.layout.actionview_refresh);
        }

        long mRefreshed = mPreferences.getLong("refreshed", (new Date(0)).getTime());
        if (mRefreshed < (new Date()).getTime() - 3600000) {
            refreshFeeds(false);
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mFeedsRefreshedReceiver);
        super.onPause();
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
    private void addFeed(URL feedUrl) {
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
        boolean refreshing = mPreferences.getBoolean("refreshing", false);
        if (!refreshing) {
            mRefreshItem.setActionView(R.layout.actionview_refresh);
        }

        Intent intent = new Intent(this, RefreshFeedService.class);

        intent.putExtra("feedid", feedID);
        startService(intent);
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
            DynamicDialogFragment dialogFragment = DynamicDialogFragment.Factory.getInstance(this);

            dialogFragment.setTitle(mResources.getString(R.string.AddFeedDialogTitle));
            dialogFragment.setLayout(R.layout.fragment_dialog_add);
            dialogFragment.setPositiveButtonListener(new DynamicDialogFragment.OnClickListener() {
                @Override
                public void onClick(DialogFragment df, String tag, SparseArray<String> map) {
                    URL parsedUrl = parseUrl(map.get(R.id.enterUrl));

                    if (parsedUrl != null) {
                        addFeed(parsedUrl);
                        df.dismiss();
                    } else {
                        Helpers.showDialog(HomeActivity.this, "Error adding feed", "Invalid URL");
                    }
                }
            });

            dialogFragment.show(getSupportFragmentManager(), "dialog");
        } else {
            Helpers.showDialog(this, mResources.getString(R.string.NoConnectionTitle), mResources.getString(R.string.NoConnectionText));
        }
    }

    /**
     * 
     * @param url
     * @return
     */
    private URL parseUrl(String url) {
        URL parsedUrl = null;

        if (!url.substring(0, 7).equalsIgnoreCase("http://")) {
            url = "http://" + url;
        }

        Pattern pattern = Helpers.PATTERN_WEB;
        Matcher matcher = pattern.matcher(url);

        try {
            if (matcher.matches()) {
                parsedUrl = new URL(url);
            }
        } catch (MalformedURLException e) {
            return null;
        }

        return parsedUrl;
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
            if (position == 0) {
                mSelectedFeed = -1;
            } else {
                cursor = ((RSSFeedAdapter) adapter).getCursor();
                cursor.moveToPosition(position - 1);
                mSelectedFeed = cursor.getInt(cursor.getColumnIndex(FeedDAO._ID));
            }

            if (mTwoPane) {
                mArticleListFragment.selectFeed(mSelectedFeed);
            } else {
                Intent intent = new Intent(this, DisplayFeedActivity.class);
                intent.putExtra("feedid", mSelectedFeed);
                startActivity(intent);
            }

            break;

        // DualPane layout only
        case RSSAdapter.TYPE_ARTICLE:
            cursor = ((RSSArticleAdapter) adapter).getCursor();
            cursor.moveToPosition(position);
            int articleID = cursor.getInt(cursor.getColumnIndex(ArticleDAO._ID));

            Intent intent = new Intent(this, DisplayFeedActivity.class);
            intent.putExtra("articleid", articleID);
            intent.putExtra("feedid", mSelectedFeed);
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
            menu.getItem(2).setIcon(R.drawable.ab_btn_checkbox_checked);
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
                item.setIcon(R.drawable.ab_btn_checkbox_unchecked);
            } else {
                Toast.makeText(this, mResources.getString(R.string.ToastAllArticles), Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.ab_btn_checkbox_checked);
            }
            mFeedListFragment.setUnreadOnly(mUnreadOnly);
            if (mTwoPane) {
                mArticleListFragment.setUnreadOnly(mUnreadOnly);
            }
            return true;
        case R.id.item_markread:
            if (mTwoPane && mSelectedFeed != -1) {
                MarkReadRunnable markReadRunnable = new MarkReadRunnable((Context) this);
                markReadRunnable.setFeed(mSelectedFeed);
                new Thread(markReadRunnable).start();
                if (mUnreadOnly) {
                    mArticleListFragment.selectFeed(-1);
                }
            } else {
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