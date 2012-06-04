package de.hdodenhof.feedreader.controller;

import java.sql.SQLException;
import java.util.ArrayList;

import android.content.Context;
import de.hdodenhof.feedreader.dao.ArticlesDataSource;
import de.hdodenhof.feedreader.model.Article;

public class ArticleController {
    private ArticlesDataSource datasource;
    private Context context;
    
    public ArticleController(Context context){
        this.context = context;
        
        datasource = new ArticlesDataSource(this.context);
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
    
}
