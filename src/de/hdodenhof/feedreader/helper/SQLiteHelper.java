package de.hdodenhof.feedreader.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "feedreader";

    public static final String FEED_TABLE_NAME = "feeds";
    public static final String FEED_TABLE_COLUMN_ID = "_id";
    public static final String FEED_TABLE_COLUMN_NAME = "name";
    public static final String FEED_TABLE_COLUMN_URL = "url";

    public static final String ARTICLE_TABLE_NAME = "articles";
    public static final String ARTICLE_TABLE_COLUMN_ID = "_id";
    public static final String ARTICLE_TABLE_COLUMN_FEEDID = "feedid";
    public static final String ARTICLE_TABLE_COLUMN_GUID = "guid";
    public static final String ARTICLE_TABLE_COLUMN_PUBDATE = "pubdate";
    public static final String ARTICLE_TABLE_COLUMN_TITLE = "title";
    public static final String ARTICLE_TABLE_COLUMN_SUMMARY = "summary";
    public static final String ARTICLE_TABLE_COLUMN_CONTENT = "content";
    public static final String ARTICLE_TABLE_COLUMN_READ = "read";

    private String dummydata[][] = { { "t3n News", "http://t3n.de/news/feed" },
            { "Gründerszene.de - Infos für Gründer, Unternehmer, StartUps | Gründerszene", "http://www.gruenderszene.de/feed/" },
            { "netzpolitik.org", "http://netzpolitik.org/feed" }, { "Android Developers Blog", "http://android-developers.blogspot.com/atom.xml" } };

    private static final String FEED_TABLE_CREATE = "CREATE TABLE " + FEED_TABLE_NAME + " (" + FEED_TABLE_COLUMN_ID + " integer primary key autoincrement, "
            + FEED_TABLE_COLUMN_NAME + " TEXT, " + FEED_TABLE_COLUMN_URL + " TEXT);";

    private static final String ARTICLE_TABLE_CREATE = "CREATE TABLE " + ARTICLE_TABLE_NAME + " (" + ARTICLE_TABLE_COLUMN_ID
            + " integer primary key autoincrement, " + ARTICLE_TABLE_COLUMN_FEEDID + " integer , " + ARTICLE_TABLE_COLUMN_GUID + " TEXT , "
            + ARTICLE_TABLE_COLUMN_PUBDATE + " TEXT , " + ARTICLE_TABLE_COLUMN_TITLE + " TEXT , " + ARTICLE_TABLE_COLUMN_SUMMARY + " TEXT , "
            + ARTICLE_TABLE_COLUMN_CONTENT + " TEXT , " + ARTICLE_TABLE_COLUMN_READ + " INTEGER);";

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(FEED_TABLE_CREATE);
        database.execSQL(ARTICLE_TABLE_CREATE);

        for (String[] data : dummydata) {
            ContentValues values = new ContentValues();
            values.put(SQLiteHelper.FEED_TABLE_COLUMN_NAME, data[0]);
            values.put(SQLiteHelper.FEED_TABLE_COLUMN_URL, data[1]);

            database.insert(SQLiteHelper.FEED_TABLE_NAME, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FEED_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ARTICLE_TABLE_NAME);
        onCreate(db);
    }

}
