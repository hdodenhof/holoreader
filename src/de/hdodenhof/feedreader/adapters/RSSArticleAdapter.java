package de.hdodenhof.feedreader.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.models.Article;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class RSSArticleAdapter extends ArrayAdapter<Article> implements RSSAdapter {

        @SuppressWarnings("unused")
        private static final String TAG = RSSArticleAdapter.class.getSimpleName();

        private ArrayList<Article> mArticles;
        private LayoutInflater mLayoutInflater;

        public RSSArticleAdapter(Context context, ArrayList<Article> articles) {
                super(context, 0, articles);
                this.mArticles = articles;
                mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

                final Article mArticle = mArticles.get(position);

                if (convertView == null) {
                        convertView = mLayoutInflater.inflate(R.layout.listitem_article, null);
                }

                if (mArticle != null) {

                        final TextView mTitle = (TextView) convertView.findViewById(R.id.list_item_entry_title);
                        final TextView mSummary = (TextView) convertView.findViewById(R.id.list_item_entry_summary);
                        final TextView mReadState = (TextView) convertView.findViewById(R.id.list_item_entry_read);

                        if (mTitle != null) {
                                mTitle.setText(mArticle.getTitle());
                        }
                        if (mSummary != null) {
                                mSummary.setText(mArticle.getSummary());
                        }
                        if (mReadState != null) {
                                mReadState.setText(readState(mArticle.isRead()));
                        }
                }
                return convertView;
        }

        private String readState(boolean read) {
                if (read == true) {
                        return "read";
                } else {
                        return "unread";
                }
        }

        public int getType() {
                return RSSAdapter.TYPE_ARTICLE;
        }

}
