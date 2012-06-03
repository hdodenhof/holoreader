package de.hdodenhof.feedreader;

import de.hdodenhof.feedreader.fragments.DisplayArticleFragment;
import de.hdodenhof.feedreader.model.Article;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

public class DisplayArticleActivity extends FragmentActivity {
    Bundle b;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        b = getIntent().getExtras();
        Article article = b.getParcelable("article");        
        
        if (savedInstanceState == null) {
            DisplayArticleFragment articleFragment = new DisplayArticleFragment().newInstance(article);
            articleFragment.setArguments(b);
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, articleFragment).commit();
        }        
        
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, DisplayFeedActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("feedid", b.getLong("feedid"));
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
