package de.hdodenhof.feedreader.tasks;

import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

public class RefreshFeedsTask extends AsyncTask<Void, Integer, Void> {

    private static final int SUMMARY_MAXLENGTH = 250;

    private Handler mMainUIHandler;
    private Context mContext;

    public RefreshFeedsTask(Handler mainUIHandler, Context context) {
        this.mMainUIHandler = mainUIHandler;
        this.mContext = context;
    }

    protected Void doInBackground(Void... params) {

        final String DATE_FORMATS[] = { "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss z", "yyyy-MM-dd'T'HH:mm:ssz", "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSZ" };
        SimpleDateFormat mSimpleDateFormats[] = new SimpleDateFormat[DATE_FORMATS.length];

        for (int i = 0; i < DATE_FORMATS.length; i++) {
            mSimpleDateFormats[i] = new SimpleDateFormat(DATE_FORMATS[i], Locale.US);
            mSimpleDateFormats[i].setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        try {
            ContentResolver mContentResolver = mContext.getContentResolver();
            ArrayList<String[]> mFeeds = new ArrayList<String[]>();

            XmlPullParserFactory mParserFactory = XmlPullParserFactory.newInstance();
            mParserFactory.setNamespaceAware(true);

            Cursor mCursor = mContentResolver
                    .query(RSSContentProvider.URI_FEEDS, new String[] { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL }, null, null, null);

            if (mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                do {
                    mFeeds.add(new String[] { mCursor.getString(mCursor.getColumnIndex(FeedDAO._ID)),
                            mCursor.getString(mCursor.getColumnIndex(FeedDAO.URL)) });
                } while (mCursor.moveToNext());
            }
            mCursor.close();

            for (String[] mFeed : mFeeds) {
                boolean mIsArticle = false;
                ArrayList<ContentValues> mContentValuesArrayList = new ArrayList<ContentValues>();
                ArrayList<String> mExistingArticles = new ArrayList<String>();

                mCursor = mContentResolver.query(RSSContentProvider.URI_ARTICLES, new String[] { ArticleDAO._ID, ArticleDAO.GUID }, ArticleDAO.FEEDID
                        + "=?", new String[] { mFeed[0] }, null);

                if (mCursor.getCount() > 0) {
                    mCursor.moveToFirst();
                    do {
                        mExistingArticles.add(mCursor.getString(mCursor.getColumnIndex(ArticleDAO.GUID)));
                    } while (mCursor.moveToNext());
                }
                mCursor.close();

                InputStream mInputStream = new URL(mFeed[1]).openConnection().getInputStream();

                XmlPullParser mPullParser = mParserFactory.newPullParser();
                mPullParser.setInput(mInputStream, null);

                String mTitle = null;
                String mSummary = null;
                String mContent = null;
                String mGUID = null;
                String mPubdate = null;

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
                        } else if (mCurrentTag.equalsIgnoreCase("pubdate") || mCurrentTag.equalsIgnoreCase("published")
                                || mCurrentTag.equalsIgnoreCase("date")) {

                            String mPubdateRaw = mPullParser.nextText();

                            for (int j = 0; j < DATE_FORMATS.length; j++) {
                                try {
                                    mPubdate = SQLiteHelper.fromDate((mSimpleDateFormats[j].parse(mPubdateRaw)));
                                    break;
                                } catch (ParseException mParserException) {
                                    if (j == DATE_FORMATS.length - 1) {
                                        throw new XmlPullParserException(mParserException.getMessage());
                                    }
                                }
                            }
                        }

                    } else if (mEventType == XmlPullParser.END_TAG) {
                        String mCurrentTag = mPullParser.getName();

                        if (mCurrentTag.equalsIgnoreCase("item") || mCurrentTag.equalsIgnoreCase("entry")) {
                            mIsArticle = false;

                            if (!mExistingArticles.contains(mGUID)) {

                                Document mDocument = Jsoup.parse(mContent);

                                Elements mIframes = mDocument.getElementsByTag("iframe");
                                TextNode mPlaceholder = new TextNode("(video removed)", null);
                                for (Element mIframe : mIframes) {
                                    mIframe.replaceWith(mPlaceholder);
                                }
                                mContent = mDocument.html();

                                if (mSummary == null) {
                                    String mContentSummary = mDocument.text();
                                    if (mContentSummary.length() > SUMMARY_MAXLENGTH) {
                                        mSummary = mContentSummary.substring(0, SUMMARY_MAXLENGTH);
                                    } else {
                                        mSummary = mContentSummary;
                                    }
                                }

                                ContentValues mContentValues = new ContentValues();

                                mContentValues.put(ArticleDAO.FEEDID, mFeed[0]);
                                mContentValues.put(ArticleDAO.GUID, mGUID);
                                mContentValues.put(ArticleDAO.PUBDATE, mPubdate);
                                mContentValues.put(ArticleDAO.TITLE, mTitle);
                                mContentValues.put(ArticleDAO.SUMMARY, mSummary);
                                mContentValues.put(ArticleDAO.CONTENT, mContent);
                                mContentValues.put(ArticleDAO.READ, 0);

                                mContentValuesArrayList.add(mContentValues);

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

                publishProgress(mFeeds.indexOf(mFeed) + 1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        Message mMSG = Message.obtain();
        mMSG.what = 9;
        mMSG.arg1 = values[0];
        mMainUIHandler.sendMessage(mMSG);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void result) {
        Message mMSG = Message.obtain();
        mMSG.what = 3;
        mMainUIHandler.sendMessage(mMSG);
    }

}