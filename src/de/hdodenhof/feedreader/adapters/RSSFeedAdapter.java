package de.hdodenhof.feedreader.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class RSSFeedAdapter extends ArrayAdapter<Feed> implements RSSAdapter {

        @SuppressWarnings("unused")
        private static final String TAG = RSSFeedAdapter.class.getSimpleName();           
        
        private ArrayList<Feed> mFeeds;
        private LayoutInflater mLayoutInflater;

        public RSSFeedAdapter(Context context, ArrayList<Feed> items) {
                super(context, 0, items);
                this.mFeeds = items;
                mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

                final Feed mFeed = mFeeds.get(position);

                if (mFeed != null) {

                        convertView = mLayoutInflater.inflate(R.layout.listitem_feed, null);
                        final TextView mTitle = (TextView) convertView.findViewById(R.id.list_item_feed_title);
                        final TextView mSummary = (TextView) convertView.findViewById(R.id.list_item_feed_summary);
                        final TextView mUpdated = (TextView) convertView.findViewById(R.id.list_item_feed_updated);

                        if (mTitle != null) {
                                mTitle.setText(mFeed.getName());
                        }
                        if (mSummary != null) {
                                mSummary.setText(mFeed.getUrl());
                        }
                        if (mUpdated != null) {
                                mUpdated.setText(DateFormat.format("E, dd MMM yyyy - kk:mm", mFeed.getUpdated()));
                        }
                }
                return convertView;
        }
        
        public int getType(){
                return RSSAdapter.TYPE_FEED;
        }

}
