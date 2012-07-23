package de.hdodenhof.feedreader.activities;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import de.hdodenhof.feedreader.R;

public class EditFeedsActivity extends SherlockFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {

        }

        setContentView(R.layout.activity_editfeeds);

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setTitle("Edit Feeds");
        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent mIntent = new Intent(this, HomeActivity.class);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mIntent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
