package de.hdodenhof.holoreader.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import android.util.Patterns;

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.provider.RSSContentProvider;
import de.hdodenhof.holoreader.provider.SQLiteHelper.FeedDAO;

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
    private int mReturnCondition = SUCCESS;

    public AddFeedTask(Handler mainUIHandler, Context context) {
        mMainUIHandler = mainUIHandler;
        mContext = context;
    }

    protected Integer doInBackground(URL... params) {
        URL url = params[0];
        String name;

        try {
            URLConnection connection = connect(url);

            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int httpResponse = httpConnection.getResponseCode();

            if (httpResponse == HttpURLConnection.HTTP_MOVED_PERM) {
                String redir = connection.getHeaderField("Location");
                url = new URL(redir);
                connection = connect(url);
            }

            String contentType = connection.getContentType();
            if (contentType != null && contentType.contains("xml")) {
                InputStream inputStream = connection.getInputStream();
                name = validateFeedAndExtractName(inputStream);
                inputStream.close();

                if (mReturnCondition == SUCCESS && name != null) {
                    return storeFeed(url.toString(), name);
                }
            } else {
                String alternateUrl = discoverFeed(url);
                if (alternateUrl == null) {
                    mReturnCondition = ERROR_NOFEED;
                } else {
                    URLConnection secondConnection = new URL(alternateUrl).openConnection();
                    secondConnection.setRequestProperty("User-agent", mContext.getResources().getString(R.string.AppName) + "/"
                            + mContext.getResources().getString(R.string.AppVersionName));
                    secondConnection.setConnectTimeout(2000);
                    secondConnection.setReadTimeout(2000);
                    secondConnection.connect();

                    InputStream inputStream = secondConnection.getInputStream();
                    name = validateFeedAndExtractName(inputStream);
                    inputStream.close();

                    if (mReturnCondition == SUCCESS && name != null) {
                        return storeFeed(alternateUrl.toString(), name);
                    }
                }
            }

        } catch (IOException e) {
            mReturnCondition = ERROR_IOEXCEPTION;
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
        if (mReturnCondition == SUCCESS) {
            msg.what = 1;
            msg.arg1 = result;
        } else {
            msg.what = 8;
            msg.arg1 = mReturnCondition;
        }
        mMainUIHandler.sendMessage(msg);
    }

    private URLConnection connect(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-agent",
                mContext.getResources().getString(R.string.AppName) + "/" + mContext.getResources().getString(R.string.AppVersionName));
        connection.connect();

        return connection;
    }

    private int storeFeed(String url, String name) {
        ContentResolver contentResolver = mContext.getContentResolver();
        ContentValues contentValues = new ContentValues();

        contentValues.put(FeedDAO.NAME, name);
        contentValues.put(FeedDAO.URL, url.toString());

        Uri newFeed = contentResolver.insert(RSSContentProvider.URI_FEEDS, contentValues);
        return Integer.parseInt(newFeed.getLastPathSegment());
    }

    private String validateFeedAndExtractName(InputStream inputStream) {
        String name = null;
        boolean isFeed = false;
        boolean isArticle = false;
        boolean hasContent = false;
        boolean hasSummary = false;
        boolean foundName = false;

        try {
            XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);
            XmlPullParser pullParser = parserFactory.newPullParser();
            pullParser.setInput(inputStream, null);

            int eventType = pullParser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String currentTag = pullParser.getName();
                    String currentPrefix = pullParser.getPrefix();

                    if (currentPrefix == null) {
                        currentPrefix = "";
                    }

                    if (currentTag.equalsIgnoreCase("rss") || currentTag.equalsIgnoreCase("feed") || currentTag.equalsIgnoreCase("rdf")) {
                        isFeed = true;
                    } else if (currentTag.equalsIgnoreCase("title") && isFeed && foundName == false) {
                        name = pullParser.nextText();
                        foundName = true;
                    } else if ((currentTag.equalsIgnoreCase("item") || currentTag.equalsIgnoreCase("entry")) && isFeed) {
                        isArticle = true;
                    } else if (((currentTag.equalsIgnoreCase("encoded") && currentPrefix.equalsIgnoreCase("content")) || (currentTag
                            .equalsIgnoreCase("content") && currentPrefix.equalsIgnoreCase(""))) && isArticle == true) {
                        hasContent = true;
                    } else if ((currentTag.equalsIgnoreCase("summary") || currentTag.equalsIgnoreCase("description")) && isArticle == true
                            && currentPrefix.equalsIgnoreCase("")) {
                        hasSummary = true;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    String currentTag = pullParser.getName();

                    if ((currentTag.equalsIgnoreCase("item") || currentTag.equalsIgnoreCase("entry")) && isFeed) {
                        break;
                    }
                }
                eventType = pullParser.next();
            }

            if (isFeed && (hasContent || hasSummary)) {
                return name;
            } else if (isFeed) {
                mReturnCondition = ERROR_NOCONTENT;
            } else {
                mReturnCondition = ERROR_NOFEED;
            }

        } catch (XmlPullParserException e) {
            mReturnCondition = ERROR_XMLPULLPARSEREXCEPTION;
        } catch (IOException e) {
            mReturnCondition = ERROR_IOEXCEPTION;
        }
        return null;
    }

    private String discoverFeed(URL url) {
        try {
            Document document = Jsoup.connect(url.toString())
                    .userAgent(mContext.getResources().getString(R.string.AppName) + "/" + mContext.getResources().getString(R.string.AppVersionName))
                    .timeout(2000).get();
            String rssUrl = document.select("link[rel=alternate][type=application/rss+xml]").attr("href");
            if (rssUrl == null || rssUrl == "") {
                rssUrl = document.select("link[rel=alternate][type=application/atom+xml]").attr("href");
            }

            if (rssUrl == null || rssUrl == "") {
                return null;
            } else {
                if (rssUrl.length() < 7 || (!rssUrl.substring(0, 7).equalsIgnoreCase("http://") && !rssUrl.substring(0, 8).equalsIgnoreCase("https://"))) {
                    String protocol = url.getProtocol();
                    String host = url.getHost();
                    if (rssUrl.substring(0, 1).equalsIgnoreCase("/")) {
                        rssUrl = protocol + "://" + host + rssUrl;
                    } else {
                        rssUrl = protocol + "://" + host + "/" + rssUrl;
                    }
                }

                Pattern pattern = Patterns.WEB_URL;
                Matcher matcher = pattern.matcher(rssUrl);

                if (matcher.matches()) {
                    return rssUrl;
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            return null;
        }
    }
}