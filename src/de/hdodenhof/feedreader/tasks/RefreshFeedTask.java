package de.hdodenhof.feedreader.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

public class RefreshFeedTask extends AsyncTask<Integer, Void, Integer> {

    @SuppressWarnings("unused")
    private static final String TAG = RefreshFeedTask.class.getSimpleName();

    public static final int SUCCESS = 0;
    public static final int ERROR_IOEXCEPTION = 1;
    public static final int ERROR_XMLPULLPARSEREXCEPTION = 2;

    private static final int SUMMARY_MAXLENGTH = 250;
    private static final String DATE_FORMATS[] = { "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss z", "yyyy-MM-dd'T'HH:mm:ssz",
            "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSZ" };
    private SimpleDateFormat mSimpleDateFormats[] = new SimpleDateFormat[DATE_FORMATS.length];

    private int mKeepReadArticlesDays;
    private int mKeepUnreadArticlesDays;

    private Handler mMainUIHandler;
    private Context mContext;
    private int returnCondition = SUCCESS;

    public RefreshFeedTask(Handler mainUIHandler, Context context) {
        mMainUIHandler = mainUIHandler;
        mContext = context;

        for (int i = 0; i < DATE_FORMATS.length; i++) {
            mSimpleDateFormats[i] = new SimpleDateFormat(DATE_FORMATS[i], Locale.US);
            mSimpleDateFormats[i].setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mKeepReadArticlesDays = Integer.parseInt(sharedPrefs.getString("pref_keep_read_articles_days", "3"));
        mKeepUnreadArticlesDays = Integer.parseInt(sharedPrefs.getString("pref_keep_unread_articles_days", "7"));
    }

    protected Integer doInBackground(Integer... params) {
        int mFeedID = params[0];

        ContentResolver contentResolver = mContext.getContentResolver();
        ArrayList<ContentValues> contentValuesArrayList = new ArrayList<ContentValues>();
        ArrayList<String> existingArticles = new ArrayList<String>();
        Date minimumDate;
        Date newestArticleDate = new Date(0);
        Date articleNotOlderThan = pastDate(mKeepUnreadArticlesDays);

        boolean isArticle = false;
        boolean linkForced = false;
        String title = null;
        String summary = null;
        String content = null;
        String guid = null;
        Date pubdate = null;
        Date updated = null;
        String link = null;

        try {

            String feedURL = queryURL(mFeedID);
            Log.v(TAG, "id_" + mFeedID + ": " + feedURL);

            // mark read articles after KEEP_READ_ARTICLES_DAYS as deleted
            ContentValues contentValues = new ContentValues();
            contentValues.put(ArticleDAO.ISDELETED, 1);
            int dbupdated = contentResolver.update(RSSContentProvider.URI_ARTICLES, contentValues, ArticleDAO.FEEDID + " = ? AND " + ArticleDAO.PUBDATE
                    + " < ? AND " + ArticleDAO.READ + " = ?", new String[] { String.valueOf(mFeedID), SQLiteHelper.fromDate(pastDate(mKeepReadArticlesDays)),
                    "1" });
            Log.v(TAG, "id_" + mFeedID + ": Marked " + dbupdated + " old articles as deleted");

            // delete all articles after MAX_NEW_ARTICLES_AGE_DAYS
            int deleted = contentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.FEEDID + " = ? AND " + ArticleDAO.PUBDATE + " < ?", new String[] {
                    String.valueOf(mFeedID), SQLiteHelper.fromDate(pastDate(mKeepUnreadArticlesDays)) });
            Log.v(TAG, "id_" + mFeedID + ": Deleted " + deleted + " old unread articles");

            existingArticles = queryArticles(contentResolver, mFeedID);
            newestArticleDate = queryNewestArticleDate(contentResolver, mFeedID);

            Log.v(TAG, "id_" + mFeedID + ": existing articles: " + existingArticles.size());
            Log.v(TAG, "id_" + mFeedID + ": newestArticleDate: " + newestArticleDate);
            Log.v(TAG, "id_" + mFeedID + ": articleNotOlderThan: " + articleNotOlderThan);
            if (newestArticleDate.equals(new Date(0))) {
                minimumDate = articleNotOlderThan;
            } else {
                minimumDate = articleNotOlderThan.before(newestArticleDate) ? newestArticleDate : articleNotOlderThan;
            }
            Log.v(TAG, "id_" + mFeedID + ": minimumDate: " + minimumDate);

            URLConnection connection = new URL(feedURL).openConnection();
            connection.setRequestProperty("User-agent",
                    mContext.getResources().getString(R.string.AppName) + "/" + mContext.getResources().getString(R.string.AppVersionName));
            InputStream inputStream = connection.getInputStream();

            XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);
            XmlPullParser pullParser = parserFactory.newPullParser();
            pullParser.setInput(inputStream, null);

            int eventType = pullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String currentTag = pullParser.getName();

                    if (currentTag.equalsIgnoreCase("item") || currentTag.equalsIgnoreCase("entry")) {
                        isArticle = true;
                    } else if (currentTag.equalsIgnoreCase("title") && isArticle == true) {
                        title = pullParser.nextText();
                    } else if (currentTag.equalsIgnoreCase("summary") && isArticle == true) {
                        summary = Html.fromHtml(pullParser.nextText()).toString();
                    } else if ((currentTag.equalsIgnoreCase("encoded") || currentTag.equalsIgnoreCase("content") || currentTag.equalsIgnoreCase("description"))
                            && isArticle == true) {
                        content = pullParser.nextText();
                    } else if ((currentTag.equalsIgnoreCase("guid") || currentTag.equalsIgnoreCase("id")) && isArticle == true) {
                        guid = pullParser.nextText();
                    } else if (currentTag.equalsIgnoreCase("pubdate") || currentTag.equalsIgnoreCase("published") || currentTag.equalsIgnoreCase("date")) {
                        pubdate = parsePubdate(pullParser.nextText());
                    } else if (currentTag.equalsIgnoreCase("updated")) {
                        updated = parsePubdate(pullParser.nextText());
                    } else if (currentTag.equalsIgnoreCase("link")) {
                        if (!linkForced) {
                            link = pullParser.nextText();
                        }
                    } else if (currentTag.equalsIgnoreCase("origLink")) {
                        link = pullParser.nextText();
                        linkForced = true;
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    String currentTag = pullParser.getName();

                    if (currentTag.equalsIgnoreCase("item") || currentTag.equalsIgnoreCase("entry")) {
                        isArticle = false;

                        Log.v(TAG, "id_" + mFeedID + ": working on " + guid);
                        if (pubdate == null) {
                            pubdate = updated;
                        }
                        if (guid == null) {
                            guid = link;
                        }

                        if (pubdate.before(minimumDate)) {
                            Log.v(TAG, "id_" + mFeedID + ": pubdate (" + pubdate + ") <  minimumDate (" + minimumDate + "), breaking");
                            break;
                        } else {
                            Log.v(TAG, "id_" + mFeedID + ": pubdate (" + pubdate + ") >=  minimumDate (" + minimumDate + ")");
                        }

                        if (!existingArticles.contains(guid)) {
                            Log.v(TAG, "id_" + mFeedID + ": adding " + guid);
                            contentValuesArrayList.add(prepareArticle(mFeedID, guid, link, pubdate, title, summary, content));

                            title = null;
                            summary = null;
                            content = null;
                            guid = null;
                            pubdate = null;
                            updated = null;
                            link = null;
                        }
                    }
                }
                eventType = pullParser.next();
            }

