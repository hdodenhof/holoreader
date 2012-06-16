package de.hdodenhof.feedreader.tasks;

import java.util.Date;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.controllers.RSSController;
import de.hdodenhof.feedreader.handlers.FeedHandler;
import de.hdodenhof.feedreader.helpers.SAXHelper;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class AddFeedTask extends AsyncTask<String, Void, Void> {
        
        @SuppressWarnings("unused")
        private static final String TAG = AddFeedTask.class.getSimpleName();        
        
        private Handler mMainUIHandler;
        private Context mContext;
        private Feed mFeed;

        public AddFeedTask(Handler mainUIHandler, Context context) {
                this.mMainUIHandler = mainUIHandler;
                this.mContext = context;
                this.mFeed = new Feed();
        }

        protected Void doInBackground(String... params) {

                mFeed.setUrl(params[0]);
                RSSController mController = new RSSController(mContext);

                try {
                        SAXHelper mSAXHelper = new SAXHelper(mFeed.getUrl(), new FeedHandler());
                        String mName = (String) mSAXHelper.parse();

                        mFeed.setName(mName);
                        mFeed.setUpdated(new Date());
                        mController.addFeed(mFeed);

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
