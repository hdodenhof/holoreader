package de.hdodenhof.playground;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import de.hdodenhof.playground.adapter.ArticleAdapter;
import de.hdodenhof.playground.dao.FeedsDataSource;
import de.hdodenhof.playground.handler.RSSHandler;
import de.hdodenhof.playground.model.Article;
import de.hdodenhof.playground.model.Feed;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class DisplayFeedActivity extends Activity {
    public String rssResult = "";
    private ProgressDialog spinner;
    private static final String PLAYGROUND = "Playground";

    private ArticleAdapter articleAdapter;
    private FeedsDataSource datasource;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.articles);

        datasource = new FeedsDataSource(this);
        try {
            datasource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }        
        
        long b = getIntent().getLongExtra("feedid", -1);
        final Feed feed = datasource.getFeed(b);
//        datasource.close();

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(feed.getName());
        actionBar.setDisplayHomeAsUpEnabled(true);          
        
        articleAdapter = new ArticleAdapter(this, new ArrayList<Article>());

        ListView articlelistview = (ListView) findViewById(R.id.article_listView);

        articlelistview.setAdapter(articleAdapter);
        articlelistview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Article article = (Article) parent.getItemAtPosition(position);
                Intent intent = new Intent(getApplicationContext(), DisplayArticleActivity.class);
                
                intent.putExtra("article", article);
                intent.putExtra("feedname", feed.getName());
                intent.putExtra("feedid", feed.getId());
                startActivity(intent);                
                
            }

        });

        spinner = ProgressDialog.show(this, "", "Please wait...", true);
        RssTask fetcharticletask = new RssTask();
        fetcharticletask.execute(feed.getUrl());

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
