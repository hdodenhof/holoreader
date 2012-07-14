package de.hdodenhof.feedreader.listadapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
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
        View mView = inflater.inflate(mLayout, parent, false);

        return mView;
    }

    private View prepareView(View view, Context context, Cursor cursor) {
        String mName = cursor.getString(cursor.getColumnIndex(FeedDAO.NAME));
        String mURL = cursor.getString(cursor.getColumnIndex(FeedDAO.URL));
        String mUpdated = cursor.getString(cursor.getColumnIndex(FeedDAO.UPDATED));
        String mUnread = cursor.getString(cursor.getColumnIndex(FeedDAO.UNREAD));

        final TextView mFeedTitle = (TextView) view.findViewById(R.id.list_item_feed_title);
        final TextView mFeedSummary = (TextView) view.findViewById(R.id.list_item_feed_summary);
        final TextView mFeedUpdated = (TextView) view.findViewById(R.id.list_item_feed_updated);
        final TextView mFeedUnread = (TextView) view.findViewById(R.id.list_item_feed_unread);

        if (mFeedTitle != null) {
            mFeedTitle.setText(mName);
        }
        if (mFeedSummary != null) {
            mFeedSummary.setText(mURL);
        }
        if (mFeedUpdated != null) {
            mFeedUpdated.setText("Refreshed: " + DateFormat.format("E, dd MMM yyyy - kk:mm", SQLiteHelper.toDate(mUpdated)));
        }
        if (mFeedUnread != null) {
            mFeedUnread.setText("Unread: " + mUnread);
        }

        return view;
    }

    public int getType() {
        return RSSAdapter.TYPE_FEED;
    }

}
