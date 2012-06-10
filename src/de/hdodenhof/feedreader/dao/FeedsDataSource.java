package de.hdodenhof.feedreader.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import de.hdodenhof.feedreader.helper.SQLiteHelper;
import de.hdodenhof.feedreader.model.Feed;

public class FeedsDataSource {

        // Database fields
        private SQLiteDatabase mDatabase;
        private SQLiteHelper mDBHelper;
        private String[] mAllColumns = { SQLiteHelper.FEED_TABLE_COLUMN_ID, SQLiteHelper.FEED_TABLE_COLUMN_NAME, SQLiteHelper.FEED_TABLE_COLUMN_URL };

        public FeedsDataSource(Context context) {
                mDBHelper = new SQLiteHelper(context);
        }

        public void open() throws SQLException {
                mDatabase = mDBHelper.getWritableDatabase();
        }

        public void close() {
                mDBHelper.close();
        }

        public Feed createFeed(String feedName, String feedUrl) {
                ContentValues mValues = new ContentValues();
                mValues.put(SQLiteHelper.FEED_TABLE_COLUMN_NAME, feedName);
                mValues.put(SQLiteHelper.FEED_TABLE_COLUMN_URL, feedUrl);

                long mInsertID = mDatabase.insert(SQLiteHelper.FEED_TABLE_NAME, null, mValues);

                Cursor mCursor = mDatabase.query(SQLiteHelper.FEED_TABLE_NAME, mAllColumns, SQLiteHelper.FEED_TABLE_COLUMN_ID + " = " + mInsertID, null, null,
                                null, null);

                mCursor.moveToFirst();
                Feed mFeed = cursorToFeed(mCursor);
                mCursor.close();

                return mFeed;
        }

        public void deleteFeed(Feed feed) {
                long mFeedID = feed.getId();

                mDatabase.delete(SQLiteHelper.FEED_TABLE_NAME, SQLiteHelper.FEED_TABLE_COLUMN_ID + " = " + mFeedID, null);
        }

        public Feed getFeed(long b) {
                Cursor mCursor = mDatabase.query(SQLiteHelper.FEED_TABLE_NAME, mAllColumns, SQLiteHelper.FEED_TABLE_COLUMN_ID + " = " + b, null, null, null,
                                null);

                mCursor.moveToFirst();
                Feed mFeed = cursorToFeed(mCursor);
                mCursor.close();

                return mFeed;
        }

        public List<Feed> getAllFeeds() {
                List<Feed> mFeeds = new ArrayList<Feed>();

                Cursor mCursor = mDatabase.query(SQLiteHelper.FEED_TABLE_NAME, mAllColumns, null, null, null, null, null);

                mCursor.moveToFirst();
                while (!mCursor.isAfterLast()) {
                        Feed mFeed = cursorToFeed(mCursor);
                        mFeeds.add(mFeed);
                        mCursor.moveToNext();
                }
                mCursor.close();
                return mFeeds;
        }

        private Feed cursorToFeed(Cursor cursor) {
                Feed mFeed = new Feed();
                mFeed.setId(cursor.getLong(0));
                mFeed.setName(cursor.getString(1));
                mFeed.setUrl(cursor.getString(2));
                return mFeed;
        }
}