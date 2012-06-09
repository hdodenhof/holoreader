package de.hdodenhof.feedreader;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import de.hdodenhof.feedreader.adapter.FeedAdapter;
import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.model.Feed;
import de.hdodenhof.feedreader.tasks.AddFeedTask;
import de.hdodenhof.feedreader.tasks.RefreshFeedsTask;

public class HomeActivity extends Activity implements OnItemClickListener {

    private FeedController feedcontroller;
    private ArrayAdapter<Feed> adapter;
    private ProgressDialog spinner;
    private ProgressDialog progressDialog;

    Handler asyncHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case 1:
                // added feed
                adapter.clear();
                adapter.addAll(feedcontroller.getAllFeeds());
                adapter.notifyDataSetChanged();
                spinner.dismiss();
                break;
            case 2:
                // updated single feed
                spinner.dismiss();
                break;
            case 3:
                // refreshed feeds
                progressDialog.dismiss();
            case 9:
                // refresh progress bar
                progressDialog.setProgress(msg.arg1);
            default:
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri uri = intent.getData();

        feedcontroller = new FeedController(this);

        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            addFeed(uri.toString());
        }

        ArrayList<Feed> values = feedcontroller.getAllFeeds();
        adapter = new FeedAdapter(this, values);

        ListView feedlistview = (ListView) findViewById(R.id.feed_listView);
        feedlistview.setAdapter(adapter);
        feedlistview.setOnItemClickListener(this);

        feedlistview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        feedlistview.setMultiChoiceModeListener(new MyMultiChoiceModeListener());

    }

    private void addFeed(String feedUrl) {
        spinner = ProgressDialog.show(this, "", "Please wait...", true);
        AddFeedTask addFeedTask = new AddFeedTask(asyncHandler, this);
        addFeedTask.execute(feedUrl);
    }

    // private void updateFeed(Feed feed) {
    // spinner = ProgressDialog.show(this, "", "Please wait...", true);
    // UpdateFeedTask updateFeedTask = new UpdateFeedTask(asyncHandler, this);
    // updateFeedTask.execute(feed);
    // }

    private void refreshFeeds() {

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.setProgress(0);
        progressDialog.setMax(adapter.getCount());
        progressDialog.show();

        RefreshFeedsTask refreshFeedsTask = new RefreshFeedsTask(asyncHandler, this);
        refreshFeedsTask.execute();
    }

    private void showAddDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Add feed");
        alert.setMessage("Input Feed URL");

        final EditText input = new EditText(this);
        input.setText("http://t3n.de/news/feed");
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                addFeed(value);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Feed feed = (Feed) parent.getItemAtPosition(position);
        Intent intent = new Intent(getApplicationContext(), DisplayFeedActivity.class);

        intent.putExtra("feedid", feed.getId());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.item_refresh:
            refreshFeeds();
            return true;
        case R.id.item_add:
            showAddDialog();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private class MyMultiChoiceModeListener implements MultiChoiceModeListener {

        private ArrayList<Feed> toDelete;

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            Feed feed = (Feed) adapter.getItem(position);

            if (checked) {
                toDelete.add(feed);
            } else {
                toDelete.remove(feed);
            }
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            case R.id.item_delete:

                for (Feed feed : toDelete) {
                    feedcontroller.deleteFeed(feed);
                    adapter.remove(feed);
                }

                adapter.notifyDataSetChanged();
                mode.finish();
                return true;
            default:
                return false;
            }
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.feed_context, menu);

            toDelete = new ArrayList<Feed>();
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }

}