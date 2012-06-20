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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.adapters.RSSAdapter;
import de.hdodenhof.feedreader.controllers.RSSController;
import de.hdodenhof.feedreader.fragments.ArticlePagerFragment;
import de.hdodenhof.feedreader.listeners.ArticleOnPageChangeListener;
import de.hdodenhof.feedreader.listeners.OnFragmentReadyListener;
import de.hdodenhof.feedreader.misc.RSSMessage;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.models.Feed;
import de.hdodenhof.feedreader.runnables.SendMessageRunnable;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class DisplayFeedActivity extends FragmentActivity implements OnFragmentReadyListener, ArticleOnPageChangeListener, OnItemClickListener {

        @SuppressWarnings("unused")
        private static final String TAG = DisplayFeedActivity.class.getSimpleName();

        private ArrayList<Handler> mHandlers = new ArrayList<Handler>();
        private boolean mTwoPane = false;
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

                if (!getIntent().hasExtra("feedid")) {
                        Intent mIntent = new Intent(this, HomeActivity.class);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mIntent);
                } else {

                        setContentView(R.layout.activity_feed);

                        mController = new RSSController(this);

                        mFeed = mController.getFeed(getIntent().getIntExtra("feedid", 0));

                        if (findViewById(R.id.viewpager_article) != null) {
                                mTwoPane = true;
                        }

                        if (mTwoPane && !getIntent().hasExtra("articleid")) {
                                Intent mIntent = new Intent(this, HomeActivity.class);
                                mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(mIntent);
                        } else {
                                mArticle = mController.getArticle(getIntent().getIntExtra("articleid", -1));

                                if (mTwoPane) {
                                        new ArticlePagerFragment(this);
                                }                                
                                
                                ActionBar mActionBar = getActionBar();
                                mActionBar.setTitle(mFeed.getName());
                                mActionBar.setDisplayHomeAsUpEnabled(true);
                        }

                }
        }

        /**
         * @see de.hdodenhof.feedreader.listeners.OnFragmentReadyListener#onFragmentReady(android.os.Handler)
         */
        public void onFragmentReady(Handler handler) {
                mHandlers.add(handler);

                ArrayList<Feed> mFeeds = new ArrayList<Feed>();
                mFeeds.add(mFeed);

                RSSMessage mMessage;

                if (mTwoPane) {
                        mMessage = new RSSMessage();
                        mMessage.type = RSSMessage.CHOICE_MODE_SINGLE_ARTICLE;
                        new Thread(new SendMessageRunnable(mHandlers, mMessage, 0)).start();
                }

                mMessage = new RSSMessage();
                mMessage.feeds = mFeeds;
                mMessage.feed = mFeed;

                if (mArticle != null) {
                        mMessage.article = mArticle;
                }

                mMessage.type = RSSMessage.INITIALIZE;
                new Thread(new SendMessageRunnable(mHandlers, mMessage, 0)).start();
        }

        /**
         * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
         */
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                case android.R.id.home:
                        Intent mIntent = new Intent(this, HomeActivity.class);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mIntent);
                        return true;
                default:
                        return super.onOptionsItemSelected(item);
                }
        }

        /**
         * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
         */
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                MenuInflater mMenuInflater = getMenuInflater();
                mMenuInflater.inflate(R.menu.settings, menu);
                return true;
        }

        /**
         * Updates all fragments or launches a new activity (depending on the
         * activities current layout) whenever an article in the
         * ArticleListFragment has been clicked
         * 
         * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView,
         *      android.view.View, int, long)
         */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RSSAdapter adapter = (RSSAdapter) parent.getAdapter();

                switch (adapter.getType()) {
                case RSSAdapter.TYPE_ARTICLE:
                        Article mArticle = (Article) parent.getItemAtPosition(position);

                        if (!mTwoPane) {
                                Intent mIntent = new Intent(this, DisplayArticleActivity.class);
                                mIntent.putExtra("feedid", mFeed.getId());
                                mIntent.putExtra("articleid", mArticle.getId());
                                startActivity(mIntent);
                        } else {
                                ArrayList<Feed> mFeeds = new ArrayList<Feed>();
                                mFeeds.add(mFeed);

                                RSSMessage mMessage = new RSSMessage();
                                mMessage.article = mArticle;
                                mMessage.position = position;
                                mMessage.type = RSSMessage.POSITION_CHANGED;

                                new Thread(new SendMessageRunnable(mHandlers, mMessage, 0)).start();
                        }
                        break;
                default:
                        break;
                }

        }

        /**
         * @see de.hdodenhof.feedreader.listeners.ArticleOnPageChangeListener#onArticleChanged(de.hdodenhof.feedreader.models.Article,
         *      int)
         */
        public void onArticleChanged(Article article, int position) {
                RSSMessage mMessage = new RSSMessage();
                mMessage.article = article;
                mMessage.position = position;
                mMessage.type = RSSMessage.POSITION_CHANGED;

                new Thread(new MarkReadRunnable(article)).start();
                new Thread(new SendMessageRunnable(mHandlers, mMessage, 200)).start();
        }

        /**
         * 
         */
        private class MarkReadRunnable implements Runnable {
                private Article mArticle;

                public MarkReadRunnable(Article article) {
                        this.mArticle = article;
                }

                public void run() {
                        mArticle.setRead(true);
                        mController.updateArticle(mArticle);
                }
        }

}
