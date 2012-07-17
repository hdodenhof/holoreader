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
    int mArticleID;

    public MarkReadRunnable(Context context, int articleID) {
        this.mContext = context;
        this.mArticleID = articleID;
    }

    public void run() {
        ContentResolver mContentResolver = mContext.getContentResolver();
        ContentValues mContentValues = new ContentValues();
        Uri mUri = Uri.withAppendedPath(RSSContentProvider.URI_ARTICLES, String.valueOf(mArticleID));

        mContentValues.put(ArticleDAO.READ, SQLiteHelper.fromBoolean(true));

        mContentResolver.update(mUri, mContentValues, null, null);
    }
}