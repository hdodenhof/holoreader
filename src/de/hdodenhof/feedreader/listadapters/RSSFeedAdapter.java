package de.hdodenhof.feedreader.listadapters;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

        final TextView mTitleView = (TextView) view.findViewById(R.id.list_item_feed_title);
        final TextView mSummaryView = (TextView) view.findViewById(R.id.list_item_feed_summary);
        final TextView mUpdatedView = (TextView) view.findViewById(R.id.list_item_feed_updated);
        final TextView mUnreadView = (TextView) view.findViewById(R.id.list_item_feed_unread);

        if (mTitleView != null) {
            mTitleView.setText(mName);
        }
        if (mSummaryView != null) {
            mSummaryView.setText(mURL);
        }
        if (mUpdatedView != null) {
            String mParsedUpdated = "";
            if (mUpdated != null) {
                mParsedUpdated = formatToYesterdayOrToday(SQLiteHelper.toDate(mUpdated));
            }
            mUpdatedView.setText("Last Update: " + mParsedUpdated);
        }
        if (mUnreadView != null) {
            if (!mUnread.equals("0")) {
                mUnreadView.setText(mUnread);
            } else {
                mUnreadView.setText("");
            }
        }

        return view;
    }

    public int getType() {
        return RSSAdapter.TYPE_FEED;
    }
    
    private String formatToYesterdayOrToday(Date date) {
        Calendar mToday = Calendar.getInstance();
        Calendar mYesterday = Calendar.getInstance();
        mYesterday.add(Calendar.DATE, -1);
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTime(date);
        
        SimpleDateFormat mTimeFormatter = new SimpleDateFormat("kk:mm");

        if (mCalendar.get(Calendar.YEAR) == mToday.get(Calendar.YEAR) && mCalendar.get(Calendar.DAY_OF_YEAR) == mToday.get(Calendar.DAY_OF_YEAR)) {
            return "Today, " + mTimeFormatter.format(date);
        } else if (mCalendar.get(Calendar.YEAR) == mYesterday.get(Calendar.YEAR) && mCalendar.get(Calendar.DAY_OF_YEAR) == mYesterday.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday, " + mTimeFormatter.format(date);
        } else {
            return DateFormat.format("MMM dd, kk:mm", date).toString();
        }
    }

}
