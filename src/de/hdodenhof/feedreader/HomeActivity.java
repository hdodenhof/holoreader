package de.hdodenhof.feedreader;

import java.sql.SQLException;
import java.util.ArrayList;

import de.hdodenhof.feedreader.adapter.FeedAdapter;
import de.hdodenhof.feedreader.controller.ProfileController;
import de.hdodenhof.feedreader.dao.FeedsDataSource;
import de.hdodenhof.feedreader.model.Feed;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class HomeActivity extends Activity implements OnItemClickListener {

    private ProfileController pc = new ProfileController();
    private FeedsDataSource datasource;

    private int listViewPosition = -1;
    private ArrayAdapter<Feed> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri uri = intent.getData();

        datasource = new FeedsDataSource(this);
        try {
            datasource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }        
        
        if (Intent.ACTION_VIEW.equals(action) && uri != null){
            Feed feed = null;
            feed = datasource.createFeed(uri.toString(), uri.toString());
        }

        ArrayList<Feed> values = (ArrayList<Feed>) datasource.getAllFeeds();

        // Use the SimpleCursorAdapter to show the
        // elements in a ListView
        adapter = new FeedAdapter(this, values);

        ListView feedlistview = (ListView) findViewById(R.id.feed_listView);
        feedlistview.setAdapter(adapter);
        feedlistview.setOnItemClickListener(this);

//        registerForContextMenu(getListView());
        
        feedlistview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        feedlistview.setMultiChoiceModeListener(new MyMultiChoiceModeListener());

    }

    private class MyMultiChoiceModeListener implements MultiChoiceModeListener {
        
            private ArrayList<Feed> toDelete;

            public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                  long id, boolean checked) {
                // Here you can do something when items are selected/de-selected,
                // such as update the title in the CAB
                Feed feed = (Feed) adapter.getItem(position);
                
                if(checked) {
                    toDelete.add(feed);
                } else {
                    toDelete.remove(feed);
                }
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                // Respond to clicks on the actions in the CAB
                switch (item.getItemId()) {
                    case R.id.item_delete:

                        for (Feed feed : toDelete) {
                            datasource.deleteFeed(feed);
                            adapter.remove(feed);
                        }
                        
                        adapter.notifyDataSetChanged(); 
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    default:
                        return false;
                }
            }

            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate the menu for the CAB
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.feed_context, menu);
                
                toDelete = new ArrayList<Feed>();
                return true;
            }

             public void onDestroyActionMode(ActionMode mode) {
                // Here you can make any necessary updates to the activity when
                // the CAB is removed. By default, selected items are deselected/unchecked.
            }

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Here you can perform updates to the CAB due to
                // an invalidate() request
                return false;
            }
    }

    // public void sendMessage(View view) {
    // Intent intent = new Intent(this, DisplayFeedActivity.class);
    // startActivity(intent);
    // }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.item_add:
            showDialog();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void showDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Add feed");
        alert.setMessage("Input Feed URL");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setText("http://www.gruenderszene.de/feed/");

        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();

                Feed feed = null;
                feed = datasource.createFeed(value, value);
                adapter.add(feed);

                adapter.notifyDataSetChanged();

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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        listViewPosition = ((AdapterContextMenuInfo) menuInfo).position;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.feed_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        
        switch (item.getItemId()) {
        case R.id.item_delete:
            
            Feed feed = (Feed) adapter.getItem(listViewPosition);
            datasource.deleteFeed(feed);
            
            adapter.remove(feed);
            adapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

}