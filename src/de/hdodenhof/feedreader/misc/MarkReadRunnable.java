package de.hdodenhof.feedreader.misc;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;

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
        String selection = ArticleDAO.READ + " != ?";
        String[] selectionArgs = new String[] { String.valueOf(SQLiteHelper.fromBoolean(true)) };
        Uri uri;

        if (mArticleID != -1) {
            uri = Uri.withAppendedPath(RSSContentProvider.URI_ARTICLES, String.valueOf(mArticleID));
        } else {
            uri = RSSContentProvider.URI_ARTICLES;
            if (mFeedID != -1) {
                selection = selection + " AND " + ArticleDAO.FEEDID + " = ? ";
                selectionArgs = new String[] { selectionArgs[0], String.valueOf(mFeedID) };
            }
        }
        contentValues.put(ArticleDAO.READ, SQLiteHelper.fromBoolean(true));
        contentResolver.update(uri, contentValues, selection, selectionArgs);

    }
}
