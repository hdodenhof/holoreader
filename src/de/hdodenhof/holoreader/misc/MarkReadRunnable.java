package de.hdodenhof.holoreader.misc;

import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import de.hdodenhof.holoreader.provider.RSSContentProvider;
import de.hdodenhof.holoreader.provider.SQLiteHelper;
import de.hdodenhof.holoreader.provider.SQLiteHelper.ArticleDAO;

public class MarkReadRunnable implements Runnable {
    Context mContext;
    int mArticleID = -1;
    int mFeedID = -1;

    public MarkReadRunnable(Context context) {
        mContext = context;
    }

    public void setArticle(int articleID) {
        mArticleID = articleID;
        mFeedID = -1;
    }

    public void setFeed(int feedID) {
        mFeedID = feedID;
        mArticleID = -1;
    }

    public void run() {
        ContentResolver contentResolver = mContext.getContentResolver();
        ContentValues contentValues = new ContentValues();
        String selection = ArticleDAO.READ + " IS NULL";
        String[] selectionArgs = null;
        Uri uri;

        if (mArticleID != -1) {
            uri = Uri.withAppendedPath(RSSContentProvider.URI_ARTICLES, String.valueOf(mArticleID));
        } else {
            uri = RSSContentProvider.URI_ARTICLES;
            if (mFeedID != -1) {
                selection = selection + " AND " + ArticleDAO.FEEDID + " = ? ";
                selectionArgs = new String[] { String.valueOf(mFeedID) };
            }
        }
        contentValues.put(ArticleDAO.READ, SQLiteHelper.fromDate(new Date()));
        contentResolver.update(uri, contentValues, selection, selectionArgs);

    }
}
