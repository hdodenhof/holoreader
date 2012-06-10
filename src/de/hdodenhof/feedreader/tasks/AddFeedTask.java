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
        Handler mMainUIHandler;
        Context mContext;
        Feed mFeed;

        public AddFeedTask(Handler mainUIHandler, Context context) {
                this.mMainUIHandler = mainUIHandler;
                this.mContext = context;
                this.mFeed = new Feed();
        }

        protected Void doInBackground(String... params) {

                mFeed.setUrl(params[0]);
                FeedController mFeedController = new FeedController(mContext);

                try {
                        SAXHelper mSAXHelper = new SAXHelper(mFeed.getUrl(), new FeedHandler());
                        String mName = (String) mSAXHelper.parse();

                        mFeed.setName(mName);
                        mFeedController.addFeed(mFeed);

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
                Message mMSG = Message.obtain();
                mMSG.what = 1;
                mMainUIHandler.sendMessage(mMSG);
        }
}
