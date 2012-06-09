package de.hdodenhof.feedreader.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.handler.FeedHandler;
import de.hdodenhof.feedreader.helper.SAXHelper;
import de.hdodenhof.feedreader.model.Feed;

public class AddFeedTask extends AsyncTask<String, Void, Void> {
    Handler mainUIHandler;
    Context applicationContext;
    Feed feed;

    public AddFeedTask(Handler mainUIHandler, Context context) {
        this.mainUIHandler = mainUIHandler;
        this.applicationContext = context;
        this.feed = new Feed();
    }

    protected Void doInBackground(String... params) {

        feed.setUrl(params[0]);
        FeedController feedController = new FeedController(applicationContext);

        try {
            SAXHelper saxHelper = new SAXHelper(feed.getUrl(), new FeedHandler());
            String name = (String) saxHelper.parse();

            feed.setName(name);
            feedController.addFeed(feed);

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
        msg.what = 1;
        mainUIHandler.sendMessage(msg);
    }
}
