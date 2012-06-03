package de.hdodenhof.playground.adapter;

import java.util.ArrayList;

import de.hdodenhof.playground.R;

import de.hdodenhof.playground.model.Feed;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FeedAdapter extends ArrayAdapter<Feed> {

	private ArrayList<Feed> items;
	private LayoutInflater layoutInflater;

	public FeedAdapter(Context context, ArrayList<Feed> items) {
		super(context, 0, items);
		this.items = items;
		layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		final Feed item = items.get(position);
		
		if (item != null) {
				
				convertView = layoutInflater.inflate(R.layout.listitem_feed, null);
				final TextView title = (TextView) convertView.findViewById(R.id.list_item_feed_title);
				final TextView summary = (TextView) convertView.findViewById(R.id.list_item_feed_summary);

				if (title != null) {
					title.setText(item.getName());
				}
				if (summary != null) {
				    summary.setText(item.getUrl());
				}
		}
		return convertView;
	}

}
