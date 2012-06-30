package de.hdodenhof.feedreader.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import de.hdodenhof.feedreader.adapters.RSSFeedAdapter;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.misc.RSSMessage;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class FeedListFragment extends ListFragment  {

        @SuppressWarnings("unused")
        private static final String TAG = FeedListFragment.class.getSimpleName();
        
        private ListView mFeedsListView;
        private ArrayAdapter<Feed> mFeedAdapter;

        Handler mMessageHandler = new Handler() {
                public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        
                        RSSMessage mMessage = (RSSMessage) msg.obj;
                        
                        switch (mMessage.type) {
                        case RSSMessage.INITIALIZE:
                        case RSSMessage.FEEDLIST_UPDATED:
                                mFeedAdapter.clear();
                                mFeedAdapter.addAll(mMessage.feeds);
                                mFeedAdapter.notifyDataSetChanged();                        
                                break;                        
                        case RSSMessage.CHOICE_MODE_SINGLE_FEED:
                                mFeedsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                                break;                        
                        default:
                                break;
                        }
                }
        };         
        
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
                
                ((FragmentCallback) getActivity()).onFragmentReady(mMessageHandler);

        }
}
