package de.hdodenhof.feedreader;

import java.sql.SQLException;
import java.util.ArrayList;

import de.hdodenhof.feedreader.adapter.FeedAdapter;
import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.dao.FeedsDataSource;
import de.hdodenhof.feedreader.model.Feed;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class HomeActivity extends Activity implements OnItemClickListener {

    private FeedsDataSource datasource;
    private FeedController feedcontroller;

    private ArrayAdapter<Feed> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri uri = intent.getData();

        feedcontroller = new FeedController(this);
        datasource = new FeedsDataSource(this);
        try {
            datasource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            datasource.createFeed(uri.toString(), uri.toString());
        }

        ArrayList<Feed> values = (ArrayList<Feed>) datasource.getAllFeeds();

        adapter = new FeedAdapter(this, values);

        ListView feedlistview = (ListView) findViewById(R.id.feed_listView);
        feedlistview.setAdapter(adapter);
        feedlistview.setOnItemClickListener(this);

        feedlistview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        feedlistview.setMultiChoiceModeListener(new MyMultiChoiceModeListener());

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
                    datasource.deleteFeed(feed);
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
            feedcontroller.refresh();
            return true;
        case R.id.item_add:
            showDialog();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void addFeed(Feed feed){
        adapter.add(feed);
        adapter.notifyDataSetChanged();       
    }
    
    
    private void showDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Add feed");
        alert.setMessage("Input Feed URL");

        final EditText input = new EditText(this);
        input.setText("http://www.gruenderszene.de/feed/");

        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                feedcontroller.addFeed(value);
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

}