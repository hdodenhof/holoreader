package de.hdodenhof.feedreader.tasks;

import java.util.ArrayList;

import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.handler.ArticleHandler;
import de.hdodenhof.feedreader.helper.SAXHelper;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class RefreshFeedsTask extends AsyncTask<Void, Void, Void> {

    Handler mainUIHandler;
    Context applicationContext;
    
    public RefreshFeedsTask(Handler mainUIHandler, Context context){
         this.mainUIHandler = mainUIHandler;
         this.applicationContext = context;
    }
    
    @SuppressWarnings("unchecked")
    protected Void doInBackground(Void... params) {

        ArrayList<Article> articles = new ArrayList<Article>();
        ArrayList<Feed> feeds = new ArrayList<Feed>();
        FeedController feedController = new FeedController(applicationContext);
        ArticleController articleController = new ArticleController(applicationContext);

        try {
            SAXHelper saxHelper = new SAXHelper(new ArticleHandler());
             
            feeds = feedController.getAllFeeds();
            for (Feed feed : feeds) {
                saxHelper.setUrl(feed.getUrl());
                articles = (ArrayList<Article>) saxHelper.parse(); 
                articleController.deleteArticles(feed.getId());
                articleController.createArticles(feed.getId(), articles);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void result) {
        Message msg = Message.obtain();
        msg.what = 2;
        mainUIHandler.sendMessage(msg);        

    }

}