            inputStream.close();

            ContentValues[] contentValuesArray = new ContentValues[contentValuesArrayList.size()];
            contentValuesArray = contentValuesArrayList.toArray(contentValuesArray);
            contentResolver.bulkInsert(RSSContentProvider.URI_ARTICLES, contentValuesArray);

        } catch (IOException e) {
            returnCondition = ERROR_IOEXCEPTION;
        } catch (XmlPullParserException e) {
            returnCondition = ERROR_XMLPULLPARSEREXCEPTION;
        } catch (RuntimeException e) {
            Log.v(TAG, "id_" + mFeedID + ": EXCEPTION");
            e.printStackTrace();
        }
        return mFeedID;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Integer result) {
        Message msg = Message.obtain();
        msg.what = 2;
        msg.arg1 = result;
        msg.arg2 = returnCondition;
        mMainUIHandler.sendMessage(msg);
    }

    private ArrayList<String> queryArticles(ContentResolver contentResolver, int feedID) {
        ArrayList<String> articles = new ArrayList<String>();

        Cursor cursor = contentResolver.query(RSSContentProvider.URI_ARTICLES, new String[] { ArticleDAO._ID, ArticleDAO.GUID, ArticleDAO.PUBDATE },
                ArticleDAO.FEEDID + " = ?", new String[] { String.valueOf(feedID) }, ArticleDAO.PUBDATE + " DESC");

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                articles.add(cursor.getString(cursor.getColumnIndex(ArticleDAO.GUID)));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return articles;
    }

    private Date queryNewestArticleDate(ContentResolver contentResolver, int feedID) {
        Date maxDate = new Date(0);
        Cursor cursor = contentResolver.query(RSSContentProvider.URI_ARTICLES, new String[] { ArticleDAO._ID, ArticleDAO.PUBDATE }, ArticleDAO.FEEDID + " = ?",
                new String[] { String.valueOf(feedID) }, ArticleDAO.PUBDATE + " DESC");

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            maxDate = SQLiteHelper.toDate(cursor.getString(cursor.getColumnIndex(ArticleDAO.PUBDATE)));
        }
        cursor.close();

        return maxDate;
    }

    private Date pastDate(int interval) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -interval);
        return calendar.getTime();
    }

    private String queryURL(int feedID) {
        String feedURL = "";
        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = contentResolver.query(RSSContentProvider.URI_FEEDS, new String[] { FeedDAO._ID, FeedDAO.URL }, FeedDAO._ID + " = ?",
                new String[] { String.valueOf(feedID) }, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            feedURL = cursor.getString(cursor.getColumnIndex(FeedDAO.URL));
        }
        cursor.close();
        return feedURL;
    }

    private Date parsePubdate(String rawDate) throws XmlPullParserException {
        Date parsedDate = null;
        for (int j = 0; j < DATE_FORMATS.length; j++) {
            try {
                parsedDate = mSimpleDateFormats[j].parse(rawDate);
                break;
            } catch (ParseException mParserException) {
                if (j == DATE_FORMATS.length - 1) {
                    throw new XmlPullParserException(mParserException.getMessage());
                }
            }
        }
        return parsedDate;
    }

    private ContentValues prepareArticle(int feedID, String guid, String link, Date pubdate, String title, String summary, String content) {
        Document document = Jsoup.parse(content);

        Elements iframes = document.getElementsByTag("iframe");
        TextNode placeholder = new TextNode("(video removed)", null);
        for (Element mIframe : iframes) {
            mIframe.replaceWith(placeholder);
        }
        content = document.html();

        if (summary == null) {
            String contentSummary = document.text();
            if (contentSummary.length() > SUMMARY_MAXLENGTH) {
                summary = contentSummary.substring(0, SUMMARY_MAXLENGTH) + "[...]";
            } else {
                summary = contentSummary;
            }
        }

        // remove appended line breaks from summary
        while (summary.charAt(summary.length() - 1) == '\n' || summary.charAt(summary.length() - 1) == '\r') {
            summary = summary.substring(0, summary.length() - 1);
        }

        Element image = document.select("img").first();

        ContentValues contentValues = new ContentValues();

        contentValues.put(ArticleDAO.FEEDID, feedID);
        contentValues.put(ArticleDAO.GUID, guid);
        contentValues.put(ArticleDAO.LINK, link);
        contentValues.put(ArticleDAO.PUBDATE, SQLiteHelper.fromDate(pubdate));
        contentValues.put(ArticleDAO.TITLE, title);
        contentValues.put(ArticleDAO.SUMMARY, summary);
        contentValues.put(ArticleDAO.CONTENT, content);

        if (image != null) {
            contentValues.put(ArticleDAO.IMAGE, image.absUrl("src"));
        }

        contentValues.put(ArticleDAO.READ, 0);
        contentValues.put(ArticleDAO.ISDELETED, 0);

        return contentValues;
    }
}