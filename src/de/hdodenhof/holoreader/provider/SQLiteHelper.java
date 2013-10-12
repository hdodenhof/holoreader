package de.hdodenhof.holoreader.provider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class SQLiteHelper extends SQLiteOpenHelper {

    @SuppressWarnings("unused")
    private static final String TAG = SQLiteHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "holoreader.db";
    private static final int DATABASE_VERSION = 1;

    private static final String FEED_TABLE_CREATE = "CREATE TABLE " + FeedDAO.TABLE + " (" + FeedDAO._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + FeedDAO.NAME + " TEXT, " + FeedDAO.URL + " TEXT);";

    private static final String ARTICLE_TABLE_CREATE = "CREATE TABLE " + ArticleDAO.TABLE + " (" + ArticleDAO._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + ArticleDAO.FEEDID + " integer, " + ArticleDAO.GUID + " TEXT, " + ArticleDAO.PUBDATE + " TEXT, " + ArticleDAO.TITLE + " TEXT , "
            + ArticleDAO.SUMMARY + " TEXT, " + ArticleDAO.CONTENT + " TEXT, " + ArticleDAO.IMAGE + " TEXT, " + ArticleDAO.LINK + " TEXT, " + ArticleDAO.READ
            + " TEXT, " + ArticleDAO.ISDELETED + " INTEGER);";

    private static final String FEED_VIEW_CREATE = "CREATE VIEW " + FeedDAO.VIEW + " AS SELECT " + FeedDAO.TABLE + "." + FeedDAO._ID + " AS " + FeedDAO._ID
            + ", " + FeedDAO.TABLE + "." + FeedDAO.NAME + " AS " + FeedDAO.NAME + ", " + FeedDAO.TABLE + "." + FeedDAO.URL + " AS " + FeedDAO.URL + ", MAX("
            + ArticleDAO.TABLE + "." + ArticleDAO.PUBDATE + ") AS " + FeedDAO.UPDATED + ", COUNT(" + ArticleDAO.TABLE + "." + ArticleDAO._ID + ")-SUM("
            + ArticleDAO.TABLE + "." + ArticleDAO.READ + " IS NOT NULL) AS " + FeedDAO.UNREAD + " FROM " + FeedDAO.TABLE + " LEFT OUTER JOIN "
            + ArticleDAO.TABLE + " ON " + ArticleDAO.TABLE + "." + ArticleDAO.FEEDID + " = " + FeedDAO.TABLE + "." + FeedDAO._ID + " GROUP BY " + FeedDAO.TABLE
            + "." + FeedDAO._ID + ", " + FeedDAO.TABLE + "." + FeedDAO.NAME + ", " + FeedDAO.TABLE + "." + FeedDAO.URL + ";";

    private static final String ARTICLE_VIEW_CREATE = "CREATE VIEW " + ArticleDAO.VIEW + " AS SELECT " + ArticleDAO.TABLE + "." + ArticleDAO._ID + " AS "
            + ArticleDAO._ID + ", " + ArticleDAO.TABLE + "." + ArticleDAO.FEEDID + " AS " + ArticleDAO.FEEDID + ", " + FeedDAO.TABLE + "." + FeedDAO.NAME
            + " AS " + ArticleDAO.FEEDNAME + ", " + ArticleDAO.TABLE + "." + ArticleDAO.GUID + " AS " + ArticleDAO.GUID + " , " + ArticleDAO.TABLE + "."
            + ArticleDAO.PUBDATE + " AS " + ArticleDAO.PUBDATE + ", " + ArticleDAO.TABLE + "." + ArticleDAO.TITLE + " AS " + ArticleDAO.TITLE + ", "
            + ArticleDAO.TABLE + "." + ArticleDAO.SUMMARY + " AS " + ArticleDAO.SUMMARY + ", " + ArticleDAO.TABLE + "." + ArticleDAO.CONTENT + " AS "
            + ArticleDAO.CONTENT + ", " + ArticleDAO.TABLE + "." + ArticleDAO.IMAGE + " AS " + ArticleDAO.IMAGE + ", " + ArticleDAO.TABLE + "."
            + ArticleDAO.LINK + " AS " + ArticleDAO.LINK + ", " + ArticleDAO.TABLE + "." + ArticleDAO.READ + " AS " + ArticleDAO.READ + ", " + ArticleDAO.TABLE
            + "." + ArticleDAO.ISDELETED + " AS " + ArticleDAO.ISDELETED + " FROM " + ArticleDAO.TABLE + " LEFT JOIN " + FeedDAO.TABLE + " ON "
            + ArticleDAO.TABLE + "." + ArticleDAO.FEEDID + " = " + FeedDAO.TABLE + "." + FeedDAO._ID + ";";

    private static final String ARTICLE_INDEX_FEEDID_CREATE = "CREATE INDEX " + ArticleDAO.IDX_FEEDID + " ON " + ArticleDAO.TABLE + " (" + ArticleDAO.FEEDID
            + ");";

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(FEED_TABLE_CREATE);
        database.execSQL(ARTICLE_TABLE_CREATE);
        database.execSQL(FEED_VIEW_CREATE);
        database.execSQL(ARTICLE_VIEW_CREATE);
        database.execSQL(ARTICLE_INDEX_FEEDID_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FeedDAO.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ArticleDAO.TABLE);
        db.execSQL("DROP VIEW IF EXISTS " + FeedDAO.VIEW);
        db.execSQL("DROP VIEW IF EXISTS " + ArticleDAO.VIEW);
        db.execSQL("DROP INDEX IF EXISTS " + ArticleDAO.IDX_FEEDID);
        onCreate(db);
    }

    public class FeedDAO {

        public static final String TABLE = "feeds";
        public static final String _ID = "_id";
        public static final String NAME = "name";
        public static final String URL = "url";
        public static final String UPDATED = "updated";
        public static final String UNREAD = "unread";

        public static final String VIEW = "feeds_view";

    }

    public class ArticleDAO {

        public static final String TABLE = "articles";
        public static final String _ID = "_id";
        public static final String FEEDID = "feedid";
        public static final String FEEDNAME = "feedname";
        public static final String GUID = "guid";
        public static final String PUBDATE = "pubdate";
        public static final String TITLE = "title";
        public static final String SUMMARY = "summary";
        public static final String CONTENT = "content";
        public static final String IMAGE = "image";
        public static final String LINK = "link";
        public static final String READ = "read";
        public static final String ISDELETED = "isdeleted";

        public static final String VIEW = "articles_view";

        private static final String IDX_FEEDID = "idx_article_feedid";

    }

    public static String fromDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public static Date toDate(String date) {
        try {
            return DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }

    }

}
