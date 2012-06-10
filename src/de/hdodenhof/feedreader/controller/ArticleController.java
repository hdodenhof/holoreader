package de.hdodenhof.feedreader.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;

import de.hdodenhof.feedreader.dao.ArticlesDataSource;
import de.hdodenhof.feedreader.model.Article;

public class ArticleController {
        private ArticlesDataSource mArticleDataSource;

        public ArticleController(Context context) {
                mArticleDataSource = new ArticlesDataSource(context);
        }

        private void connect() {
                try {
                        mArticleDataSource.open();
                } catch (SQLException e) {
                        e.printStackTrace();
                }
        }

        private void disconnect() {
                mArticleDataSource.close();
        }

        public ArrayList<Article> getAllArticles(long feedid) {
                connect();
                ArrayList<Article> mArticles = (ArrayList<Article>) mArticleDataSource.getAllArticles(feedid);
                disconnect();
                return mArticles;
        }

        public ArrayList<Article> getAllArticles() {
                connect();
                ArrayList<Article> mArticles = (ArrayList<Article>) mArticleDataSource.getAllArticles();
                disconnect();
                return mArticles;
        }

        public Article getArticle(long id) {
                connect();
                Article mArticle = mArticleDataSource.getArticle(id);
                disconnect();
                return mArticle;
        }

        public void deleteArticles(long feedId) {
                connect();
                mArticleDataSource.deleteArticles(feedId);
                disconnect();
        }

        public void createArticle(long feedId, String guid, Date date, String title, String summary, String content) {
                connect();
                mArticleDataSource.createArticle(feedId, guid, date, title, summary, content, false);
                disconnect();
        }

        public void createArticles(long feedId, ArrayList<Article> articles) {
                connect();
                mArticleDataSource.createArticles(feedId, articles);
                disconnect();
        }

        public void setRead(long id) {
                connect();
                mArticleDataSource.setRead(id);
                disconnect();
        }

}
