package de.hdodenhof.feedreader.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
import android.util.Log;

import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

public class AddFeedTask extends AsyncTask<String, Void, Integer> {

    @SuppressWarnings("unused")
    private static final String TAG = AddFeedTask.class.getSimpleName();

    private Handler mMainUIHandler;
    private Context mContext;

    public AddFeedTask(Handler mainUIHandler, Context context) {
        mMainUIHandler = mainUIHandler;
        mContext = context;
    }

    protected Integer doInBackground(String... params) {

        String mURL = params[0];
        String mName = "";
        boolean mIsFeed = false;

        try {
            InputStream mInputStream = new URL(mURL).openConnection().getInputStream();

            XmlPullParserFactory mParserFactory = XmlPullParserFactory.newInstance();
            mParserFactory.setNamespaceAware(true);
            XmlPullParser mPullParser = mParserFactory.newPullParser();
            mPullParser.setInput(mInputStream, null);

            int mEventType = mPullParser.getEventType();

            while (mEventType != XmlPullParser.END_DOCUMENT) {
                if (mEventType == XmlPullParser.START_TAG) {
                    if (mPullParser.getName().equalsIgnoreCase("rss") || mPullParser.getName().equalsIgnoreCase("feed")) {
                        mIsFeed = true;
                    }
                    if (mPullParser.getName().equalsIgnoreCase("title")) {
                        mName = mPullParser.nextText();
                        break;
                    }
                }
                mEventType = mPullParser.next();
            }
            mInputStream.close();

            if (mIsFeed) {
                ContentResolver mContentResolver = mContext.getContentResolver();
                ContentValues mContentValues = new ContentValues();

                mContentValues.put(FeedDAO.NAME, mName);
                mContentValues.put(FeedDAO.URL, mURL);

                Uri mNewFeed = mContentResolver.insert(RSSContentProvider.URI_FEEDS, mContentValues);
                return Integer.parseInt(mNewFeed.getLastPathSegment());
            } else {
                return null;
            }

        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed URL");
        } catch (IOException e) {
            Log.e(TAG, "IOException");
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
        if (result != null) {
            mMSG.what = 1;
            mMSG.arg1 = result;
        } else {
            mMSG.what = 9;
        }
        mMainUIHandler.sendMessage(mMSG);
    }
}