package de.hdodenhof.feedreader.listadapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class EditFeedAdapter extends SimpleCursorAdapter {

    @SuppressWarnings("unused")
    private static final String TAG = EditFeedAdapter.class.getSimpleName();

    private int mLayout;

    public EditFeedAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mLayout = layout;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String mName = cursor.getString(cursor.getColumnIndex(FeedDAO.NAME));
        String mURL = cursor.getString(cursor.getColumnIndex(FeedDAO.URL));

        final TextView mTitleView = (TextView) view.findViewById(R.id.list_item_editfeed_title);
        final TextView mSummaryView = (TextView) view.findViewById(R.id.list_item_editfeed_url);
        // final CheckBox mCheckbox = (CheckBox) view.findViewById(R.id.list_item_editfeed_checkbox);

        if (mTitleView != null) {
            mTitleView.setText(mName);
        }
        if (mSummaryView != null) {
            mSummaryView.setText(mURL);
        }
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        return super.swapCursor(c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final LayoutInflater inflater = LayoutInflater.from(context);
        View mView = inflater.inflate(mLayout, parent, false);

        // Setting this programmatic to be able to handle API level differences
        mView.setBackgroundResource(R.drawable.listview_background);

        return mView;
    }

    @Override
    public final boolean hasStableIds() {
        return true;
    }
}
