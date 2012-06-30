package de.hdodenhof.feedreader.tasks;

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import de.hdodenhof.feedreader.controllers.RSSController;
import de.hdodenhof.feedreader.handlers.ArticleHandler;
import de.hdodenhof.feedreader.helpers.SAXHelper;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class UpdateFeedTask extends AsyncTask<Feed, Void, Void> {

        @SuppressWarnings("unused")
        private static final String TAG = UpdateFeedTask.class.getSimpleName();               
        
        private Handler mMainUIHandler;
        private Context mContext;
        private Feed mFeed;
        
        public UpdateFeedTask(Handler mainUIHandler, Context context) {
                this.mMainUIHandler = mainUIHandler;
                this.mContext = context;
        }

        @SuppressWarnings("unchecked")
        protected Void doInBackground(Feed... params) {

                mFeed = (Feed) params[0];
                ArrayList<Article> mArticles = new ArrayList<Article>();
                RSSController mController = new RSSController(mContext);

                try {
                        SAXHelper mSAXHelper = new SAXHelper(mFeed.getUrl(), new ArticleHandler());
                        mArticles = (ArrayList<Article>) mSAXHelper.parse();

                        mController.deleteArticles(mFeed.getId());
                        for (Article mArticle : mArticles) {
                                mArticle.setFeedId(mFeed.getId());
                                
                                Document doc = Jsoup.parse(mArticle.getContent());
                                Elements ifrms = doc.getElementsByTag("iframe");
                                ifrms.remove();

                                Elements imgs = doc.getElementsByTag("img");
                                for (Element img : imgs) {
                                        img.removeAttr("width");
                                        img.removeAttr("height");
                                }                                
                        }
                        mController.createOrUpdateArticles(mArticles);

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
