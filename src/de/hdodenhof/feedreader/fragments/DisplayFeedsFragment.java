package de.hdodenhof.feedreader.fragments;

import java.util.ArrayList;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.adapter.FeedAdapter;
import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.controller.FeedController;
import de.hdodenhof.feedreader.model.Feed;

public class DisplayFeedsFragment extends Fragment {
    
    private View feedsView;
    private ListView feedslistview;
    private ArrayAdapter<Feed> feedAdapter;
    private FeedController feedController;
    private ArticleController articleController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        feedsView = inflater.inflate(R.layout.fragment_feeds, null);
        feedslistview = (ListView) feedsView.findViewById(R.id.feeds_listview);

        return feedsView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = getActivity().getActionBar();
        bar.setDisplayHomeAsUpEnabled(false);

        setHasOptionsMenu(true);

        if (savedInstanceState != null) {

        }

        articleController = new ArticleController(getActivity());
        feedController = new FeedController(getActivity());
        feedAdapter = new FeedAdapter(getActivity(), feedController.getAllFeeds());

        feedslistview.setAdapter(feedAdapter);
        feedslistview.setOnItemClickListener((OnItemClickListener) getActivity());

        feedslistview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        feedslistview.setMultiChoiceModeListener(new FeedsMultiChoiceModeListener());

    }
    
    
    public int getListLength(){
        return feedAdapter.getCount();
    } 
    
    public void updateFeeds(){
        feedAdapter.clear();
        feedAdapter.addAll(feedController.getAllFeeds());
        feedAdapter.notifyDataSetChanged();        
    }

    private class FeedsMultiChoiceModeListener implements MultiChoiceModeListener {

        private ArrayList<Feed> toDelete;

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            Feed feed = (Feed) feedAdapter.getItem(position);

            if (checked) {
                toDelete.add(feed);
            } else {
                toDelete.remove(feed);
            }
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            case R.id.item_delete:

                for (Feed feed : toDelete) {
                    articleController.deleteArticles(feed.getId());
                    feedController.deleteFeed(feed);
                    feedAdapter.remove(feed);
                }

                feedAdapter.notifyDataSetChanged();
                mode.finish();
                return true;
            default:
                return false;
            }
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.feed_context, menu);

            toDelete = new ArrayList<Feed>();
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }

}
