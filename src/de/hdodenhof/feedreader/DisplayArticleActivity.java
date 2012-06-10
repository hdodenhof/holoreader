package de.hdodenhof.feedreader;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;
import de.hdodenhof.feedreader.model.Feed;

public class DisplayArticleActivity extends ViewPagerFragmentActivity implements DisplayArticleFragment.ActivityConnector {

        private ArticleController mArticleController;

        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                super.setContentView(R.layout.activity_article);

                mArticleController = new ArticleController(this);

                mArticleID = getIntent().getLongExtra("articleid", -1);
                mFeedID = mArticleController.getArticle(mArticleID).getFeedId();

                initialisePaging();

                FeedController mFeedController = new FeedController(this);
                Feed mFeed = mFeedController.getFeed(mFeedID);

                ActionBar mActionBar = getActionBar();
                mActionBar.setTitle(mFeed.getName());
                mActionBar.setDisplayHomeAsUpEnabled(true);

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                case android.R.id.home:
                        Intent mIntent = new Intent(this, DisplayFeedActivity.class);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        mIntent.putExtra("feedid", mFeedID);
                        startActivity(mIntent);
                        return true;
                default:
                        return super.onOptionsItemSelected(item);
                }
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                MenuInflater mMenuInflater = getMenuInflater();
                mMenuInflater.inflate(R.menu.app, menu);
                return true;
        }
}
