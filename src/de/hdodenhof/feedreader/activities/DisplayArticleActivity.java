package de.hdodenhof.feedreader.activities;

import java.util.ArrayList;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.controllers.RSSController;
import de.hdodenhof.feedreader.fragments.ArticlePagerFragment;
import de.hdodenhof.feedreader.listeners.ArticleOnPageChangeListener;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.RSSMessage;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.models.Feed;
import de.hdodenhof.feedreader.runnables.SendMessageRunnable;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class DisplayArticleActivity extends FragmentActivity implements FragmentCallback, ArticleOnPageChangeListener {

        @SuppressWarnings("unused")
        private static final String TAG = DisplayArticleActivity.class.getSimpleName();

        private ArrayList<Handler> mHandlers = new ArrayList<Handler>();
        private RSSController mController;
        private Feed mFeed;
        private Article mArticle;

        /**
         * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                if (savedInstanceState != null) {

                }

                if (!getIntent().hasExtra("feedid") || !getIntent().hasExtra("articleid")) {
                        Intent mIntent = new Intent(this, HomeActivity.class);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mIntent);
                } else {

                        setContentView(R.layout.activity_article);

                        mController = new RSSController(this);

                        mFeed = mController.getFeed(getIntent().getIntExtra("feedid", 0));
                        mArticle = mController.getArticle(getIntent().getIntExtra("articleid", -1));

                        new ArticlePagerFragment(this);
                        
                        ActionBar mActionBar = getActionBar();
                        mActionBar.setTitle(mFeed.getName());
                        mActionBar.setDisplayHomeAsUpEnabled(true);
                }

        }

        /**
         * @see de.hdodenhof.feedreader.misc.FragmentCallback#onFragmentReady(android.os.Handler)
         */
        public void onFragmentReady(Handler handler) {
                mHandlers.add(handler);

                ArrayList<Feed> mFeeds = new ArrayList<Feed>();
                mFeeds.add(mFeed);

                RSSMessage mMessage = new RSSMessage();
                mMessage.feeds = mFeeds;
                mMessage.feed = mFeed;
                mMessage.article = mArticle;
                mMessage.type = RSSMessage.INITIALIZE;

                new Thread(new SendMessageRunnable(mHandlers, mMessage, 0)).start();
        }
        
        /**
         * @see de.hdodenhof.feedreader.misc.FragmentCallback#isDualPane()
         */
        public boolean isDualPane() {
                return false;
        }    

        /*
         * @see
         * android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
         */
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                case android.R.id.home:
                        Intent mIntent = new Intent(this, DisplayFeedActivity.class);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        mIntent.putExtra("feedid", mFeed.getId());
                        startActivity(mIntent);
                        return true;
                default:
                        return super.onOptionsItemSelected(item);
                }
        }

        /*
         * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
         */
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                MenuInflater mMenuInflater = getMenuInflater();
                mMenuInflater.inflate(R.menu.settings, menu);
                return true;
        }

        /*
         * @see de.hdodenhof.feedreader.listeners.ArticleOnPageChangeListener#
         * onArticleChanged(de.hdodenhof.feedreader.models.Article, int)
         */
        public void onArticleChanged(Article article, int position) {
                article.setRead(true);
                mController.updateArticle(article);
        }

}
