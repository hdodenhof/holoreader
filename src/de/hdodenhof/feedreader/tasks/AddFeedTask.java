package de.hdodenhof.feedreader.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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

        String url = params[0];
        String name = "";
        boolean isFeed = false;
        boolean isArticle = false;
        boolean hasContent = false;
        boolean foundName = false;

        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-agent", "Feedreader/0.8");
            InputStream inputStream = connection.getInputStream();

            XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);
            XmlPullParser pullParser = parserFactory.newPullParser();
            pullParser.setInput(inputStream, null);

            int eventType = pullParser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (pullParser.getName().equalsIgnoreCase("rss") || pullParser.getName().equalsIgnoreCase("feed")) {
                        isFeed = true;
                    }
                    if (pullParser.getName().equalsIgnoreCase("title") && isFeed && foundName == false) {
                        name = pullParser.nextText();
                        foundName = true;
                    }
                    if ((pullParser.getName().equalsIgnoreCase("item") || pullParser.getName().equalsIgnoreCase("entry")) && isFeed) {
                        isArticle = true;
                    } else if ((pullParser.getName().equalsIgnoreCase("encoded") || pullParser.getName().equalsIgnoreCase("content")) && isArticle == true) {
                        hasContent = true;
                        break;
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    if ((pullParser.getName().equalsIgnoreCase("item") || pullParser.getName().equalsIgnoreCase("entry")) && isFeed) {
                        break;
                    }
                }
                eventType = pullParser.next();
            }
            inputStream.close();

            if (isFeed && hasContent) {
                ContentResolver contentResolver = mContext.getContentResolver();
                ContentValues contentValues = new ContentValues();

                contentValues.put(FeedDAO.NAME, name);
                contentValues.put(FeedDAO.URL, url);

                Uri newFeed = contentResolver.insert(RSSContentProvider.URI_FEEDS, contentValues);
                return Integer.parseInt(newFeed.getLastPathSegment());
            } else if (isFeed) {
                return -1;
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
        Message msg = Message.obtain();
        if (result != null && result != -1) {
            msg.what = 1;
            msg.arg1 = result;
        } else if (result == -1) {
            msg.what = 8;
        } else {
            msg.what = 9;
        }
        mMainUIHandler.sendMessage(msg);
    }
}