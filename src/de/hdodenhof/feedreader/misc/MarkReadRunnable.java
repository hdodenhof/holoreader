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
        this.mContext = context;
    }

    public void setArticle(int articleID) {
        this.mArticleID = articleID;
        this.mFeedID = -1;
    }

    public void setFeed(int feedID) {
        this.mFeedID = feedID;
        this.mArticleID = -1;
    }

    public void run() {
        ContentResolver mContentResolver = mContext.getContentResolver();
        ContentValues mContentValues = new ContentValues();
        String mSelection = ArticleDAO.READ + " != ?";
        String[] mSelectionArgs = new String[] { String.valueOf(SQLiteHelper.fromBoolean(true)) };
        Uri mUri;

        if (mArticleID != -1) {
            mUri = Uri.withAppendedPath(RSSContentProvider.URI_ARTICLES, String.valueOf(mArticleID));
        } else {
            mUri = RSSContentProvider.URI_ARTICLES;
            if (mFeedID != -1) {
                mSelection = mSelection + " AND " + ArticleDAO.FEEDID + " = ? ";
                mSelectionArgs = new String[] { mSelectionArgs[0], String.valueOf(mFeedID) };
            }
        }
        mContentValues.put(ArticleDAO.READ, SQLiteHelper.fromBoolean(true));
        mContentResolver.update(mUri, mContentValues, mSelection, mSelectionArgs);

    }
}
