package de.hdodenhof.feedreader.tasks;

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.helpers.SAXHelper;
import de.hdodenhof.feedreader.helpers.SQLiteHelper;
import de.hdodenhof.feedreader.helpers.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.helpers.SQLiteHelper.FeedDAO;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.providers.RSSContentProvider;
import de.hdodenhof.feedreader.saxhandlers.ArticleHandler;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class RefreshFeedsTask extends AsyncTask<Void, Integer, Void> {

        @SuppressWarnings("unused")
        private static final String TAG = RefreshFeedsTask.class.getSimpleName();

        private static final int SUMMARY_MAXLENGTH = 250;

        private Handler mMainUIHandler;
        private Context mContext;

        public RefreshFeedsTask(Handler mainUIHandler, Context context) {
                this.mMainUIHandler = mainUIHandler;
                this.mContext = context;
        }

        @SuppressWarnings("unchecked")
        protected Void doInBackground(Void... params) {

                ArrayList<Article> mArticles = new ArrayList<Article>();

                ContentResolver mContentResolver = mContext.getContentResolver();

                try {
                        String[] mProjection = { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL };
                        Cursor mCursor = mContentResolver.query(RSSContentProvider.URI_FEEDS, mProjection, null, null, null);
                        mCursor.moveToFirst();

                        do {
                                int mFeedID = mCursor.getInt(mCursor.getColumnIndex(FeedDAO._ID));
                                SAXHelper mSAXHelper = new SAXHelper(mCursor.getString(mCursor.getColumnIndex(FeedDAO.URL)), new ArticleHandler());
                                mArticles = (ArrayList<Article>) mSAXHelper.parse();
                                int i = 0;

                                ContentValues[] mContentValuesArray = new ContentValues[mArticles.size()];
                                for (Article mArticle : mArticles) {

                                        mArticle.setFeedId(mFeedID);

                                        Document mDocument = Jsoup.parse(mArticle.getContent());
                                        Elements mIframes = mDocument.getElementsByTag("iframe");

                                        TextNode mPlaceholder = new TextNode("(video removed)", null);
                                        for (Element mIframe : mIframes) {
                                                mIframe.replaceWith(mPlaceholder);
                                        }

                                        mArticle.setContent(mDocument.html());

                                        if (mArticle.getSummary() == null) {
                                                String mSummary = mDocument.text();
                                                if (mSummary.length() > SUMMARY_MAXLENGTH) {
                                                        mArticle.setSummary(mSummary.substring(0, SUMMARY_MAXLENGTH));
                                                } else {
                                                        mArticle.setSummary(mSummary);
                                                }
                                        }

                                        ContentValues mContentValues = new ContentValues();

                                        mContentValues.put(ArticleDAO.FEEDID, mArticle.getFeedId());
                                        mContentValues.put(ArticleDAO.GUID, mArticle.getGuid());
                                        mContentValues.put(ArticleDAO.PUBDATE, SQLiteHelper.fromDate(mArticle.getPubDate()));
                                        mContentValues.put(ArticleDAO.TITLE, mArticle.getTitle());
                                        mContentValues.put(ArticleDAO.SUMMARY, mArticle.getSummary());
                                        mContentValues.put(ArticleDAO.CONTENT, mArticle.getContent());
                                        mContentValues.put(ArticleDAO.READ, SQLiteHelper.fromBoolean(mArticle.isRead()));

                                        mContentValuesArray[i++] = mContentValues;

                                }

                                mContentResolver.delete(RSSContentProvider.URI_ARTICLES, ArticleDAO.FEEDID + "=?", new String[] { Integer.toString(mFeedID) });
                                mContentResolver.bulkInsert(RSSContentProvider.URI_ARTICLES, mContentValuesArray);

                                // TODO: update instead of insert if applicable, set lastUpdate on Feed

                                publishProgress(mCursor.getPosition()+1);
                        } while (mCursor.moveToNext());

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
