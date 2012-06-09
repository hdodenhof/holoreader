package de.hdodenhof.feedreader.tasks;

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.handler.ArticleHandler;
import de.hdodenhof.feedreader.helper.SAXHelper;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;

public class UpdateFeedTask extends AsyncTask<Feed, Void, Void> {

    private Feed feed;

    Handler mainUIHandler;
    Context applicationContext;

    public UpdateFeedTask(Handler mainUIHandler, Context context) {
        this.mainUIHandler = mainUIHandler;
        this.applicationContext = context;
    }

    @SuppressWarnings("unchecked")
    protected Void doInBackground(Feed... params) {

        feed = (Feed) params[0];
        ArrayList<Article> al = new ArrayList<Article>();
        ArticleController articleController = new ArticleController(applicationContext);

        try {
            SAXHelper saxHelper = new SAXHelper(feed.getUrl(), new ArticleHandler());
            al = (ArrayList<Article>) saxHelper.parse();

            articleController.deleteArticles(feed.getId());
            for (Article article : al) {
                articleController.createArticle(feed.getId(), article.getGuid(), article.getPubDate(), article.getTitle(), article.getSummary(),
                        article.getContent());
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
