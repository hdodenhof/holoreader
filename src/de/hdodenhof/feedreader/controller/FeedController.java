package de.hdodenhof.feedreader.controller;

import java.sql.SQLException;
import java.util.ArrayList;

import android.content.Context;
import de.hdodenhof.feedreader.dao.FeedsDataSource;
import de.hdodenhof.feedreader.model.Feed;

public class FeedController {
    private FeedsDataSource datasource;
    
    public FeedController(Context context){
        datasource = new FeedsDataSource(context);
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
    
    public void addFeed(Feed feed){
        connect();
        datasource.createFeed(feed.getName(), feed.getUrl());
        disconnect();
    }
    
}
