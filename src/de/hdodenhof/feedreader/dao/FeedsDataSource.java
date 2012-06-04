package de.hdodenhof.feedreader.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.feedreader.helper.SQLiteHelper;
import de.hdodenhof.feedreader.model.Feed;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class FeedsDataSource {

    // Database fields
    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;
    private String[] allColumns = { SQLiteHelper.FEED_TABLE_COLUMN_ID, SQLiteHelper.FEED_TABLE_COLUMN_NAME, SQLiteHelper.FEED_TABLE_COLUMN_URL };

    public FeedsDataSource(Context context) {
        dbHelper = new SQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Feed createFeed(String feedName, String feedUrl) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.FEED_TABLE_COLUMN_NAME, feedName);
        values.put(SQLiteHelper.FEED_TABLE_COLUMN_URL, feedUrl);

        long insertId = database.insert(SQLiteHelper.FEED_TABLE_NAME, null, values);

        Cursor cursor = database.query(SQLiteHelper.FEED_TABLE_NAME, allColumns, SQLiteHelper.FEED_TABLE_COLUMN_ID + " = " + insertId, null, null, null, null);

        cursor.moveToFirst();
        Feed newFeed = cursorToFeed(cursor);
        cursor.close();

        return newFeed;
    }
  
    
    public void deleteFeed(Feed feed) {
        long id = feed.getId();

        database.delete(SQLiteHelper.FEED_TABLE_NAME, SQLiteHelper.FEED_TABLE_COLUMN_ID + " = " + id, null);
    }
    
    public Feed getFeed(long b){
        Cursor cursor = database.query(SQLiteHelper.FEED_TABLE_NAME, allColumns, SQLiteHelper.FEED_TABLE_COLUMN_ID + " = " + b, null, null, null, null);

        cursor.moveToFirst();
        Feed newFeed = cursorToFeed(cursor);
        cursor.close();

        return newFeed;
    }

    public List<Feed> getAllFeeds() {
        List<Feed> feeds = new ArrayList<Feed>();

        Cursor cursor = database.query(SQLiteHelper.FEED_TABLE_NAME, allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Feed feed = cursorToFeed(cursor);
            feeds.add(feed);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return feeds;
    }

    private Feed cursorToFeed(Cursor cursor) {
        Feed feed = new Feed();
        feed.setId(cursor.getLong(0));
        feed.setName(cursor.getString(1));
        feed.setUrl(cursor.getString(2));
        return feed;
    }
}