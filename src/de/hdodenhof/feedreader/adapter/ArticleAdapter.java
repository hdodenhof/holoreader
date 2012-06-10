package de.hdodenhof.feedreader.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.model.Article;

public class ArticleAdapter extends ArrayAdapter<Article> {

        private ArrayList<Article> mArticles;
        private LayoutInflater mLayoutInflater;

        public ArticleAdapter(Context context, ArrayList<Article> articles) {
                super(context, 0, articles);
                this.mArticles = articles;
                mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

                final Article mArticle = mArticles.get(position);

                if (mArticle != null) {

                        convertView = mLayoutInflater.inflate(R.layout.listitem_article, null);
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

        // temporary helper
        private String readState(boolean read) {
                if (read == true) {
                        return "read";
                } else {
                        return "unread";
                }
        }

}
