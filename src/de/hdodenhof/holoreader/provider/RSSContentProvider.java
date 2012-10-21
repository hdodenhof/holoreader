package de.hdodenhof.holoreader.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import de.hdodenhof.holoreader.provider.SQLiteHelper.ArticleDAO;
import de.hdodenhof.holoreader.provider.SQLiteHelper.FeedDAO;

public class RSSContentProvider extends ContentProvider {

    private SQLiteHelper mSQLiteHelper;

    private static final String AUTHORITY = "de.hdodenhof.holoreader.RSSProvider";

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

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
        case FEEDS:
            queryBuilder.setTables(FeedDAO.VIEW);
            break;
        case FEED_ID:
            queryBuilder.setTables(FeedDAO.VIEW);
            queryBuilder.appendWhere(FeedDAO._ID + "=" + uri.getLastPathSegment());
            break;
        case ARTICLES:
            queryBuilder.setTables(ArticleDAO.VIEW);
            break;
        case ARTICLE_ID:
            queryBuilder.setTables(ArticleDAO.VIEW);
            queryBuilder.appendWhere(ArticleDAO._ID + "=" + uri.getLastPathSegment());
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase database = mSQLiteHelper.getWritableDatabase();
        Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;

    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase database = mSQLiteHelper.getWritableDatabase();
        long mID = 0;
        Uri returnUri;

        switch (uriType) {
        case FEEDS:
            mID = database.insert(FeedDAO.TABLE, null, values);
            returnUri = Uri.parse(BASE_PATH_FEEDS + "/" + mID);
            break;
        case ARTICLES:
            mID = database.insert(ArticleDAO.TABLE, null, values);
            returnUri = Uri.parse(BASE_PATH_ARTICLES + "/" + mID);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        if (uriType == ARTICLES) {
            getContext().getContentResolver().notifyChange(URI_FEEDS, null);
        }

        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase database = mSQLiteHelper.getWritableDatabase();
        int rowsDeleted = 0;
        String id;

        switch (uriType) {
        case FEEDS:
            rowsDeleted = database.delete(FeedDAO.TABLE, selection, selectionArgs);
            break;
        case FEED_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = database.delete(FeedDAO.TABLE, FeedDAO._ID + "=" + id, null);
            } else {
                rowsDeleted = database.delete(FeedDAO.TABLE, FeedDAO._ID + "=" + id + " and " + selection, selectionArgs);
            }
            break;
        case ARTICLES:
            rowsDeleted = database.delete(ArticleDAO.TABLE, selection, selectionArgs);
            getContext().getContentResolver().notifyChange(URI_FEEDS, null);
            break;
        case ARTICLE_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = database.delete(ArticleDAO.TABLE, ArticleDAO._ID + "=" + id, null);
            } else {
                rowsDeleted = database.delete(ArticleDAO.TABLE, ArticleDAO._ID + "=" + id + " and " + selection, selectionArgs);
            }
            getContext().getContentResolver().notifyChange(URI_ARTICLES, null);
            getContext().getContentResolver().notifyChange(URI_FEEDS, null);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriype = sURIMatcher.match(uri);
        SQLiteDatabase database = mSQLiteHelper.getWritableDatabase();
        int rowsUpdated = 0;
        String id;

        switch (uriype) {
        case FEEDS:
            rowsUpdated = database.update(FeedDAO.TABLE, values, selection, selectionArgs);
            break;
        case FEED_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = database.update(FeedDAO.TABLE, values, FeedDAO._ID + "=" + id, null);
            } else {
                rowsUpdated = database.update(FeedDAO.TABLE, values, FeedDAO._ID + "=" + id + " and " + selection, selectionArgs);
            }
            getContext().getContentResolver().notifyChange(URI_FEEDS, null);
            break;
        case ARTICLES:
            rowsUpdated = database.update(ArticleDAO.TABLE, values, selection, selectionArgs);
            if (values.containsKey(ArticleDAO.READ)) {
                getContext().getContentResolver().notifyChange(URI_FEEDS, null);
            }
            break;
        case ARTICLE_ID:
            id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = database.update(ArticleDAO.TABLE, values, ArticleDAO._ID + "=" + id, null);
            } else {
                rowsUpdated = database.update(ArticleDAO.TABLE, values, ArticleDAO._ID + "=" + id + " and " + selection, selectionArgs);
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
        return rowsUpdated;
    }

}
