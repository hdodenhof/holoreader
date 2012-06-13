package de.hdodenhof.feedreader.daos;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import de.hdodenhof.feedreader.helpers.SQLiteHelper;
import de.hdodenhof.feedreader.models.Article;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class ArticleDAO {

        @SuppressWarnings("unused")
        private static final String TAG = ArticleDAO.class.getSimpleName();         
        
        public static final String TABLE = "articles";
        public static final String _ID = "_id";
        public static final String FEEDID = "feedid";
        public static final String GUID = "guid";
        public static final String PUBDATE = "pubdate";
        public static final String TITLE = "title";
        public static final String SUMMARY = "summary";
        public static final String CONTENT = "content";
        public static final String READ = "read";

        private SQLiteHelper mDBHelper;

        public ArticleDAO(Context context) {
                mDBHelper = new SQLiteHelper(context);
        }

        public ArrayList<Article> getAll() {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                Cursor mCursor = mDatabase.query(TABLE, null, null, null, null, null, PUBDATE + " desc");
                ArrayList<Article> mArticles = new ArrayList<Article>();

                while (mCursor.moveToNext()) {
                        Article mArticle = cursorToArticle(mCursor);
                        mArticles.add(mArticle);
                }
                mCursor.close();
                mDBHelper.close();

                return mArticles;
        }
        
        public ArrayList<Article> getAllWithFeedID(int feedID) {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                Cursor mCursor = mDatabase.query(TABLE, null, FEEDID + " = " + feedID, null, null, null, PUBDATE + " desc");
                ArrayList<Article> mArticles = new ArrayList<Article>();

                while (mCursor.moveToNext()) {
                        Article mArticle = cursorToArticle(mCursor);
                        mArticles.add(mArticle);
                }
                mCursor.close();
                mDBHelper.close();

                return mArticles;
        }        

        public Article get(int id) {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                Cursor mCursor = mDatabase.query(TABLE, null, _ID + " = " + id, null, null, null, null);
                Article mArticle = null;

                if (mCursor.moveToFirst()) {
                        mArticle = cursorToArticle(mCursor);
                }
                mCursor.close();
                mDBHelper.close();

                return mArticle;
        }

        public long insert(Article article) {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                ContentValues mValues = new ContentValues();

                if (article.getId() > 0) {
                        mValues.put(_ID, article.getId());
                }
                mValues.put(FEEDID, article.getFeedId());
                mValues.put(GUID, article.getGuid());
                mValues.put(PUBDATE, SQLiteHelper.fromDate(article.getPubDate()));
                mValues.put(TITLE, article.getTitle());
                mValues.put(SUMMARY, article.getSummary());
                mValues.put(CONTENT, article.getContent());
                mValues.put(READ, SQLiteHelper.fromBoolean(false));

                long mInsertID = mDatabase.insert(TABLE, null, mValues);

                Cursor mCursor = mDatabase.query(TABLE, null, _ID + " = " + mInsertID, null, null, null, null);
                mCursor.close();
                mDBHelper.close();

                return mInsertID;
        }

        public long update(Article article) {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                ContentValues mValues = new ContentValues();

                mValues.put(_ID, article.getId());
                mValues.put(FEEDID, article.getFeedId());
                mValues.put(GUID, article.getGuid());
                mValues.put(PUBDATE, SQLiteHelper.fromDate(article.getPubDate()));
                mValues.put(TITLE, article.getTitle());
                mValues.put(SUMMARY, article.getSummary());
                mValues.put(CONTENT, article.getContent());
                mValues.put(READ, SQLiteHelper.fromBoolean(article.isRead()));

                int mNum = mDatabase.update(TABLE, mValues, _ID + "=?", new String[] { Integer.toString(article.getId()) });
                mDatabase.close();

                return mNum;
        }

        public void delete(int id) {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                mDatabase.delete(TABLE, _ID + "=?", new String[] { Integer.toString(id) });
                mDBHelper.close();
        }
        
        public void deleteWithFeedID(int id) {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                mDatabase.delete(TABLE, FEEDID + "=?", new String[] { Integer.toString(id) });
                mDBHelper.close();
        }

        public void delete(Article article) {
                delete(article.getId());
        }

        public void deleteAll() {
                SQLiteDatabase mDatabase = mDBHelper.getWritableDatabase();
                mDatabase.delete(TABLE, null, null);
                mDatabase.close();
        }

        private Article cursorToArticle(Cursor cursor) {
                Article mArticle = new Article();
                mArticle.setId(cursor.getInt(cursor.getColumnIndex(_ID)));
                mArticle.setFeedId(cursor.getInt(cursor.getColumnIndex(FEEDID)));
                mArticle.setGuid(cursor.getString(cursor.getColumnIndex(GUID)));
                mArticle.setPubDate(SQLiteHelper.toDate(cursor.getString(cursor.getColumnIndex(PUBDATE))));
                mArticle.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
                mArticle.setSummary(cursor.getString(cursor.getColumnIndex(SUMMARY)));
                mArticle.setContent(cursor.getString(cursor.getColumnIndex(CONTENT)));
                mArticle.setRead(SQLiteHelper.toBoolean(cursor.getInt(cursor.getColumnIndex(READ))));
                return mArticle;
        }
}