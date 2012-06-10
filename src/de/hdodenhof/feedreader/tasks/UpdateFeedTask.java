package de.hdodenhof.feedreader.tasks;

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.handler.ArticleHandler;
import de.hdodenhof.feedreader.helper.SAXHelper;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;

public class UpdateFeedTask extends AsyncTask<Feed, Void, Void> {

        private Feed mFeed;

        Handler mMainUIHandler;
        Context mContext;

        public UpdateFeedTask(Handler mainUIHandler, Context context) {
                this.mMainUIHandler = mainUIHandler;
                this.mContext = context;
        }

        @SuppressWarnings("unchecked")
        protected Void doInBackground(Feed... params) {

                mFeed = (Feed) params[0];
                ArrayList<Article> mArticles = new ArrayList<Article>();
                ArticleController mArticleController = new ArticleController(mContext);

                try {
                        SAXHelper mSAXHelper = new SAXHelper(mFeed.getUrl(), new ArticleHandler());
                        mArticles = (ArrayList<Article>) mSAXHelper.parse();

                        mArticleController.deleteArticles(mFeed.getId());
                        for (Article mArticle : mArticles) {
                                mArticleController.createArticle(mFeed.getId(), mArticle.getGuid(), mArticle.getPubDate(), mArticle.getTitle(),
                                                mArticle.getSummary(), mArticle.getContent());
                        }

                } catch (Exception e) {
                        e.printStackTrace();
                }

                return null;

        }

        @Override
        protected void onPreExecute() {
                super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void result) {
                Message mMSG = Message.obtain();
                mMSG.what = 2;
                mMainUIHandler.sendMessage(mMSG);

        }

}
