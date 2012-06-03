package de.hdodenhof.playground;

import de.hdodenhof.playground.model.Article;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

public class DisplayArticleActivity extends Activity {
    Bundle b;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.article);
        
        b = getIntent().getExtras();
        Article article = b.getParcelable("article");

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(b.getString("feedname"));
        actionBar.setDisplayHomeAsUpEnabled(true);           
        
        TextView header = (TextView) findViewById(R.id.article_header);
        header.setText(article.getTitle());        
        
        TextView text = (TextView) findViewById(R.id.article_text);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setText(article.getFormatedFull());
        
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
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
