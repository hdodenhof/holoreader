package de.hdodenhof.feedreader.tasks;

import java.io.InputStream;
import java.net.URL;
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
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.Html;

import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

public class RefreshFeedTask extends AsyncTask<Integer, Void, Integer> {

    private static final int SUMMARY_MAXLENGTH = 250;
    private static final String DATE_FORMATS[] = { "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss z", "yyyy-MM-dd'T'HH:mm:ssz",
            "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSZ" };
    private SimpleDateFormat mSimpleDateFormats[] = new SimpleDateFormat[DATE_FORMATS.length];

    private static final int KEEP_READ_ARTICLES_DAYS = 3;
    private static final int MAX_NEW_ARTICLES_AGE_DAYS = 7;

    private Handler mMainUIHandler;
    private Context mContext;

    public RefreshFeedTask(Handler mainUIHandler, Context context) {
        this.mMainUIHandler = mainUIHandler;
        this.mContext = context;

        for (int i = 0; i < DATE_FORMATS.length; i++) {
            mSimpleDateFormats[i] = new SimpleDateFormat(DATE_FORMATS[i], Locale.US);
            mSimpleDateFormats[i].setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    }

    protected Integer doInBackground(Integer... params) {
        int mFeedID = params[0];

        try {
            ContentResolver mContentResolver = mContext.getContentResolver();
            ArrayList<ContentValues> mContentValuesArrayList = new ArrayList<ContentValues>();
            ArrayList<String> mExistingArticles = new ArrayList<String>();
            Date mNewestArticleDate = new Date(0);

            boolean mIsArticle = false;
            String mTitle = null;
            String mSummary = null;
            String mContent = null;
            String mGUID = null;
            String mPubdate = null;

            String mFeedURL = queryURL(mFeedID);

            // delete read articles after KEEP_READ_ARTICLES_DAYS
            mContentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.PUBDATE + " < ? AND " + ArticleDAO.READ + " = ?",
                    new String[] { SQLiteHelper.fromDate(pastDate(KEEP_READ_ARTICLES_DAYS)), "1" });

            // delete all articles after MAX_NEW_ARTICLES_AGE_DAYS
            mContentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.PUBDATE + " < ?",
                    new String[] { SQLiteHelper.fromDate(pastDate(MAX_NEW_ARTICLES_AGE_DAYS)) });

            Cursor mCursor = mContentResolver.query(RSSContentProvider.URI_ARTICLES, new String[] { ArticleDAO._ID, ArticleDAO.GUID, ArticleDAO.PUBDATE },
                    ArticleDAO.FEEDID + " = ?", new String[] { String.valueOf(mFeedID) }, ArticleDAO.PUBDATE + " DESC");
            if (mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                do {
                    mExistingArticles.add(mCursor.getString(mCursor.getColumnIndex(ArticleDAO.GUID)));
                    if (mCursor.isFirst()) {
                        mNewestArticleDate = SQLiteHelper.toDate(mCursor.getString(mCursor.getColumnIndex(ArticleDAO.PUBDATE)));
                    }
                } while (mCursor.moveToNext());
            }
            mCursor.close();

            Date mMinimumDate;
            Date mArticleNotOlderThan = pastDate(MAX_NEW_ARTICLES_AGE_DAYS);

            if (mNewestArticleDate.equals(new Date(0))) {
                mMinimumDate = mArticleNotOlderThan;
            } else {
                mMinimumDate = mArticleNotOlderThan.before(mNewestArticleDate) ? mArticleNotOlderThan : mNewestArticleDate;
            }

            InputStream mInputStream = new URL(mFeedURL).openConnection().getInputStream();
            XmlPullParserFactory mParserFactory = XmlPullParserFactory.newInstance();
            mParserFactory.setNamespaceAware(true);
            XmlPullParser mPullParser = mParserFactory.newPullParser();
            mPullParser.setInput(mInputStream, null);

            int mEventType = mPullParser.getEventType();
            while (mEventType != XmlPullParser.END_DOCUMENT) {
                if (mEventType == XmlPullParser.START_TAG) {
                    String mCurrentTag = mPullParser.getName();

                    if (mCurrentTag.equalsIgnoreCase("item") || mCurrentTag.equalsIgnoreCase("entry")) {
                        mIsArticle = true;
                    } else if (mCurrentTag.equalsIgnoreCase("title") && mIsArticle == true) {
                        mTitle = mPullParser.nextText();
                    } else if ((mCurrentTag.equalsIgnoreCase("description") || mCurrentTag.equalsIgnoreCase("summary")) && mIsArticle == true) {
                        mSummary = Html.fromHtml(mPullParser.nextText()).toString();
                    } else if ((mCurrentTag.equalsIgnoreCase("encoded") || mCurrentTag.equalsIgnoreCase("content")) && mIsArticle == true) {
                        mContent = mPullParser.nextText();
                    } else if ((mCurrentTag.equalsIgnoreCase("guid") || mCurrentTag.equalsIgnoreCase("id")) && mIsArticle == true) {
                        mGUID = mPullParser.nextText();
                    } else if (mCurrentTag.equalsIgnoreCase("pubdate") || mCurrentTag.equalsIgnoreCase("published") || mCurrentTag.equalsIgnoreCase("date")) {
                        mPubdate = parsePubdate(mPullParser.nextText());
                    }

                } else if (mEventType == XmlPullParser.END_TAG) {
                    String mCurrentTag = mPullParser.getName();

                    if (mCurrentTag.equalsIgnoreCase("item") || mCurrentTag.equalsIgnoreCase("entry")) {
                        mIsArticle = false;

                        if (SQLiteHelper.toDate(mPubdate).before(mMinimumDate)) {
                            break;
                        }

                        if (!mExistingArticles.contains(mGUID)) {
                            mContentValuesArrayList.add(prepareArticle(mFeedID, mGUID, mPubdate, mTitle, mSummary, mContent));

                            mTitle = null;
                            mSummary = null;
                            mContent = null;
                            mGUID = null;
                            mPubdate = null;
                        }
                    }
                }
                mEventType = mPullParser.next();
            }
            mInputStream.close();

            ContentValues[] mContentValuesArray = new ContentValues[mContentValuesArrayList.size()];
            mContentValuesArray = mContentValuesArrayList.toArray(mContentValuesArray);
            mContentResolver.bulkInsert(RSSContentProvider.URI_ARTICLES, mContentValuesArray);

        } catch (Exception e) {
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
        Message mMSG = Message.obtain();
        mMSG.what = 2;
        mMSG.arg1 = result;
        mMainUIHandler.sendMessage(mMSG);
    }

