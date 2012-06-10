package de.hdodenhof.feedreader.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.adapter.FeedAdapter;
import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.model.Feed;

public class DisplayFeedsFragment extends ListFragment {

        private ListView mFeedsListView;
        private ArrayAdapter<Feed> mFeedAdapter;
        private FeedController mFeedController;
        private ArticleController mArticleController;
        private boolean mChoiceModeSingle = false;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
                super.onActivityCreated(savedInstanceState);

                if (savedInstanceState != null) {

                }

                mArticleController = new ArticleController(getActivity());
                mFeedController = new FeedController(getActivity());
                mFeedAdapter = new FeedAdapter(getActivity(), mFeedController.getAllFeeds());

                this.setEmptyText("No feeds");
                this.setListAdapter(mFeedAdapter);
                mFeedsListView = getListView();

                mFeedsListView.setOnItemClickListener((OnItemClickListener) getActivity());

                if (mChoiceModeSingle) {
                        mFeedsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                } else {
                        mFeedsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                        mFeedsListView.setMultiChoiceModeListener(new FeedsMultiChoiceModeListener());
                }

        }

        public void setChoiceModeSingle() {
                this.mChoiceModeSingle = true;
        }

        public int getListLength() {
                return mFeedAdapter.getCount();
        }

        public void updateFeeds() {
                mFeedAdapter.clear();
                mFeedAdapter.addAll(mFeedController.getAllFeeds());
                mFeedAdapter.notifyDataSetChanged();
        }

        private class FeedsMultiChoiceModeListener implements MultiChoiceModeListener {

                private ArrayList<Feed> mFeedsToDelete;

                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                        Feed mFeed = (Feed) mFeedAdapter.getItem(position);

                        if (checked) {
                                mFeedsToDelete.add(mFeed);
                        } else {
                                mFeedsToDelete.remove(mFeed);
                        }
                }

                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        switch (item.getItemId()) {
                        case R.id.item_delete:

                                for (Feed mFeed : mFeedsToDelete) {
                                        mArticleController.deleteArticles(mFeed.getId());
                                        mFeedController.deleteFeed(mFeed);
                                        mFeedAdapter.remove(mFeed);
                                }

                                mFeedAdapter.notifyDataSetChanged();
                                mode.finish();
                                return true;
                        default:
                                return false;
                        }
                }

                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        MenuInflater mMenuInflater = mode.getMenuInflater();
                        mMenuInflater.inflate(R.menu.feed_context, menu);

                        mFeedsToDelete = new ArrayList<Feed>();
                        return true;
                }

                public void onDestroyActionMode(ActionMode mode) {
                }

                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                }
        }

}
