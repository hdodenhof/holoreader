package de.hdodenhof.feedreader.tasks;

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

import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

public class RefreshFeedTask extends AsyncTask<Integer, Void, Integer> {

    @SuppressWarnings("unused")
    private static final String TAG = AddFeedTask.class.getSimpleName();

    private static final int SUMMARY_MAXLENGTH = 250;
    private static final String DATE_FORMATS[] = { "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss z", "yyyy-MM-dd'T'HH:mm:ssz",
            "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSZ" };
    private SimpleDateFormat mSimpleDateFormats[] = new SimpleDateFormat[DATE_FORMATS.length];

    private int mKeepReadArticlesDays;
    private int mKeepUnreadArticlesDays;

    private Handler mMainUIHandler;
    private Context mContext;

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
        String title = null;
        String summary = null;
        String content = null;
        String guid = null;
        String pubdate = null;

        try {

            String feedURL = queryURL(mFeedID);
            Log.v(TAG, "id_" + mFeedID + ": " + feedURL);

            // delete read articles after KEEP_READ_ARTICLES_DAYS
            int deleted = contentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.FEEDID + " = ? AND " + ArticleDAO.PUBDATE + " < ? AND "
                    + ArticleDAO.READ + " = ?", new String[] { String.valueOf(mFeedID), SQLiteHelper.fromDate(pastDate(mKeepReadArticlesDays)), "1" });
            Log.v(TAG, "id_" + mFeedID + ": Deleted " + deleted + " read articles");

            // delete all articles after MAX_NEW_ARTICLES_AGE_DAYS
            deleted = contentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.FEEDID + " = ? AND " + ArticleDAO.PUBDATE + " < ?", new String[] {
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
            connection.setRequestProperty("User-agent", "Feedreader/0.8");
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
                    } else if ((currentTag.equalsIgnoreCase("description") || currentTag.equalsIgnoreCase("summary")) && isArticle == true) {
                        summary = Html.fromHtml(pullParser.nextText()).toString();
                    } else if ((currentTag.equalsIgnoreCase("encoded") || currentTag.equalsIgnoreCase("content")) && isArticle == true) {
                        content = pullParser.nextText();
                    } else if ((currentTag.equalsIgnoreCase("guid") || currentTag.equalsIgnoreCase("id")) && isArticle == true) {
                        guid = pullParser.nextText();
                    } else if (currentTag.equalsIgnoreCase("pubdate") || currentTag.equalsIgnoreCase("published") || currentTag.equalsIgnoreCase("date")) {
                        pubdate = parsePubdate(pullParser.nextText());
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    String currentTag = pullParser.getName();

                    if (currentTag.equalsIgnoreCase("item") || currentTag.equalsIgnoreCase("entry")) {
                        isArticle = false;

                        Log.v(TAG, "id_" + mFeedID + ": working on " + guid);

                        if (SQLiteHelper.toDate(pubdate).before(minimumDate)) {
                            Log.v(TAG, "id_" + mFeedID + ": pubdate (" + pubdate + ") <  minimumDate (" + minimumDate + "), breaking");
                            break;
                        } else {
                            Log.v(TAG, "id_" + mFeedID + ": pubdate (" + pubdate + ") >=  minimumDate (" + minimumDate + ")");
                        }

                        if (!existingArticles.contains(guid)) {
                            Log.v(TAG, "id_" + mFeedID + ": adding " + guid);
                            contentValuesArrayList.add(prepareArticle(mFeedID, guid, pubdate, title, summary, content));

                            title = null;
                            summary = null;
                            content = null;
                            guid = null;
                            pubdate = null;
                        }
                    }
                }
                eventType = pullParser.next();
            }
            inputStream.close();

            ContentValues[] contentValuesArray = new ContentValues[contentValuesArrayList.size()];
            contentValuesArray = contentValuesArrayList.toArray(contentValuesArray);
            contentResolver.bulkInsert(RSSContentProvider.URI_ARTICLES, contentValuesArray);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return mFeedID;
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

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Integer result) {
        Message msg = Message.obtain();
        msg.what = 2;
        msg.arg1 = result;
        mMainUIHandler.sendMessage(msg);
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

    private String parsePubdate(String rawDate) throws XmlPullParserException {
        String parsedDate = "";
        for (int j = 0; j < DATE_FORMATS.length; j++) {
            try {
                parsedDate = SQLiteHelper.fromDate((mSimpleDateFormats[j].parse(rawDate)));
                break;
            } catch (ParseException mParserException) {
                if (j == DATE_FORMATS.length - 1) {
                    throw new XmlPullParserException(mParserException.getMessage());
                }
            }
        }
        return parsedDate;
    }

    private ContentValues prepareArticle(int feedID, String guid, String pubdate, String title, String summary, String content) {
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
        String imageURL = image.absUrl("src");

        ContentValues contentValues = new ContentValues();

        contentValues.put(ArticleDAO.FEEDID, feedID);
        contentValues.put(ArticleDAO.GUID, guid);
        contentValues.put(ArticleDAO.PUBDATE, pubdate);
        contentValues.put(ArticleDAO.TITLE, title);
        contentValues.put(ArticleDAO.SUMMARY, summary);
        contentValues.put(ArticleDAO.CONTENT, content);
        contentValues.put(ArticleDAO.IMAGE, imageURL);
        contentValues.put(ArticleDAO.READ, 0);

        return contentValues;
    }
}