package de.hdodenhof.feedreader.adapter;

import java.util.ArrayList;

import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ArticleAdapter extends ArrayAdapter<Article> {

	private ArrayList<Article> items;
	private LayoutInflater layoutInflater;

	public ArticleAdapter(Context context, ArrayList<Article> items) {
		super(context, 0, items);
		this.items = items;
		layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		final Article item = items.get(position);
		
		if (item != null) {
				
				convertView = layoutInflater.inflate(R.layout.listitem_article, null);
				final TextView title = (TextView) convertView.findViewById(R.id.list_item_entry_title);
				final TextView summary = (TextView) convertView.findViewById(R.id.list_item_entry_summary);

				if (title != null) {
					title.setText(item.getTitle());
				}
				if (summary != null) {
				    summary.setText(item.getSummary());
				}
		}
		return convertView;
	}

}
