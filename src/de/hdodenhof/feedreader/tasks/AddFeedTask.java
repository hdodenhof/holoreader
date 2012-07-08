package de.hdodenhof.feedreader.tasks;

import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.helpers.SAXHelper;
import de.hdodenhof.feedreader.helpers.SQLiteHelper;
import de.hdodenhof.feedreader.helpers.SQLiteHelper.FeedDAO;
import de.hdodenhof.feedreader.providers.RSSContentProvider;
import de.hdodenhof.feedreader.saxhandlers.FeedHandler;

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

        public AddFeedTask(Handler mainUIHandler, Context context) {
                this.mMainUIHandler = mainUIHandler;
                this.mContext = context;
        }

        protected Void doInBackground(String... params) {

                String mURL = params[0];

                try {
                        SAXHelper mSAXHelper = new SAXHelper(mURL, new FeedHandler());
                        String mName = (String) mSAXHelper.parse();
                        
                        ContentResolver mContentResolver = mContext.getContentResolver();
                        ContentValues mContentValues = new ContentValues();
                        
                        mContentValues.put(FeedDAO.NAME, mName);
                        mContentValues.put(FeedDAO.URL, mURL);
                        mContentValues.put(FeedDAO.UPDATED, SQLiteHelper.fromDate(new Date()));
                        mContentValues.put(FeedDAO.UNREAD, 0);
                        
                        mContentResolver.insert(RSSContentProvider.URI_FEEDS, mContentValues);

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
