package de.hdodenhof.feedreader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;

import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.fragments.DisplayArticlesFragment;
import de.hdodenhof.feedreader.fragments.DisplayFeedsFragment;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;
import de.hdodenhof.feedreader.tasks.AddFeedTask;
import de.hdodenhof.feedreader.tasks.RefreshFeedsTask;

public class HomeActivity extends FragmentActivity implements OnItemClickListener, DisplayArticlesFragment.ActivityConnector {

        private boolean mTwoPane = false;
        private ProgressDialog mSpinner;
        private ProgressDialog mProgresBar;
        private DisplayFeedsFragment mFeedsFragment;
        private DisplayArticlesFragment mArticlesFragment;
        private long mFeedID = -1;

        Handler mAsyncHandler = new Handler() {
                public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        switch (msg.what) {
                        case 1:
                                // added feed
                                mFeedsFragment.updateFeeds();
                                if (mTwoPane) {
                                        mArticlesFragment.updateContent(null);
                                }
                                mSpinner.dismiss();
                                break;
                        case 2:
                                // updated single feed
                                mSpinner.dismiss();
                                break;
                        case 3:
                                // refreshed feeds
                                if (mTwoPane) {
                                        mArticlesFragment.updateContent(null);
                                }
                                mProgresBar.dismiss();
                        case 9:
                                // refresh progress bar
                                mProgresBar.setProgress(msg.arg1);
                        default:
                                break;
                        }
                }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                if (savedInstanceState != null) {

                }

                setContentView(R.layout.activity_home);

                mFeedsFragment = (DisplayFeedsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_feeds);
                mArticlesFragment = (DisplayArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articles);
                if (mArticlesFragment != null) {
                        mTwoPane = true;
                }

                if (mTwoPane) {
                        mFeedsFragment.setChoiceModeSingle();
                }

        }

        public long getFeedId() {
                return this.mFeedID;
        }

        public int getArticlePosition() {
                return -1;
        }

        private void addFeed(String feedUrl) {
                mSpinner = ProgressDialog.show(this, "", "Please wait...", true);
                AddFeedTask mAddFeedTask = new AddFeedTask(mAsyncHandler, this);
                mAddFeedTask.execute(feedUrl);
        }

        private void refreshFeeds() {
                mProgresBar = new ProgressDialog(this);
                mProgresBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgresBar.setMessage("Loading...");
                mProgresBar.setCancelable(false);
                mProgresBar.setProgress(0);
                mProgresBar.setMax(mFeedsFragment.getListLength());
                mProgresBar.show();

                RefreshFeedsTask mRefreshFeedsTask = new RefreshFeedsTask(mAsyncHandler, this);
                mRefreshFeedsTask.execute();
        }

        private void showAddDialog() {
                AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);

                mAlertDialog.setTitle("Add feed");
                mAlertDialog.setMessage("Input Feed URL");

                final EditText mInput = new EditText(this);
                mInput.setText("http://t3n.de/news/feed");
                mAlertDialog.setView(mInput);

                mAlertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                                String value = mInput.getText().toString();
                                addFeed(value);
                        }
                });

                mAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                });

                mAlertDialog.show();
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Feed mFeed = (Feed) parent.getItemAtPosition(position);
                this.mFeedID = mFeed.getId();

                if (!mTwoPane) {
                        Intent mIntent = new Intent(this, DisplayFeedActivity.class);
                        mIntent.putExtra("feedid", mFeed.getId());
                        startActivity(mIntent);
                } else {
                        mArticlesFragment.updateContent(mFeed.getId());
                }

        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                MenuInflater mMenuInflater = getMenuInflater();
                mMenuInflater.inflate(R.menu.main, menu);
                return true;
        }

        public void articleSelected(int index, Article article) {
                ArticleController mArticleController = new ArticleController(this);
                long mFeedID = mArticleController.getArticle(article.getId()).getFeedId();

                Intent mIntent = new Intent(this, DisplayFeedActivity.class);
                mIntent.putExtra("articleid", article.getId());
                mIntent.putExtra("feedid", mFeedID);
                startActivity(mIntent);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                case R.id.item_refresh:
                        refreshFeeds();
                        return true;
                case R.id.item_add:
                        showAddDialog();
                        return true;
                default:
                        return super.onOptionsItemSelected(item);
                }
        }

}