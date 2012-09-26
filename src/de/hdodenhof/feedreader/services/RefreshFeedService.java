package de.hdodenhof.feedreader.services;

import java.io.IOException;
import java.io.InputStream;
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

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

public class RefreshFeedService extends IntentService {

    @SuppressWarnings("unused")
    private static final String TAG = RefreshFeedService.class.getSimpleName();

    public static final int SUCCESS = 0;
    public static final int ERROR_IOEXCEPTION = 1;
    public static final int ERROR_XMLPULLPARSEREXCEPTION = 2;

    private static final String NO_ACTION = "no_action";

    private static final int SUMMARY_MAXLENGTH = 250;
    private static final String DATE_FORMATS[] = { "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss z", "yyyy-MM-dd'T'HH:mm:ssz",
            "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSZ" };
    private SimpleDateFormat mSimpleDateFormats[] = new SimpleDateFormat[DATE_FORMATS.length];

    private int mKeepReadArticlesDays;
    private int mKeepUnreadArticlesDays;

    private int returnCondition = SUCCESS;

    private HashSet<Integer> mFeedsUpdating;

    public RefreshFeedService() {
        super("RefreshFeedService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        for (int i = 0; i < DATE_FORMATS.length; i++) {
            mSimpleDateFormats[i] = new SimpleDateFormat(DATE_FORMATS[i], Locale.US);
            mSimpleDateFormats[i].setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mKeepReadArticlesDays = Integer.parseInt(sharedPrefs.getString("pref_keep_read_articles_days", "3"));
        mKeepUnreadArticlesDays = Integer.parseInt(sharedPrefs.getString("pref_keep_unread_articles_days", "7"));

        mFeedsUpdating = new HashSet<Integer>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int feedID = intent.getIntExtra("feedid", -1);

        if (mFeedsUpdating.contains(feedID)) {
            intent.setAction(NO_ACTION);
        } else {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putBoolean("refreshing", true);
            editor.commit();

            mFeedsUpdating.add(feedID);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction() == NO_ACTION) {
            return;
        }
        int feedID = intent.getIntExtra("feedid", -1);

        try {

            ContentResolver contentResolver = getContentResolver();
            ArrayList<ContentValues> contentValuesArrayList = new ArrayList<ContentValues>();
            ArrayList<String> existingArticles = new ArrayList<String>();
            Date minimumDate;
            Date newestArticleDate = new Date(0);
            Date articleNotOlderThan = pastDate(mKeepUnreadArticlesDays);

            boolean isArticle = false;
            boolean linkOverride = false;
            String title = null;
            String summary = null;
            String content = null;
            String guid = null;
            Date pubdate = null;
            Date updated = null;
            String link = null;

            String feedURL = queryURL(feedID);
            Log.v(TAG, "id_" + feedID + ": " + feedURL);

            // mark read articles after KEEP_READ_ARTICLES_DAYS as deleted
            ContentValues contentValues = new ContentValues();
            contentValues.put(ArticleDAO.ISDELETED, 1);
            int dbupdated = contentResolver.update(RSSContentProvider.URI_ARTICLES, contentValues, ArticleDAO.FEEDID + " = ? AND " + ArticleDAO.PUBDATE
                    + " < ? AND " + ArticleDAO.READ + " IS NOT NULL",
                    new String[] { String.valueOf(feedID), SQLiteHelper.fromDate(pastDate(mKeepReadArticlesDays)) });
            Log.v(TAG, "id_" + feedID + ": Marked " + dbupdated + " old articles as deleted");

            // delete all articles after MAX_NEW_ARTICLES_AGE_DAYS
            int deleted = contentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.FEEDID + " = ? AND " + ArticleDAO.PUBDATE + " < ?", new String[] {
                    String.valueOf(feedID), SQLiteHelper.fromDate(pastDate(mKeepUnreadArticlesDays)) });
            Log.v(TAG, "id_" + feedID + ": Deleted " + deleted + " old unread articles");

            existingArticles = queryArticles(contentResolver, feedID);
            newestArticleDate = queryNewestArticleDate(contentResolver, feedID);

            Log.v(TAG, "id_" + feedID + ": existing articles: " + existingArticles.size());
            Log.v(TAG, "id_" + feedID + ": newestArticleDate: " + newestArticleDate);
            Log.v(TAG, "id_" + feedID + ": articleNotOlderThan: " + articleNotOlderThan);
            if (newestArticleDate.equals(new Date(0))) {
                minimumDate = articleNotOlderThan;
            } else {
                minimumDate = articleNotOlderThan.before(newestArticleDate) ? newestArticleDate : articleNotOlderThan;
            }
            Log.v(TAG, "id_" + feedID + ": minimumDate: " + minimumDate);

            URLConnection connection = new URL(feedURL).openConnection();
            connection.setRequestProperty("User-agent", getResources().getString(R.string.AppName) + "/" + getResources().getString(R.string.AppVersionName));
            InputStream inputStream = connection.getInputStream();

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

                    if (currentTag.equalsIgnoreCase("item") || currentTag.equalsIgnoreCase("entry")) {
                        isArticle = true;
                    } else if (currentTag.equalsIgnoreCase("title") && isArticle == true) {
                        title = safeNextText(pullParser);
                    } else if ((currentTag.equalsIgnoreCase("summary") || currentTag.equalsIgnoreCase("description")) && isArticle == true
                            && currentPrefix.equalsIgnoreCase("")) {
                        summary = safeNextText(pullParser);
                    } else if (((currentTag.equalsIgnoreCase("encoded") && currentPrefix.equalsIgnoreCase("content")) || (currentTag
                            .equalsIgnoreCase("content") && currentPrefix.equalsIgnoreCase(""))) && isArticle == true) {
                        content = extractContent(pullParser);
                    } else if ((currentTag.equalsIgnoreCase("guid") || currentTag.equalsIgnoreCase("id")) && isArticle == true) {
                        guid = safeNextText(pullParser);
                    } else if (currentTag.equalsIgnoreCase("pubdate") || currentTag.equalsIgnoreCase("published") || currentTag.equalsIgnoreCase("date")
                            && isArticle == true) {
                        pubdate = parsePubdate(safeNextText(pullParser));
                    } else if (currentTag.equalsIgnoreCase("updated") && isArticle == true) {
                        updated = parsePubdate(safeNextText(pullParser));
                    } else if (currentTag.equalsIgnoreCase("link") && isArticle == true) {
                        if (!linkOverride) {
                            link = extractLink(pullParser);
                        }
                    } else if (currentTag.equalsIgnoreCase("origLink") && isArticle == true) {
                        link = safeNextText(pullParser);
                        linkOverride = true;
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    String currentTag = pullParser.getName();

                    if (currentTag.equalsIgnoreCase("item") || currentTag.equalsIgnoreCase("entry")) {
                        isArticle = false;

                        Log.v(TAG, "id_" + feedID + ": working on " + guid);
                        if (pubdate == null) {
                            pubdate = updated;
                        }
                        if (guid == null) {
                            guid = link;
                        }

                        if (pubdate.before(minimumDate)) {
                            Log.v(TAG, "id_" + feedID + ": pubdate (" + pubdate + ") <  minimumDate (" + minimumDate + "), breaking");
                            break;
                        } else {
                            Log.v(TAG, "id_" + feedID + ": pubdate (" + pubdate + ") >=  minimumDate (" + minimumDate + ")");
                        }

                        if (!existingArticles.contains(guid)) {
                            ContentValues newArticle = prepareArticle(feedID, guid, link, pubdate, title, summary, content);
                            if (newArticle != null) {
                                Log.v(TAG, "id_" + feedID + ": adding " + guid);
                                contentValuesArrayList.add(newArticle);
                            } else {
                                Log.e(TAG, "id_" + feedID + ": " + guid + " cannot be added");
                            }
                        }

                        title = null;
                        summary = null;
                        content = null;
                        guid = null;
                        pubdate = null;
                        updated = null;
                        link = null;
                    }
                }
                eventType = pullParser.next();
            }

            inputStream.close();

            ContentValues[] contentValuesArray = new ContentValues[contentValuesArrayList.size()];
            contentValuesArray = contentValuesArrayList.toArray(contentValuesArray);
            contentResolver.bulkInsert(RSSContentProvider.URI_ARTICLES, contentValuesArray);

        } catch (IOException e) {
            Log.v(TAG, "id_" + feedID + ": IOEXCEPTION");
            returnCondition = ERROR_IOEXCEPTION;
        } catch (XmlPullParserException e) {
            Log.v(TAG, "id_" + feedID + ": XMLPULLPARSEREXCEPTION");
            returnCondition = ERROR_XMLPULLPARSEREXCEPTION;
        } catch (RuntimeException e) {
            Log.v(TAG, "id_" + feedID + ": RUNTIMEEXCEPTION");
            e.printStackTrace();
        } catch (Exception e) {
            Log.v(TAG, "id_" + feedID + ": EXCEPTION");
            e.printStackTrace();
        } finally {
            mFeedsUpdating.remove(feedID);
            if (mFeedsUpdating.size() == 0) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putBoolean("refreshing", false);
                editor.commit();

                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("de.hdodenhof.feedreader.FEEDS_REFRESHED");
                sendBroadcast(broadcastIntent);
            }
        }
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
