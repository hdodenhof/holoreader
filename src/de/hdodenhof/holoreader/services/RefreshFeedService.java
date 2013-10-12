package de.hdodenhof.holoreader.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.misc.Prefs;
import de.hdodenhof.holoreader.provider.RSSContentProvider;
import de.hdodenhof.holoreader.provider.SQLiteHelper;
import de.hdodenhof.holoreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.holoreader.provider.SQLiteHelper.FeedDAO;

public class RefreshFeedService extends IntentService {

    @SuppressWarnings("unused")
    private static final String TAG = RefreshFeedService.class.getSimpleName();

    public static final long INTERVAL_MILLIS = 14400000; // 4h
    public static final long WAIT_MILLIS = 60000; // 1min

    public static final String BROADCAST_REFRESHED = "de.hdodenhof.holoreader.FEEDS_REFRESHED";
    public static final String BROADCAST_REFRESHING = "de.hdodenhof.holoreader.FEEDS_REFRESHING";

    public static final String EXTRA_FEEDID = "feedid";

    private static final String NO_ACTION = "no_action";
    private static final int KEEP_READ_ARTICLES_DAYS = 3;
    private static final int KEEP_UNREAD_ARTICLES_DAYS = 7;

    private static final int SUMMARY_MAXLENGTH = 250;
    private static final String DATE_FORMATS[] = { "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss z", "yyyy-MM-dd'T'HH:mm:ssz",
            "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSZ" };
    private SimpleDateFormat mSimpleDateFormats[] = new SimpleDateFormat[DATE_FORMATS.length];

    private ContentResolver mContentResolver;
    private SharedPreferences mSharedPrefs;
    private HashSet<Integer> mFeedsUpdating;

    private static PowerManager.WakeLock wakeLock = null;

    public RefreshFeedService() {
        super("RefreshFeedService");
    }