    private Date pastDate(int interval) {
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.add(Calendar.DATE, -interval);
        return mCalendar.getTime();
    }

    private String queryURL(int feedID) {
        String mFeedURL = "";
        ContentResolver mContentResolver = mContext.getContentResolver();
        Cursor mCursor = mContentResolver.query(RSSContentProvider.URI_FEEDS, new String[] { FeedDAO._ID, FeedDAO.URL }, FeedDAO._ID + " = ?",
                new String[] { String.valueOf(feedID) }, null);
        if (mCursor.getCount() > 0) {
            mCursor.moveToFirst();
            mFeedURL = mCursor.getString(mCursor.getColumnIndex(FeedDAO.URL));
        }
        mCursor.close();
        return mFeedURL;
    }

    private String parsePubdate(String rawDate) throws XmlPullParserException {
        String mParsedDate = "";
        for (int j = 0; j < DATE_FORMATS.length; j++) {
            try {
                mParsedDate = SQLiteHelper.fromDate((mSimpleDateFormats[j].parse(rawDate)));
                break;
            } catch (ParseException mParserException) {
                if (j == DATE_FORMATS.length - 1) {
                    throw new XmlPullParserException(mParserException.getMessage());
                }
            }
        }
        return mParsedDate;
    }

    private ContentValues prepareArticle(int feedID, String guid, String pubdate, String title, String summary, String content) {
        Document mDocument = Jsoup.parse(content);

        Elements mIframes = mDocument.getElementsByTag("iframe");
        TextNode mPlaceholder = new TextNode("(video removed)", null);
        for (Element mIframe : mIframes) {
            mIframe.replaceWith(mPlaceholder);
        }
        content = mDocument.html();

        if (summary == null) {
            String mContentSummary = mDocument.text();
            if (mContentSummary.length() > SUMMARY_MAXLENGTH) {
                summary = mContentSummary.substring(0, SUMMARY_MAXLENGTH) + "[...]";
            } else {
                summary = mContentSummary;
            }
        }

        // remove appended line breaks from summary
        while (summary.charAt(summary.length() - 1) == '\n' || summary.charAt(summary.length() - 1) == '\r') {
            summary = summary.substring(0, summary.length() - 1);
        }

        Element mImage = mDocument.select("img").first();
        String mImageURL = mImage.absUrl("src");

        ContentValues mContentValues = new ContentValues();

        mContentValues.put(ArticleDAO.FEEDID, feedID);
        mContentValues.put(ArticleDAO.GUID, guid);
        mContentValues.put(ArticleDAO.PUBDATE, pubdate);
        mContentValues.put(ArticleDAO.TITLE, title);
        mContentValues.put(ArticleDAO.SUMMARY, summary);
        mContentValues.put(ArticleDAO.CONTENT, content);
        mContentValues.put(ArticleDAO.IMAGE, mImageURL);
        mContentValues.put(ArticleDAO.READ, 0);

        return mContentValues;
    }
}