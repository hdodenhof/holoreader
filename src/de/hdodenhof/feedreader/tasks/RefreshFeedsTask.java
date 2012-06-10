package de.hdodenhof.feedreader.tasks;

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.handler.ArticleHandler;
import de.hdodenhof.feedreader.helper.SAXHelper;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;

public class RefreshFeedsTask extends AsyncTask<Void, Integer, Void> {

        Handler mMainUIHandler;
        Context mContext;

        public RefreshFeedsTask(Handler mainUIHandler, Context context) {
                this.mMainUIHandler = mainUIHandler;
                this.mContext = context;
        }

        @SuppressWarnings("unchecked")
        protected Void doInBackground(Void... params) {

                ArrayList<Article> mAticles = new ArrayList<Article>();
                ArrayList<Feed> mFeeds = new ArrayList<Feed>();
                FeedController mFeedController = new FeedController(mContext);
                ArticleController mArticleController = new ArticleController(mContext);

                try {

                        int n = 0;
                        mFeeds = mFeedController.getAllFeeds();
                        for (Feed mFeed : mFeeds) {
                                n++;
                                SAXHelper mSAXHelper = new SAXHelper(mFeed.getUrl(), new ArticleHandler());
                                mAticles = (ArrayList<Article>) mSAXHelper.parse();
                                mArticleController.deleteArticles(mFeed.getId());
                                mArticleController.createArticles(mFeed.getId(), mAticles);
                                publishProgress(n);
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
