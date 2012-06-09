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

    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;
    private String[] allColumns = { SQLiteHelper.ARTICLE_TABLE_COLUMN_ID, SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID, SQLiteHelper.ARTICLE_TABLE_COLUMN_GUID,
            SQLiteHelper.ARTICLE_TABLE_COLUMN_PUBDATE, SQLiteHelper.ARTICLE_TABLE_COLUMN_TITLE, SQLiteHelper.ARTICLE_TABLE_COLUMN_SUMMARY,
            SQLiteHelper.ARTICLE_TABLE_COLUMN_CONTENT, SQLiteHelper.ARTICLE_TABLE_COLUMN_READ };
    private SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ArticlesDataSource(Context context) {
        dbHelper = new SQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void createArticle(long feedId, String guid, Date pubDate, String title, String summary, String content, boolean read) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID, feedId);
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_GUID, guid);
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_PUBDATE, fromDate(pubDate));
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_TITLE, title);
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_SUMMARY, summary);
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_CONTENT, content);
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_READ, fromBoolean(read));

        database.insert(SQLiteHelper.ARTICLE_TABLE_NAME, null, values);
    }

    public void createArticles(long feedId, ArrayList<Article> articles) {
        database.beginTransaction();
        try {
            for (Article article : articles) {
                createArticle(feedId, article.getGuid(), article.getPubDate(), article.getTitle(), article.getSummary(), article.getContent(), article.isRead());
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

    }

    public void deleteArticle(Article article) {
        database.delete(SQLiteHelper.ARTICLE_TABLE_NAME, SQLiteHelper.ARTICLE_TABLE_COLUMN_ID + " = " + article.getId(), null);
    }

    public void deleteArticles(long feedid) {
        database.delete(SQLiteHelper.ARTICLE_TABLE_NAME, SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID + " = " + feedid, null);
    }

    public Article getArticle(long b) {
        Cursor cursor = database.query(SQLiteHelper.ARTICLE_TABLE_NAME, allColumns, SQLiteHelper.ARTICLE_TABLE_COLUMN_ID + " = " + b, null, null, null, null);

        cursor.moveToFirst();
        Article newArticle = cursorToArticle(cursor);
        cursor.close();

        return newArticle;
    }

    public List<Article> getAllArticles(long feedid) {
        List<Article> articles = new ArrayList<Article>();

        Cursor cursor = database.query(SQLiteHelper.ARTICLE_TABLE_NAME, allColumns, SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID + " = " + feedid, null, null,
                null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Article article = cursorToArticle(cursor);
            articles.add(article);
            cursor.moveToNext();
        }
        cursor.close();

        return articles;
    }
    
    public void setRead(long id){
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_READ, 1);
        
        database.update(SQLiteHelper.ARTICLE_TABLE_NAME, values, SQLiteHelper.ARTICLE_TABLE_COLUMN_ID + " = " + id, null);
    }

    private String fromDate(Date date) {
        return iso8601Format.format(date);
    }

    private Date toDate(String date) {
        try {
            return iso8601Format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }

    }
    
    private int fromBoolean(boolean bool){
        if (bool == true){
            return 1;
        } else {
            return 0;
        }
    }
    
    private boolean toBoolean(int integer){
        if (integer == 1){
            return true;
        } else {
            return false;
        }
    }

    private Article cursorToArticle(Cursor cursor) {
        Article article = new Article();
        article.setId(cursor.getLong(0));
        article.setFeedId(cursor.getLong(1));
        article.setGuid(cursor.getString(2));
        article.setPubDate(toDate(cursor.getString(3)));
        article.setTitle(cursor.getString(4));
        article.setSummary(cursor.getString(5));
        article.setContent(cursor.getString(6));
        article.setRead(toBoolean(cursor.getInt(7)));
        return article;
    }
}