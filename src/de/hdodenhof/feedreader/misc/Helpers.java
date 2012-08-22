package de.hdodenhof.feedreader.misc;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.hdodenhof.feedreader.fragments.DynamicDialogFragment;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

public class Helpers {

    /**
     * Checks the devices connectivity
     * 
     * @return True if connection is available, false if not
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo == null ? false : networkInfo.isAvailable();
    }

    /**
     * Shows a simple dialog
     * 
     * @param title
     *            Dialog title
     * @param message
     *            Dialog message
     */
    public static void showDialog(Context context, String title, String message) {
        DynamicDialogFragment dialogFragment = DynamicDialogFragment.Factory.getInstance(context);
        dialogFragment.setTitle(title);
        dialogFragment.setMessage(message);
        dialogFragment.show(((SherlockFragmentActivity) context).getSupportFragmentManager(), "dialog");
    }

    /**
     * Queries the name of the feed with the given ID
     * 
     * @param feedID
     * @return Name of the feed with the given ID
     */
    public static String queryFeedName(ContentResolver contentResolver, int feedID) {
        Uri baseUri = Uri.withAppendedPath(RSSContentProvider.URI_FEEDS, String.valueOf(feedID));
        String[] projection = { FeedDAO._ID, FeedDAO.NAME };

        Cursor cursor = contentResolver.query(baseUri, projection, null, null, null);
        cursor.moveToFirst();
        String feedName = cursor.getString(cursor.getColumnIndex(FeedDAO.NAME));
        cursor.close();

        return feedName;
    }

    /**
     * @param date
     * @return
     */
    public static String formatToYesterdayOrToday(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        SimpleDateFormat timeFormatter = new SimpleDateFormat("kk:mm");

        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return "Today, " + timeFormatter.format(date);
        } else if (calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday, " + timeFormatter.format(date);
        } else {
            return DateFormat.format("MMM dd, kk:mm", date).toString();
        }
    }

    /**
     * 
     * @param context
     * @param view
     * @param resid
     * @return
     */
    public static View addBackgroundIndicator(Context context, View view, int resid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            TypedArray attributes = context.obtainStyledAttributes(new int[] { resid });
            int resource = attributes.getResourceId(0, 0);
            attributes.recycle();

            // setBackgroundResource resets padding
            int paddingLeft = view.getPaddingLeft();
            int paddingTop = view.getPaddingTop();
            int paddingRight = view.getPaddingRight();
            int paddingBottom = view.getPaddingBottom();
            view.setBackgroundResource(resource);
            view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }
        return view;
    }
}
