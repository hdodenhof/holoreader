package de.hdodenhof.holoreader.misc;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Pattern;

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

import de.hdodenhof.holoreader.fragments.DynamicDialogFragment;
import de.hdodenhof.holoreader.provider.RSSContentProvider;
import de.hdodenhof.holoreader.provider.SQLiteHelper.FeedDAO;

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
     * Queries all feed ids
     * 
     * @return HashMap of all feed ids
     */
    public static HashSet<Integer> queryFeeds(ContentResolver contentResolver) {
        HashSet<Integer> feeds = new HashSet<Integer>();

        Cursor cursor = contentResolver.query(RSSContentProvider.URI_FEEDS, new String[] { FeedDAO._ID, FeedDAO.NAME, FeedDAO.URL }, null, null, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                feeds.add(cursor.getInt(cursor.getColumnIndex(FeedDAO._ID)));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return feeds;
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

        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");

        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return "Today, " + timeFormatter.format(date);
        } else if (calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday, " + timeFormatter.format(date);
        } else {
            return DateFormat.format("MMM dd, ", date).toString() + timeFormatter.format(date);

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

    /**
     * 
     * @param selectionArgs
     * @param newArg
     * @return
     */
    public static String[] addSelectionArg(String[] selectionArgs, String newArg) {
        int newLength = selectionArgs.length + 1;
        int newPosition = newLength - 1;

        String newSA[] = new String[newLength];

        for (int i = 0; i < selectionArgs.length; i++) {
            newSA[i] = selectionArgs[i];
        }

        newSA[newPosition] = newArg;
        return newSA;
    }

    /* @formatter:off */
    public static final String DEFAULTFEEDS[][] = {
        { "Huffington Post", "http://feeds.huffingtonpost.com/huffingtonpost/raw_feed" },
        { "Buzzfeed", "http://www.buzzfeed.com/index.xml" },
        { "TechCrunch", "http://feeds.feedburner.com/TechCrunch/" },
        { "Mashable", "http://feeds.mashable.com/Mashable?format=xml" },
        { "Gawker.com", "http://feeds.gawker.com/Gawker/full" },
        { "Business Insider", "http://feeds2.feedburner.com/businessinsider" },
        { "CNN Top Stories", "http://rss.cnn.com/rss/cnn_topstories.rss" }
    };
    /* @formatter:on */

    /* @formatter:off */
    /**
     * based on AOSP
     */
    public static final Pattern PATTERN_WEB = Pattern.compile(new StringBuilder()
            .append("((?:(http|Http):")
            .append("\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)")
            .append("\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_")
            .append("\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?")
            .append("((?:(?:[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}\\.)+")
            // named host
            .append("(?:")
            // plus top level domain
            .append("(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])")
            .append("|(?:biz|b[abdefghijmnorstvwyz])")
            .append("|(?:cat|com|coop|c[acdfghiklmnoruvxyz])")
            .append("|d[ejkmoz]")
            .append("|(?:edu|e[cegrstu])")
            .append("|f[ijkmor]")
            .append("|(?:gov|g[abdefghilmnpqrstuwy])")
            .append("|h[kmnrtu]")
            .append("|(?:info|int|i[delmnoqrst])")
            .append("|(?:jobs|j[emop])")
            .append("|k[eghimnrwyz]")
            .append("|l[abcikrstuvy]")
            .append("|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])")
            .append("|(?:name|net|n[acefgilopruz])")
            .append("|(?:org|om)")
            .append("|(?:pro|p[aefghklmnrstwy])")
            .append("|qa")
            .append("|r[eouw]")
            .append("|s[abcdeghijklmnortuvyz]")
            .append("|(?:tel|travel|t[cdfghjklmnoprtvwz])")
            .append("|u[agkmsyz]")
            .append("|v[aceginu]")
            .append("|w[fs]")
            .append("|y[etu]")
            .append("|z[amw]))")
            .append("|(?:(?:25[0-5]|2[0-4]")
            // or ip address
            .append("[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]")
            .append("|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]")
            .append("[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}")
            .append("|[1-9][0-9]|[0-9])))")
            .append("(?:\\:\\d{1,5})?)") // plus option port number
            .append("(\\/(?:(?:[a-zA-Z0-9\\;\\/\\?\\:\\@\\&\\=\\#\\~") // plus option query params
            .append("\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?")
            .append("(?:\\b|$)").toString());
}
