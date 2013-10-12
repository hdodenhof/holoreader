package de.hdodenhof.holoreader.listadapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.provider.SQLiteHelper.FeedDAO;

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
        String name = cursor.getString(cursor.getColumnIndex(FeedDAO.NAME));
        String url = cursor.getString(cursor.getColumnIndex(FeedDAO.URL));

        final TextView titleView = (TextView) view.findViewById(R.id.list_item_editfeed_name);
        final TextView summaryView = (TextView) view.findViewById(R.id.list_item_editfeed_url);

        if (titleView != null) {
            titleView.setText(name);
        }
        if (summaryView != null) {
            summaryView.setText(url);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(mLayout, parent, false);
    }

    @Override
    public final boolean hasStableIds() {
        return true;
    }

}
