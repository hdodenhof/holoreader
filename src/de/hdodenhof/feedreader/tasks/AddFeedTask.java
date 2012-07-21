package de.hdodenhof.feedreader.tasks;

import java.io.InputStream;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

public class AddFeedTask extends AsyncTask<String, Void, Integer> {

    private Handler mMainUIHandler;
    private Context mContext;

    public AddFeedTask(Handler mainUIHandler, Context context) {
        this.mMainUIHandler = mainUIHandler;
        this.mContext = context;
    }

    protected Integer doInBackground(String... params) {

        String mURL = params[0];
        String mName = "";

        try {
            InputStream mInputStream = new URL(mURL).openConnection().getInputStream();

            XmlPullParserFactory mParserFactory = XmlPullParserFactory.newInstance();
            mParserFactory.setNamespaceAware(true);
            XmlPullParser mPullParser = mParserFactory.newPullParser();
            mPullParser.setInput(mInputStream, null);

            int mEventType = mPullParser.getEventType();

            while (mEventType != XmlPullParser.END_DOCUMENT) {
                if (mEventType == XmlPullParser.START_TAG) {
                    if (mPullParser.getName().equalsIgnoreCase("title")) {
                        mName = mPullParser.nextText();
                        break;
                    }
                }
                mEventType = mPullParser.next();
            }
            mInputStream.close();

            ContentResolver mContentResolver = mContext.getContentResolver();
            ContentValues mContentValues = new ContentValues();

            mContentValues.put(FeedDAO.NAME, mName);
            mContentValues.put(FeedDAO.URL, mURL);

            Uri mNewFeed = mContentResolver.insert(RSSContentProvider.URI_FEEDS, mContentValues);
            return Integer.parseInt(mNewFeed.getLastPathSegment());

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
    protected void onPostExecute(Integer result) {
        Message mMSG = Message.obtain();
        mMSG.what = 1;
        mMSG.arg1 = result;
        mMainUIHandler.sendMessage(mMSG);
    }
}