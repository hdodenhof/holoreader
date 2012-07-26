package de.hdodenhof.feedreader.listadapters;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Build;
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
    private Context mContext;

    public RSSFeedAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mLayout = layout;
        mContext = context;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            TypedArray mAttributes = context.obtainStyledAttributes(new int[] { android.R.attr.activatedBackgroundIndicator });
            int mResource = mAttributes.getResourceId(0, 0);
            mAttributes.recycle();

            // setBackgroundResource resets padding
            int mLeftPadding = mView.getPaddingLeft();
            int mTopPadding = mView.getPaddingTop();
            int mRightPadding = mView.getPaddingRight();
            int mBottomPadding = mView.getPaddingBottom();
            mView.setBackgroundResource(mResource);
            mView.setPadding(mLeftPadding, mTopPadding, mRightPadding, mBottomPadding);
        }

        return mView;
    }

    private View prepareView(View view, Context context, Cursor cursor) {
        String mName = cursor.getString(cursor.getColumnIndex(FeedDAO.NAME));
        String mUpdated = cursor.getString(cursor.getColumnIndex(FeedDAO.UPDATED));
        String mUnread = cursor.getString(cursor.getColumnIndex(FeedDAO.UNREAD));

        final TextView mNameView = (TextView) view.findViewById(R.id.list_item_feed_name);
        final TextView mUpdatedView = (TextView) view.findViewById(R.id.list_item_feed_updated);
        final TextView mUnreadView = (TextView) view.findViewById(R.id.list_item_feed_unread);

        if (mNameView != null) {
            mNameView.setText(mName);
        }

        if (mUpdatedView != null) {
            String mParsedUpdated = "";
            if (mUpdated != null) {
                mParsedUpdated = formatToYesterdayOrToday(SQLiteHelper.toDate(mUpdated));
            }
            mUpdatedView.setText(context.getResources().getString(R.string.LastUpdate) + ": " + mParsedUpdated);
        }
        if (mUnreadView != null) {
            if (mUnread != null && !mUnread.equals("0")) {
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
            return mContext.getResources().getString(R.string.Today) + ", " + mTimeFormatter.format(date);
        } else if (mCalendar.get(Calendar.YEAR) == mYesterday.get(Calendar.YEAR) && mCalendar.get(Calendar.DAY_OF_YEAR) == mYesterday.get(Calendar.DAY_OF_YEAR)) {
            return mContext.getResources().getString(R.string.Yesterday) + ", " + mTimeFormatter.format(date);
        } else {
            return DateFormat.format("MMM dd, kk:mm", date).toString();
        }
    }

}
