package de.hdodenhof.feedreader.daos;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import de.hdodenhof.feedreader.helpers.SQLiteHelper;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class FeedDAO {

        @SuppressWarnings("unused")
        private static final String TAG = FeedDAO.class.getSimpleName();          
        
        public static final String TABLE = "feeds";
        public static final String _ID = "_id";
        public static final String NAME = "name";
        public static final String URL = "url";
        public static final String UPDATED = "updated";

        private SQLiteHelper mDBHelper;

        public FeedDAO(Context context) {
                mDBHelper = new SQLiteHelper(context);
        }

        public ArrayList<Feed> getAll() {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                Cursor mCursor = mDatabase.query(TABLE, null, null, null, null, null, null);
                ArrayList<Feed> mFeeds = new ArrayList<Feed>();

                while (mCursor.moveToNext()) {
                        Feed mFeed = cursorToFeed(mCursor);
                        mFeeds.add(mFeed);
                }
                mCursor.close();
                mDBHelper.close();

                return mFeeds;
        }

        public Feed get(int id) {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                Cursor mCursor = mDatabase.query(TABLE, null, _ID + " = " + id, null, null, null, null);
                Feed mFeed = null;

                if (mCursor.moveToFirst()) {
                        mFeed = cursorToFeed(mCursor);
                }
                mCursor.close();
                mDBHelper.close();

                return mFeed;
        }

        public long insert(Feed feed) {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                ContentValues mValues = new ContentValues();

                if (feed.getId() > 0) {
                        mValues.put(_ID, feed.getId());
                }
                mValues.put(NAME, feed.getName());
                mValues.put(URL, feed.getUrl());
                mValues.put(UPDATED, SQLiteHelper.fromDate(feed.getUpdated()));

                long mInsertID = mDatabase.insert(TABLE, null, mValues);

                Cursor mCursor = mDatabase.query(TABLE, null, _ID + " = " + mInsertID, null, null, null, null);
                mCursor.close();
                mDBHelper.close();

                return mInsertID;
        }

        public int update(Feed feed) {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                ContentValues mValues = new ContentValues();
                mValues.put(_ID, feed.getId());
                mValues.put(NAME, feed.getName());
                mValues.put(URL, feed.getUrl());
                mValues.put(UPDATED, SQLiteHelper.fromDate(feed.getUpdated()));

                int mNum = mDatabase.update(TABLE, mValues, _ID + "=?", new String[] { Integer.toString(feed.getId()) });
                mDatabase.close();

                return mNum;
        }

        public void delete(int id) {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                mDatabase.delete(TABLE, _ID + "=?", new String[] { Integer.toString(id) });
                mDBHelper.close();
        }

        public void delete(Feed feed) {
                delete(feed.getId());
        }

        public void deleteAll() {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                mDatabase.delete(TABLE, null, null);
                mDatabase.close();
        }

        private Feed cursorToFeed(Cursor cursor) {
                Feed mFeed = new Feed();
                mFeed.setId(cursor.getInt(cursor.getColumnIndex(_ID)));
                mFeed.setName(cursor.getString(cursor.getColumnIndex(NAME)));
                mFeed.setUrl(cursor.getString(cursor.getColumnIndex(URL)));
                mFeed.setUpdated(SQLiteHelper.toDate(cursor.getString(cursor.getColumnIndex(UPDATED))));
                return mFeed;
        }
}