package de.hdodenhof.feedreader;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;
import de.hdodenhof.feedreader.fragments.DisplayArticlesFragment;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;

public class DisplayFeedActivity extends ViewPagerFragmentActivity implements DisplayArticlesFragment.ActivityConnector,
                DisplayArticleFragment.ActivityConnector {

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
                        mFeedID = getIntent().getLongExtra("feedid", -1);
                }

                if (getIntent().hasExtra("articleid")) {
                        mArticleID = getIntent().getLongExtra("articleid", -1);
                }

                setContentView(R.layout.activity_feed);

                View mArticleFragment = findViewById(R.id.viewpager);
                if (mArticleFragment != null) {
                        mTwoPane = true;
                }

                if (mTwoPane) {
                        initialisePaging();

                        DisplayArticlesFragment mArticlesFragment = (DisplayArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_feed);
                        mArticlesFragment.setChoiceModeSingle();

                        // FIXME
                        mFragmentsReady = true;
                }

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
                        Intent mIntent = new Intent(this, HomeActivity.class);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

        public int getArticlePosition() {
                if (mTwoPane) {
                        return (Integer) mArticleMap.get(this.mArticleID);
                } else {
                        return -1;
                }
        }

        public void articleSelected(int index, Article article) {

                if (mTwoPane) {
                        mPager.setCurrentItem((Integer) mArticleMap.get(article.getId()));

                } else {
                        Intent mIntent = new Intent(this, DisplayArticleActivity.class);
                        mIntent.putExtra("articleid", article.getId());
                        startActivity(mIntent);
                }

        }

}
