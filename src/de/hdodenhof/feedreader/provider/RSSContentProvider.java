package de.hdodenhof.feedreader.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

public class RSSContentProvider extends ContentProvider {

    private SQLiteHelper mSQLiteHelper;

    private static final String AUTHORITY = "de.hdodenhof.feedreader.RSSProvider";

    public static final int FEEDS = 110;
    public static final int FEED_ID = 120;
    public static final int ARTICLES = 210;
    public static final int ARTICLE_ID = 220;

    private static final String BASE_PATH_FEEDS = "feeds";
    private static final String BASE_PATH_ARTICLES = "articles";

    public static final Uri URI_FEEDS = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_FEEDS);
    public static final Uri URI_ARTICLES = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_ARTICLES);

    public static final String TYPE_FEEDS = ContentResolver.CURSOR_DIR_BASE_TYPE + "/feeds";
    public static final String ITEM_TYPE_FEEDS = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/feed";
    public static final String TYPE_ARTICLES = ContentResolver.CURSOR_DIR_BASE_TYPE + "/articles";
    public static final String ITEM_TYPE_ARTICLES = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/article";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_FEEDS, FEEDS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_FEEDS + "/#", FEED_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_ARTICLES, ARTICLES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_ARTICLES + "/#", ARTICLE_ID);
    }

    @Override
    public boolean onCreate() {
        mSQLiteHelper = new SQLiteHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder mQueryBuilder = new SQLiteQueryBuilder();

        int mURIType = sURIMatcher.match(uri);
        switch (mURIType) {
        case FEEDS:
            mQueryBuilder.setTables(FeedDAO.VIEW);
            break;
        case FEED_ID:
            mQueryBuilder.setTables(FeedDAO.VIEW);
            mQueryBuilder.appendWhere(FeedDAO._ID + "=" + uri.getLastPathSegment());
            break;
        case ARTICLES:
            mQueryBuilder.setTables(ArticleDAO.VIEW);
            break;
        case ARTICLE_ID:
            mQueryBuilder.setTables(ArticleDAO.VIEW);
            mQueryBuilder.appendWhere(ArticleDAO._ID + "=" + uri.getLastPathSegment());
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase mDatabase = mSQLiteHelper.getWritableDatabase();
        Cursor mCursor = mQueryBuilder.query(mDatabase, projection, selection, selectionArgs, null, null, sortOrder);

        mCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return mCursor;

    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int mURIType = sURIMatcher.match(uri);
        SQLiteDatabase mDatabase = mSQLiteHelper.getWritableDatabase();
        long mID = 0;
        Uri mURI;

        switch (mURIType) {
        case FEEDS:
            mID = mDatabase.insert(FeedDAO.TABLE, null, values);
            mURI = Uri.parse(BASE_PATH_FEEDS + "/" + mID);
            break;
        case ARTICLES:
            mID = mDatabase.insert(ArticleDAO.TABLE, null, values);
            mURI = Uri.parse(BASE_PATH_ARTICLES + "/" + mID);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        if (mURIType == ARTICLES) {
            getContext().getContentResolver().notifyChange(URI_FEEDS, null);
        }

        return mURI;
    }

    // @Override
    // public int bulkInsert(Uri uri, ContentValues[] values) {
    // int numValues = values.length;
    // for (int i = 0; i < numValues; i++) {
    // insert(uri, values[i]);
    // }
    // return numValues;
    // }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int mURIType = sURIMatcher.match(uri);
        SQLiteDatabase mDatabase = mSQLiteHelper.getWritableDatabase();
        int mRowsDeleted = 0;
        String mID;

        switch (mURIType) {
        case FEEDS:
            mRowsDeleted = mDatabase.delete(FeedDAO.TABLE, selection, selectionArgs);
            break;
        case FEED_ID:
            mID = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                mRowsDeleted = mDatabase.delete(FeedDAO.TABLE, FeedDAO._ID + "=" + mID, null);
            } else {
                mRowsDeleted = mDatabase.delete(FeedDAO.TABLE, FeedDAO._ID + "=" + mID + " and " + selection, selectionArgs);
            }
            break;
        case ARTICLES:
            mRowsDeleted = mDatabase.delete(ArticleDAO.TABLE, selection, selectionArgs);
            break;
        case ARTICLE_ID:
            mID = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                mRowsDeleted = mDatabase.delete(ArticleDAO.TABLE, ArticleDAO._ID + "=" + mID, null);
            } else {
                mRowsDeleted = mDatabase.delete(ArticleDAO.TABLE, ArticleDAO._ID + "=" + mID + " and " + selection, selectionArgs);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return mRowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int mURIype = sURIMatcher.match(uri);
        SQLiteDatabase mDatabase = mSQLiteHelper.getWritableDatabase();
        int mRowsUpdated = 0;
        String mID;

        switch (mURIype) {
        case FEEDS:
            mRowsUpdated = mDatabase.update(FeedDAO.TABLE, values, selection, selectionArgs);
            break;
        case FEED_ID:
            mID = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                mRowsUpdated = mDatabase.update(FeedDAO.TABLE, values, FeedDAO._ID + "=" + mID, null);
            } else {
                mRowsUpdated = mDatabase.update(FeedDAO.TABLE, values, FeedDAO._ID + "=" + mID + " and " + selection, selectionArgs);
            }
            getContext().getContentResolver().notifyChange(URI_FEEDS, null);
            break;
        case ARTICLES:
            mRowsUpdated = mDatabase.update(ArticleDAO.TABLE, values, selection, selectionArgs);
            if (values.containsKey(ArticleDAO.READ)) {
                getContext().getContentResolver().notifyChange(URI_FEEDS, null);
            }
            break;
        case ARTICLE_ID:
            mID = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                mRowsUpdated = mDatabase.update(ArticleDAO.TABLE, values, ArticleDAO._ID + "=" + mID, null);
            } else {
                mRowsUpdated = mDatabase.update(ArticleDAO.TABLE, values, ArticleDAO._ID + "=" + mID + " and " + selection, selectionArgs);
            }
            getContext().getContentResolver().notifyChange(URI_ARTICLES, null);
            if (values.containsKey(ArticleDAO.READ)) {
                getContext().getContentResolver().notifyChange(URI_FEEDS, null);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return mRowsUpdated;
    }

}
