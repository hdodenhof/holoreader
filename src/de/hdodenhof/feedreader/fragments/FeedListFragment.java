package de.hdodenhof.feedreader.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import de.hdodenhof.feedreader.adapters.RSSFeedAdapter;
import de.hdodenhof.feedreader.listeners.OnFragmentReadyListener;
import de.hdodenhof.feedreader.misc.RSSMessage;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class FeedListFragment extends ListFragment implements RSSFragment {

        @SuppressWarnings("unused")
        private static final String TAG = FeedListFragment.class.getSimpleName();
        
        private ListView mFeedsListView;
        private ArrayAdapter<Feed> mFeedAdapter;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
                super.onActivityCreated(savedInstanceState);

                if (savedInstanceState != null) {

                }

                mFeedAdapter = new RSSFeedAdapter(getActivity(), new ArrayList<Feed>());

                this.setEmptyText("No feeds");
                this.setListAdapter(mFeedAdapter);
                mFeedsListView = getListView();

                mFeedsListView.setOnItemClickListener((OnItemClickListener) getActivity());
                
                ((OnFragmentReadyListener) getActivity()).onFragmentReady(this);

        }
        
        public void handleMessage(RSSMessage message){
                switch (message.type) {
                case RSSMessage.INITIALIZE:
                        mFeedAdapter.clear();
                        mFeedAdapter.addAll(message.feeds);
                        mFeedAdapter.notifyDataSetChanged();                        
                        break;
                case RSSMessage.FEEDLIST_UPDATED:
                        mFeedAdapter.clear();
                        mFeedAdapter.addAll(message.feeds);
                        mFeedAdapter.notifyDataSetChanged();                        
                        break;                        
                case RSSMessage.CHOICE_MODE_SINGLE:
                        mFeedsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                        break;
                default:
                        break;
                }
        }
}
