package de.hdodenhof.feedreader.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import de.hdodenhof.feedreader.dao.ArticlesDataSource;
import de.hdodenhof.feedreader.model.Article;

public class ArticleController {
    private ArticlesDataSource datasource;
    
    public ArticleController(Context context){
        datasource = new ArticlesDataSource(context);
    }
    
    private void connect(){
        try {
            datasource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void disconnect(){
        datasource.close();
    }    
    
    public ArrayList<Article> getAllArticles(long feedid){
        connect();
        ArrayList<Article> articles = (ArrayList<Article>) datasource.getAllArticles(feedid);
        disconnect();
        return articles;        
    }
    
    public Article getArticle(long id){
        connect();
        Article article = datasource.getArticle(id);
        disconnect();
        return article;         
    }
    
    public void deleteArticles(long feedId){
        connect();
        datasource.deleteArticles(feedId);
        disconnect();        
    }
    
    public void createArticle(long feedId, String guid, Date date, String title, String summary, String content){
        connect();
        datasource.createArticle(feedId, guid, date, title, summary, content);
        disconnect();
    }
    
    public void createArticles(long feedId, ArrayList<Article> articles){
        connect();
        datasource.createArticles(feedId, articles);
        disconnect();
    }
    
}
