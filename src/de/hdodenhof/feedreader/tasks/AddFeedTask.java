package de.hdodenhof.feedreader.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

public class AddFeedTask extends AsyncTask<URL, Void, Integer> {

    @SuppressWarnings("unused")
    private static final String TAG = AddFeedTask.class.getSimpleName();

    public static final int SUCCESS = 0;
    public static final int ERROR_IOEXCEPTION = 1;
    public static final int ERROR_XMLPULLPARSEREXCEPTION = 2;
    public static final int ERROR_NOCONTENT = 3;
    public static final int ERROR_NOFEED = 4;

    private Handler mMainUIHandler;
    private Context mContext;
    private int returnCondition = SUCCESS;

    public AddFeedTask(Handler mainUIHandler, Context context) {
        mMainUIHandler = mainUIHandler;
        mContext = context;
    }

    protected Integer doInBackground(URL... params) {

        URL url = params[0];
        String name = "";
        boolean isFeed = false;
        boolean isArticle = false;
        boolean hasContent = false;
        boolean foundName = false;

        try {
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-agent",
                    mContext.getResources().getString(R.string.AppName) + "/" + mContext.getResources().getString(R.string.AppVersionName));
            String contentType = connection.getContentType();
            if (!contentType.contains("xml")) {
                returnCondition = ERROR_NOFEED;
                return null;
            }
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
                contentValues.put(FeedDAO.URL, url.toString());

                Uri newFeed = contentResolver.insert(RSSContentProvider.URI_FEEDS, contentValues);
                return Integer.parseInt(newFeed.getLastPathSegment());
            } else if (isFeed) {
                returnCondition = ERROR_NOCONTENT;
            } else {
                returnCondition = ERROR_NOFEED;
            }

        } catch (IOException e) {
            returnCondition = ERROR_IOEXCEPTION;
        } catch (XmlPullParserException e) {
            returnCondition = ERROR_XMLPULLPARSEREXCEPTION;
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
        if (returnCondition == SUCCESS) {
            msg.what = 1;
            msg.arg1 = result;
        } else {
            msg.what = 8;
            msg.arg1 = returnCondition;
        }
        mMainUIHandler.sendMessage(msg);
    }
}