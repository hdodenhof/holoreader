package de.hdodenhof.feedreader.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "feedreader";

    public static final String FEED_TABLE_NAME = "feeds";
    public static final String FEED_TABLE_COLUMN_ID = "_id";
    public static final String FEED_TABLE_COLUMN_NAME = "name";
    public static final String FEED_TABLE_COLUMN_URL = "url";
    
    private static final String FEED_TABLE_CREATE = "CREATE TABLE " + FEED_TABLE_NAME + " (" + FEED_TABLE_COLUMN_ID + " integer primary key autoincrement, "
            + FEED_TABLE_COLUMN_NAME + " TEXT, " + FEED_TABLE_COLUMN_URL + " TEXT);";

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(FEED_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FEED_TABLE_NAME);
        onCreate(db);
    }

}