    private static PowerManager.WakeLock getLock(Context context) {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FeedRefreshLock");
        }
        return wakeLock;
    }

    public static void scheduleRefresh(Context context, long waitMillis){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lastRefresh = prefs.getLong(Prefs.LAST_REFRESHED, 0);

        if (lastRefresh == 0|| (System.currentTimeMillis() > lastRefresh
                && System.currentTimeMillis() - lastRefresh > (INTERVAL_MILLIS + AlarmManager.INTERVAL_FIFTEEN_MINUTES))) {
            Intent broadcastIntent = new Intent(context, RefreshFeedReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, broadcastIntent, 0);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                    + ((waitMillis == 0l) ? WAIT_MILLIS : waitMillis), INTERVAL_MILLIS, pendingIntent);
        }
    }

    public static void cancelPendingRefresh(Context context){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent broadcastIntent = new Intent(context, RefreshFeedReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, broadcastIntent, 0);

        alarmManager.cancel(pendingIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        for (int i = 0; i < DATE_FORMATS.length; i++) {
            mSimpleDateFormats[i] = new SimpleDateFormat(DATE_FORMATS[i], Locale.US);
            mSimpleDateFormats[i].setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        mContentResolver = getContentResolver();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFeedsUpdating = new HashSet<Integer>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager.WakeLock wakeLock = getLock(this);
        if (!wakeLock.isHeld() || (flags & START_FLAG_REDELIVERY) != 0) {
            wakeLock.acquire();
        }

        int feedID = intent.getIntExtra(EXTRA_FEEDID, -1);

        if (mFeedsUpdating.contains(feedID)) {
            intent.setAction(NO_ACTION);
        } else {
            if (mFeedsUpdating.size() == 0) {
                mSharedPrefs.edit().putBoolean(Prefs.REFRESHING, true).commit();

                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(BROADCAST_REFRESHING);
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
            }
            mFeedsUpdating.add(feedID);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction() == NO_ACTION) {
            return;
        }
        int feedID = intent.getIntExtra(EXTRA_FEEDID, -1);

        ArrayList<ContentValues> contentValuesArrayList = new ArrayList<ContentValues>();

        boolean isArticle = false;
        boolean linkOverride = false;
        String title = null;
        String summary = null;
        String content = null;
        String guid = null;
        Date pubdate = null;
        Date updated = null;
        String link = null;

        try {
            deleteOldArticles(feedID);

            ArrayList<String> existingArticles = queryArticles(feedID);
            Date minimumDate = getMinimumDate(feedID);

            InputStream inputStream = getURLInputStream(queryURL(feedID));
            XmlPullParser pullParser = getParser(inputStream);

            int eventType = pullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String currentTag = pullParser.getName();
                    String currentPrefix = pullParser.getPrefix() != null ? pullParser.getPrefix() : "";

                    if (isItemTag(currentTag, currentPrefix)) {
                        isArticle = true;
                    } else if (isTitleTag(currentTag, currentPrefix) && isArticle) {
                        title = safeNextText(pullParser);
                    } else if (isSummaryTag(currentTag, currentPrefix) && isArticle) {
                        summary = safeNextText(pullParser);
                    } else if (isContentTag(currentTag, currentPrefix) && isArticle) {
                        content = extractContent(pullParser);
                    } else if (isGuidTag(currentTag, currentPrefix) && isArticle) {
                        guid = safeNextText(pullParser);
                    } else if (isDateTag(currentTag, currentPrefix) && isArticle) {
                        pubdate = parsePubdate(safeNextText(pullParser));
                    } else if (isUpdatedTag(currentTag, currentPrefix) && isArticle) {
                        updated = parsePubdate(safeNextText(pullParser));
                    } else if (isLinkTag(currentTag, currentPrefix) && isArticle) {
                        if (!linkOverride) {
                            link = extractLink(pullParser);
                        }
                    } else if (isOrigLinkTag(currentTag, currentPrefix) && isArticle) {
                        link = safeNextText(pullParser);
                        linkOverride = true;
                    }

                } else if (eventType == XmlPullParser.END_TAG && isItemTag(pullParser.getName(), "")) {
                    pubdate = pubdate != null ? pubdate : updated;
                    guid = guid != null ? guid : link;

                    if (pubdate.before(minimumDate)) {
                        break;
                    }

                    if (!existingArticles.contains(guid)) {
                        ContentValues newArticle = prepareArticle(feedID, guid, link, pubdate, title, summary, content);
                        if (newArticle != null) {
                            contentValuesArrayList.add(newArticle);
                        }
                    }

                    isArticle = false;
                    title = null;
                    summary = null;
                    content = null;
                    guid = null;
                    pubdate = null;
                    updated = null;
                    link = null;

                }
                eventType = pullParser.next();
            }

            inputStream.close();
            storeArticles(contentValuesArrayList);

        } catch (IOException e) {
        } catch (XmlPullParserException e) {
        } catch (Exception e) {
        } finally {
            mFeedsUpdating.remove(feedID);
            if (mFeedsUpdating.size() == 0) {
                SharedPreferences.Editor editor = mSharedPrefs.edit();
                editor.putBoolean(Prefs.REFRESHING, false);
                editor.putLong(Prefs.LAST_REFRESHED, SystemClock.elapsedRealtime());
                editor.commit();

                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(BROADCAST_REFRESHED);
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

                PowerManager.WakeLock wakeLock = getLock(this);
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        }
    }

    private boolean isItemTag(String currentTag, String currentPrefix) {
        return currentTag.equalsIgnoreCase("item") || currentTag.equalsIgnoreCase("entry");
    }

    private boolean isTitleTag(String currentTag, String currentPrefix) {
        return currentTag.equalsIgnoreCase("title") && currentPrefix.equalsIgnoreCase("");
    }

    private boolean isSummaryTag(String currentTag, String currentPrefix) {
        return (currentTag.equalsIgnoreCase("summary") || currentTag.equalsIgnoreCase("description")) && currentPrefix.equalsIgnoreCase("");
    }

    private boolean isContentTag(String currentTag, String currentPrefix) {
        return (currentTag.equalsIgnoreCase("encoded") && currentPrefix.equalsIgnoreCase("content"))
                || (currentTag.equalsIgnoreCase("content") && currentPrefix.equalsIgnoreCase(""));
    }

    private boolean isGuidTag(String currentTag, String currentPrefix) {
        return currentTag.equalsIgnoreCase("guid") || currentTag.equalsIgnoreCase("id");
    }

    private boolean isDateTag(String currentTag, String currentPrefix) {
        return currentTag.equalsIgnoreCase("pubdate") || currentTag.equalsIgnoreCase("published") || currentTag.equalsIgnoreCase("date");
    }

    private boolean isUpdatedTag(String currentTag, String currentPrefix) {
        return currentTag.equalsIgnoreCase("updated");
    }

    private boolean isLinkTag(String currentTag, String currentPrefix) {
        return currentTag.equalsIgnoreCase("link");
    }

    private boolean isOrigLinkTag(String currentTag, String currentPrefix) {
        return currentTag.equalsIgnoreCase("origLink");
    }

    private XmlPullParser getParser(InputStream inputStream) throws XmlPullParserException {
        XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        XmlPullParser pullParser = parserFactory.newPullParser();
        pullParser.setInput(inputStream, null);
        return pullParser;
    }

    private InputStream getURLInputStream(String feedURL) throws IOException, MalformedURLException {
        URLConnection connection = new URL(feedURL).openConnection();
        connection.setRequestProperty("User-agent", getResources().getString(R.string.AppName) + "/" + getResources().getString(R.string.AppVersionName));
        connection.connect();
        return connection.getInputStream();
    }

    private Date getMinimumDate(int feedID) {
        Date newestArticleDate = queryNewestArticleDate(feedID);
        Date articleNotOlderThan = pastDate(KEEP_UNREAD_ARTICLES_DAYS);

        if (newestArticleDate == null) {
            return articleNotOlderThan;
        } else {
            return articleNotOlderThan.before(newestArticleDate) ? newestArticleDate : articleNotOlderThan;
        }
    }

    private void deleteOldArticles(int feedID) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ArticleDAO.ISDELETED, 1);
        mContentResolver.update(RSSContentProvider.URI_ARTICLES, contentValues, ArticleDAO.FEEDID + " = ? AND " + ArticleDAO.PUBDATE + " < ? AND "
                + ArticleDAO.READ + " IS NOT NULL", new String[] { String.valueOf(feedID), SQLiteHelper.fromDate(pastDate(KEEP_READ_ARTICLES_DAYS)) });

        mContentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.FEEDID + " = ? AND " + ArticleDAO.PUBDATE + " < ?",
                new String[] { String.valueOf(feedID), SQLiteHelper.fromDate(pastDate(KEEP_UNREAD_ARTICLES_DAYS)) });
    }

    private void storeArticles(ArrayList<ContentValues> contentValuesArrayList) {
        ContentValues[] contentValuesArray = new ContentValues[contentValuesArrayList.size()];
        contentValuesArray = contentValuesArrayList.toArray(contentValuesArray);
        mContentResolver.bulkInsert(RSSContentProvider.URI_ARTICLES, contentValuesArray);
    }

    private String extractLink(XmlPullParser pullParser) throws XmlPullParserException, IOException {
        String link = null;
        if (pullParser.getAttributeCount() > 0) {
            for (int i = 0; i < pullParser.getAttributeCount(); i++) {
                if (pullParser.getAttributeName(i).equals("href")) {
                    link = pullParser.getAttributeValue(i);
                    break;
                }
            }
        }
        if (link == null) {
            pullParser.next();
            link = pullParser.getText();
        }
        pullParser.next();
        return link;
    }

    private String extractContent(XmlPullParser pullParser) throws XmlPullParserException, IOException {
        String content = "";

        if (pullParser.getAttributeCount() > 0) {
            boolean isEncodedContent = false;
            for (int i = 0; i < pullParser.getAttributeCount(); i++) {
                if (pullParser.getAttributeName(i).equals("type")) {
                    isEncodedContent = (pullParser.getAttributeValue(i).equals("html") || pullParser.getAttributeValue(i).equals("xhtml"));
                    break;
                }
            }
            if (isEncodedContent) {
                content = parseEncodedContent(pullParser);
            }
        } else {
            content = safeNextText(pullParser);
        }

        return content;
    }

    private String parseEncodedContent(XmlPullParser pullParser) throws XmlPullParserException, IOException {
        StringBuilder sb = new StringBuilder();

        pullParser.next();
        int eventType = pullParser.getEventType();

        if (eventType == XmlPullParser.TEXT) {
            String txt = cleanText(pullParser.getText());
            if (txt.length() > 0) {
                pullParser.next();
                return txt;
            } else {
                eventType = pullParser.next();
            }
        }

        if (eventType == XmlPullParser.START_TAG) {
            while (!isContentEnd(pullParser)) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (pullParser.isEmptyElementTag()) {
                        String tag = pullParser.getName();
                        sb.append("<");
                        sb.append(tag);
                        if (tag.equals("img")) {
                            sb.append(getAttribute(pullParser, "src"));
                        }
                        sb.append("/>");
                        pullParser.next();
                    } else {
                        String tag = pullParser.getName();
                        sb.append("<");
                        sb.append(pullParser.getName());
                        if (tag.equals("a")) {
                            sb.append(getAttribute(pullParser, "href"));
                        }
                        sb.append(">");
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    sb.append(cleanText(pullParser.getText()));
                } else if (eventType == XmlPullParser.END_TAG) {
                    sb.append("</");
                    sb.append(pullParser.getName());
                    sb.append(">");
                }
                eventType = pullParser.next();
            }
        }
        return sb.toString();
    }

    private String cleanText(String text) {
        return text.replace("\n", "").replace("\t", "").replace("\r", "").trim();
    }

    private String getAttribute(XmlPullParser pullParser, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        sb.append(name);
        sb.append("=\"");
        for (int i = 0; i < pullParser.getAttributeCount(); i++) {
            if (pullParser.getAttributeName(i).equals(name)) {
                sb.append(pullParser.getAttributeValue(i));
                break;
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private boolean isContentEnd(XmlPullParser pullParser) throws XmlPullParserException {
        if (pullParser.getEventType() != XmlPullParser.END_TAG) {
            return false;
        }

        if (pullParser.getName().equals("content")) {
            return true;
        }

        return false;
    }

    /*
     * Work around a bug in early XMLPullParser versions, see http://android-developers.blogspot.de/2011/12/watch-out-for-xmlpullparsernexttext.html
     */
    private String safeNextText(XmlPullParser parser) throws XmlPullParserException, IOException {
        String result = parser.nextText();
        if (parser.getEventType() != XmlPullParser.END_TAG) {
            parser.nextTag();
        }
        return result;
    }

    private ArrayList<String> queryArticles(int feedID) {
        ArrayList<String> articles = new ArrayList<String>();

        Cursor cursor = mContentResolver.query(RSSContentProvider.URI_ARTICLES, new String[] { ArticleDAO._ID, ArticleDAO.GUID, ArticleDAO.PUBDATE },
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

    private Date queryNewestArticleDate(int feedID) {
        Date maxDate = null;
        Cursor cursor = mContentResolver.query(RSSContentProvider.URI_ARTICLES, new String[] { ArticleDAO._ID, ArticleDAO.PUBDATE },
                ArticleDAO.FEEDID + " = ?", new String[] { String.valueOf(feedID) }, ArticleDAO.PUBDATE + " DESC");

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
        ContentResolver contentResolver = getContentResolver();
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
                parsedDate = mSimpleDateFormats[j].parse(rawDate.replaceAll("([\\+\\-]\\d\\d):(\\d\\d)", "$1$2"));
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
        boolean missingContent = false;
        boolean missingSummary = false;

        if (content == null) {
            missingContent = true;
        }
        if (summary == null) {
            missingSummary = true;
        }

        if (missingContent && missingSummary) {
            return null;
        }

        if (missingContent) {
            content = summary;
        } else if (missingSummary) {
            summary = content;
        }

        Document parsedContent = Jsoup.parse(content);
        Elements iframes = parsedContent.getElementsByTag("iframe");
        TextNode placeholder = new TextNode("(video removed)", null);
        for (Element mIframe : iframes) {
            mIframe.replaceWith(placeholder);
        }
        content = parsedContent.html();

        Document parsedSummary = Jsoup.parse(summary);
        Elements pics = parsedSummary.getElementsByTag("img");
        for (Element pic : pics) {
            pic.remove();
        }
        summary = parsedSummary.text();

        if (summary.length() > SUMMARY_MAXLENGTH) {
            summary = summary.substring(0, SUMMARY_MAXLENGTH) + "...";
        }

        Element image = parsedContent.select("img").first();

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

        contentValues.put(ArticleDAO.ISDELETED, 0);

        return contentValues;
    }

}
