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

public class HomeActivity extends FragmentActivity implements OnItemClickListener, DisplayArticlesFragment.OnArticleSelectedListener,
        DisplayArticlesFragment.ParameterProvider {

    private boolean mDualFragments = false;
    private ProgressDialog spinner;
    private ProgressDialog progressDialog;
    private DisplayFeedsFragment feedsFragment;
    private DisplayArticlesFragment articlesFragment;
    private long feedId = -1;

    Handler asyncHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case 1:
                // added feed
                feedsFragment.updateFeeds();
                if(mDualFragments){
                    articlesFragment.updateContent(null);
                }
                spinner.dismiss();
                break;
            case 2:
                // updated single feed
                spinner.dismiss();
                break;
            case 3:
                // refreshed feeds
                if(mDualFragments){
                    articlesFragment.updateContent(null);
                }                
                progressDialog.dismiss();
            case 9:
                // refresh progress bar
                progressDialog.setProgress(msg.arg1);
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

        feedsFragment = (DisplayFeedsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_feeds);
        articlesFragment = (DisplayArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articles);
        if (articlesFragment != null) {
            mDualFragments = true;
        }
        
        if (mDualFragments){
            feedsFragment.setChoiceModeSingle();
        }

    }

    public long getFeedId() {
        return this.feedId;
    }  

    public int getArticlePosition() {
        return -1;
    } 

    private void addFeed(String feedUrl) {
        spinner = ProgressDialog.show(this, "", "Please wait...", true);
        AddFeedTask addFeedTask = new AddFeedTask(asyncHandler, this);
        addFeedTask.execute(feedUrl);
    }

    private void refreshFeeds() {

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.setProgress(0);
        progressDialog.setMax(feedsFragment.getListLength());
        progressDialog.show();

        RefreshFeedsTask refreshFeedsTask = new RefreshFeedsTask(asyncHandler, this);
        refreshFeedsTask.execute();
    }

    private void showAddDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Add feed");
        alert.setMessage("Input Feed URL");

        final EditText input = new EditText(this);
        input.setText("http://t3n.de/news/feed");
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                addFeed(value);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Feed feed = (Feed) parent.getItemAtPosition(position);
        this.feedId = feed.getId();

        if (!mDualFragments) {
            Intent intent = new Intent(this, DisplayFeedActivity.class);
            intent.putExtra("feedid", feed.getId());
            startActivity(intent);
        } else {
            articlesFragment.updateContent(feed.getId());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    public void articleSelected(int index, Article article) {

        ArticleController articleController = new ArticleController(this);
        long feedId = articleController.getArticle(article.getId()).getFeedId();

        Intent intent = new Intent(this, DisplayFeedActivity.class);
        intent.putExtra("articleid", article.getId());
        intent.putExtra("feedid", feedId);
        startActivity(intent);
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