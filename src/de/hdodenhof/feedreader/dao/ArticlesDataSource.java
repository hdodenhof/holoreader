package de.hdodenhof.feedreader.dao;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import de.hdodenhof.feedreader.helper.SQLiteHelper;
import de.hdodenhof.feedreader.model.Article;

public class ArticlesDataSource {

        private SQLiteDatabase mDatabase;
        private SQLiteHelper mDBHelper;
        private String[] mAllColumns = { SQLiteHelper.ARTICLE_TABLE_COLUMN_ID, SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID,
                        SQLiteHelper.ARTICLE_TABLE_COLUMN_GUID, SQLiteHelper.ARTICLE_TABLE_COLUMN_PUBDATE, SQLiteHelper.ARTICLE_TABLE_COLUMN_TITLE,
                        SQLiteHelper.ARTICLE_TABLE_COLUMN_SUMMARY, SQLiteHelper.ARTICLE_TABLE_COLUMN_CONTENT, SQLiteHelper.ARTICLE_TABLE_COLUMN_READ };
        private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public ArticlesDataSource(Context context) {
                mDBHelper = new SQLiteHelper(context);
        }

        public void open() throws SQLException {
                mDatabase = mDBHelper.getWritableDatabase();
        }

        public void close() {
                mDBHelper.close();
        }

        public void createArticle(long feedId, String guid, Date pubDate, String title, String summary, String content, boolean read) {
                ContentValues mValues = new ContentValues();
                mValues.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID, feedId);
                mValues.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_GUID, guid);
                mValues.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_PUBDATE, fromDate(pubDate));
                mValues.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_TITLE, title);
                mValues.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_SUMMARY, summary);
                mValues.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_CONTENT, content);
                mValues.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_READ, fromBoolean(read));

                mDatabase.insert(SQLiteHelper.ARTICLE_TABLE_NAME, null, mValues);
        }

        public void createArticles(long feedId, ArrayList<Article> articles) {
                mDatabase.beginTransaction();
                try {
                        for (Article mArticle : articles) {
                                createArticle(feedId, mArticle.getGuid(), mArticle.getPubDate(), mArticle.getTitle(), mArticle.getSummary(),
                                                mArticle.getContent(), mArticle.isRead());
                        }
                        mDatabase.setTransactionSuccessful();
                } finally {
                        mDatabase.endTransaction();
                }

        }

        public void deleteArticle(Article article) {
                mDatabase.delete(SQLiteHelper.ARTICLE_TABLE_NAME, SQLiteHelper.ARTICLE_TABLE_COLUMN_ID + " = " + article.getId(), null);
        }

        public void deleteArticles(long feedid) {
                mDatabase.delete(SQLiteHelper.ARTICLE_TABLE_NAME, SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID + " = " + feedid, null);
        }

        public Article getArticle(long b) {
                Cursor cursor = mDatabase.query(SQLiteHelper.ARTICLE_TABLE_NAME, mAllColumns, SQLiteHelper.ARTICLE_TABLE_COLUMN_ID + " = " + b, null, null,
                                null, null);

                cursor.moveToFirst();
                Article newArticle = cursorToArticle(cursor);
                cursor.close();

                return newArticle;
        }

        public List<Article> getAllArticles(long feedid) {
                List<Article> mArticles = new ArrayList<Article>();

                Cursor mCursor = mDatabase.query(SQLiteHelper.ARTICLE_TABLE_NAME, mAllColumns, SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID + " = " + feedid, null,
                                null, null, SQLiteHelper.ARTICLE_TABLE_COLUMN_PUBDATE + " desc");

                mCursor.moveToFirst();
                while (!mCursor.isAfterLast()) {
                        Article article = cursorToArticle(mCursor);
                        mArticles.add(article);
                        mCursor.moveToNext();
                }
                mCursor.close();

                return mArticles;
        }

        public List<Article> getAllArticles() {
                List<Article> mArticles = new ArrayList<Article>();

                Cursor mCursor = mDatabase.query(SQLiteHelper.ARTICLE_TABLE_NAME, mAllColumns, null, null, null, null,
                                SQLiteHelper.ARTICLE_TABLE_COLUMN_PUBDATE + " desc");

                mCursor.moveToFirst();
                while (!mCursor.isAfterLast()) {
                        Article article = cursorToArticle(mCursor);
                        mArticles.add(article);
                        mCursor.moveToNext();
                }
                mCursor.close();

                return mArticles;
        }

        public void setRead(long id) {
                ContentValues mValues = new ContentValues();
                mValues.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_READ, 1);

                mDatabase.update(SQLiteHelper.ARTICLE_TABLE_NAME, mValues, SQLiteHelper.ARTICLE_TABLE_COLUMN_ID + " = " + id, null);
        }

        private String fromDate(Date date) {
                return mDateFormat.format(date);
        }

        private Date toDate(String date) {
                try {
                        return mDateFormat.parse(date);
                } catch (ParseException e) {
                        e.printStackTrace();
                        return new Date();
                }

        }

        private int fromBoolean(boolean bool) {
                if (bool == true) {
                        return 1;
                } else {
                        return 0;
                }
        }

        private boolean toBoolean(int integer) {
                if (integer == 1) {
                        return true;
                } else {
                        return false;
                }
        }

        private Article cursorToArticle(Cursor cursor) {
                Article mArticle = new Article();
                mArticle.setId(cursor.getLong(0));
                mArticle.setFeedId(cursor.getLong(1));
                mArticle.setGuid(cursor.getString(2));
                mArticle.setPubDate(toDate(cursor.getString(3)));
                mArticle.setTitle(cursor.getString(4));
                mArticle.setSummary(cursor.getString(5));
                mArticle.setContent(cursor.getString(6));
                mArticle.setRead(toBoolean(cursor.getInt(7)));
                return mArticle;
        }
}