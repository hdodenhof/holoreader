package de.hdodenhof.feedreader.listadapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.misc.Helpers;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class RSSFeedAdapter extends SimpleCursorAdapter implements RSSAdapter {

    @SuppressWarnings("unused")
    private static final String TAG = RSSFeedAdapter.class.getSimpleName();

    private int mLayout;

    public RSSFeedAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mLayout = layout;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        prepareView(view, context, cursor);
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        return super.swapCursor(c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(mLayout, parent, false);

        return Helpers.addBackgroundIndicator(context, view, R.attr.customActivatedBackgroundIndicator);
    }

    private View prepareView(View view, Context context, Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndex(FeedDAO.NAME));
        String updated = cursor.getString(cursor.getColumnIndex(FeedDAO.UPDATED));
        String unread = cursor.getString(cursor.getColumnIndex(FeedDAO.UNREAD));

        final TextView nameView = (TextView) view.findViewById(R.id.list_item_feed_name);
        final TextView updatedView = (TextView) view.findViewById(R.id.list_item_feed_updated);
        final TextView unreadView = (TextView) view.findViewById(R.id.list_item_feed_unread);

        if (nameView != null) {
            nameView.setText(name);
        }

        if (updatedView != null) {
            String parsedUpdated = "";
            if (updated != null) {
                parsedUpdated = Helpers.formatToYesterdayOrToday(SQLiteHelper.toDate(updated));
            }
            updatedView.setText(context.getResources().getString(R.string.LastUpdate) + ": " + parsedUpdated);
        }
        if (unreadView != null) {
            if (unread != null && !unread.equals("0")) {
                unreadView.setText(unread);
            } else {
                unreadView.setText("");
            }
        }

        return view;
    }

    public int getType() {
        return RSSAdapter.TYPE_FEED;
    }
}
