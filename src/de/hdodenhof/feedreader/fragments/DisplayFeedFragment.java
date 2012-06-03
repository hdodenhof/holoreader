package de.hdodenhof.feedreader.fragments;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import de.hdodenhof.feedreader.DisplayArticleActivity;
import de.hdodenhof.feedreader.adapter.ArticleAdapter;
import de.hdodenhof.feedreader.dao.FeedsDataSource;
import de.hdodenhof.feedreader.handler.RSSHandler;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;
import de.hdodenhof.feedreader.R;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class DisplayFeedFragment extends Fragment {
    public String rssResult = "";
    private ProgressDialog spinner;

    private ArticleAdapter articleAdapter;
    private FeedsDataSource datasource;
    private ListView articlelistview;
    private Feed feed;

    boolean mDualPane;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.feed_fragment, container, false);

        datasource = new FeedsDataSource(getActivity());
        try {
            datasource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        long b = getActivity().getIntent().getLongExtra("feedid", -1);
        feed = datasource.getFeed(b);

        articleAdapter = new ArticleAdapter(getActivity(), new ArrayList<Article>());

        articlelistview = (ListView) contentView.findViewById(R.id.article_listview);

        articlelistview.setAdapter(articleAdapter);
        articlelistview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                
                Article article = (Article) parent.getItemAtPosition(position);
                
                if (mDualPane) {
                    
                    DisplayArticleFragment articleFragment = (DisplayArticleFragment) getFragmentManager().findFragmentById(R.id.article_fragment);
                    if (articleFragment == null || articleFragment.getShownIndex() != article) {
                        articleFragment = DisplayArticleFragment.newInstance(article);

                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.replace(R.id.article_fragment, articleFragment);
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                        ft.commit();
                    }

                } else {
                    Intent intent = new Intent(getActivity().getApplicationContext(), DisplayArticleActivity.class);

                    intent.putExtra("article", article);
                    intent.putExtra("feedname", feed.getName());
                    intent.putExtra("feedid", feed.getId());
                    startActivity(intent);
                }

            }

        });

        spinner = ProgressDialog.show(getActivity(), "", "Please wait...", true);
        RssTask fetcharticletask = new RssTask();
        fetcharticletask.execute(feed.getUrl());

        return contentView;

    }
    
    @Override
    public void onActivityCreated(Bundle savedState){
        super.onActivityCreated(savedState);
        
        View articleFrame = getActivity().findViewById(R.id.article_fragment);
        mDualPane = articleFrame != null && articleFrame.getVisibility() == View.VISIBLE;

        if (mDualPane) {
            articlelistview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
        
//        ActionBar actionBar = getActivity().getActionBar();
//        actionBar.setTitle(feed.getName());
//        actionBar.setDisplayHomeAsUpEnabled(true);         
    }

    private class RssTask extends AsyncTask<String, Void, ArrayList<Article>> {

        protected ArrayList<Article> doInBackground(String... params) {

            ArrayList<Article> al = new ArrayList<Article>();

            try {

                URL rssUrl = new URL(params[0]);
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                XMLReader xmlReader = saxParser.getXMLReader();
                RSSHandler rssHandler = new RSSHandler(al);
                xmlReader.setContentHandler(rssHandler);
                InputSource inputSource = new InputSource(rssUrl.openStream());
                xmlReader.parse(inputSource);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return al;

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected void onPostExecute(ArrayList<Article> al) {
            articleAdapter.clear();

            for (Article article : al) {

                articleAdapter.add(article);
            }
            spinner.dismiss();

        }
    }

}
