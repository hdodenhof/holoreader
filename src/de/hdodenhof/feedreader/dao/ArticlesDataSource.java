package de.hdodenhof.feedreader.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.feedreader.helper.SQLiteHelper;
import de.hdodenhof.feedreader.model.Article;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ArticlesDataSource {

    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;
    private String[] allColumns = { SQLiteHelper.ARTICLE_TABLE_COLUMN_ID, SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID, SQLiteHelper.ARTICLE_TABLE_COLUMN_GUID,
            SQLiteHelper.ARTICLE_TABLE_COLUMN_TITLE, SQLiteHelper.ARTICLE_TABLE_COLUMN_SUMMARY, SQLiteHelper.ARTICLE_TABLE_COLUMN_CONTENT };

    public ArticlesDataSource(Context context) {
        dbHelper = new SQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void createArticle(long feedId, String guid, String title, String summary, String content) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID, feedId);
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_GUID, guid);
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_TITLE, title);
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_SUMMARY, summary);
        values.put(SQLiteHelper.ARTICLE_TABLE_COLUMN_CONTENT, content);

        database.insert(SQLiteHelper.ARTICLE_TABLE_NAME, null, values);
    }
    
    public void createArticles(long feedId, ArrayList<Article> articles){
        database.beginTransaction();
        try {
            for (Article article : articles) {
                createArticle(feedId, article.getGuid(), article.getTitle(), article.getSummary(), article.getContent());
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

        Cursor cursor = database.query(SQLiteHelper.ARTICLE_TABLE_NAME, allColumns, SQLiteHelper.ARTICLE_TABLE_COLUMN_FEEDID + " = " + feedid, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Article article = cursorToArticle(cursor);
            articles.add(article);
            cursor.moveToNext();
        }
        cursor.close();
        
        return articles;
    }

    private Article cursorToArticle(Cursor cursor) {
        Article article = new Article();
        article.setId(cursor.getLong(0));
        article.setFeedId(cursor.getLong(1));
        article.setGuid(cursor.getString(2));
        article.setTitle(cursor.getString(3));
        article.setSummary(cursor.getString(4));
        article.setContent(cursor.getString(5));
        return article;
    }
}