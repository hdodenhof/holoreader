package de.hdodenhof.feedreader.controller;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import de.hdodenhof.feedreader.HomeActivity;
import de.hdodenhof.feedreader.dao.ArticlesDataSource;
import de.hdodenhof.feedreader.dao.FeedsDataSource;
import de.hdodenhof.feedreader.handler.FeedHandler;
import de.hdodenhof.feedreader.handler.ArticleHandler;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;

public class FeedController {
    private FeedsDataSource datasource;
    private ArrayList<Feed> feeds;
    private Context context;
    private ProgressDialog spinner;
    private ProgressDialog progressDialog;
    private Feed newFeed;
    private int refreshRunner;
    private boolean isRefreshing = false;
    
    public FeedController(Context context){
        this.context = context;
        
        datasource = new FeedsDataSource(this.context);
        connect();
        this.feeds = (ArrayList<Feed>) datasource.getAllFeeds();
        disconnect();
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
    
    public Feed getFeed(long feedid){
        connect();
        Feed feed = datasource.getFeed(feedid);
        disconnect();
        return feed;
    }
    
    public ArrayList<Feed> getAllFeeds(){
        connect();
        ArrayList<Feed> feeds = (ArrayList<Feed>) datasource.getAllFeeds();
        disconnect();
        return feeds;        
    }
    
    public void deleteFeed(Feed feed){
        connect();
        datasource.deleteFeed(feed);
        disconnect();       
    }
    
    public void addFeed(String url){
        newFeed = new Feed();
        newFeed.setUrl(url);
        
        spinner = ProgressDialog.show(this.context, "", "Please wait...", true);
        
        GetFeedNameTask getFeedNameTask = new GetFeedNameTask();
        getFeedNameTask.execute(url);        

    }
    
    public void finishAddFeed(String name){
        newFeed.setName(name);
        connect();
        newFeed = datasource.createFeed(newFeed.getName(), newFeed.getUrl());
        disconnect();
        feeds.add(newFeed);
        this.update(newFeed);
    }

    public void update(Feed feed){
        UpdateFeedTask updateFeedTask = new UpdateFeedTask();
        updateFeedTask.execute(feed);               
    }
    
    public void finishUpdate(){
        if (isRefreshing){
            refreshRunner++;
            progressDialog.setProgress(refreshRunner);            
            if(feeds.size() > refreshRunner){
                update(feeds.get(refreshRunner));
            } else {
                finishRefresh();
            }
        } else {
        spinner.dismiss();
        ((HomeActivity) context).addFeed(newFeed);
        }
    }
    
    public void refresh(){
        progressDialog = new ProgressDialog(this.context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.setProgress(0);
        progressDialog.setMax(feeds.size());        
        progressDialog.show();
        
        isRefreshing = true;
        refreshRunner = 0;
        update(feeds.get(refreshRunner));
    }
    
    public void finishRefresh(){
        refreshRunner = 0;
        isRefreshing = false;
        progressDialog.dismiss();
    }
    
    private class GetFeedNameTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... params) {

            String name = new String();

            try {

                URL rssUrl = new URL(params[0]);
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                XMLReader xmlReader = saxParser.getXMLReader();
                FeedHandler rssHandler = new FeedHandler(name);
                xmlReader.setContentHandler(rssHandler);
                InputSource inputSource = new InputSource(rssUrl.openStream());
                xmlReader.parse(inputSource);
                name = rssHandler.getName();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return name;

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected void onPostExecute(String name) {
            finishAddFeed(name);
        }
    }  
    
    private class UpdateFeedTask extends AsyncTask<Feed, Void, ArrayList<Article>> {

        private Feed feed;
        
        protected ArrayList<Article> doInBackground(Feed... params) {

            feed = (Feed) params[0];
            ArrayList<Article> al = new ArrayList<Article>();

            try {

                URL rssUrl = new URL(feed.getUrl());
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                XMLReader xmlReader = saxParser.getXMLReader();
                ArticleHandler articleHandler = new ArticleHandler(al);
                xmlReader.setContentHandler(articleHandler);
                InputSource inputSource = new InputSource(rssUrl.openStream());
                xmlReader.parse(inputSource);
                al = articleHandler.getArticles();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return al;

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected void onPostExecute(ArrayList<Article> al) {
            ArticlesDataSource articlesdatasource = new ArticlesDataSource(context);
            try {
                articlesdatasource.open();
            } catch (SQLException e) {
                e.printStackTrace();
            }            
            
            articlesdatasource.deleteArticles(feed.getId());

            for (Article article : al) {
                
                article = articlesdatasource.createArticle(feed.getId(), article.getGuid(), article.getTitle(), article.getSummary(), article.getContent());
            }
            
            articlesdatasource.close();
            finishUpdate();

        }
    }
    
}
