package de.hdodenhof.feedreader.provider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
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

    private static final String DATABASE_NAME = "feedreader.db";
    private static final int DATABASE_VERSION = 16;

    private static final String FEED_TABLE_CREATE = "CREATE TABLE " + FeedDAO.TABLE + " (" + FeedDAO._ID + " integer primary key autoincrement, "
            + FeedDAO.NAME + " TEXT, " + FeedDAO.URL + " TEXT);";

    private static final String ARTICLE_TABLE_CREATE = "CREATE TABLE " + ArticleDAO.TABLE + " (" + ArticleDAO._ID + " integer primary key autoincrement, "
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

    /* @formatter:off */
    private static final String mDummydata[][] = {
        { "Android Developers Blog", "http://android-developers.blogspot.com/atom.xml" },
        { "Android Police - Android News, Apps, Games, Phones, Tablets", "http://feeds.feedburner.com/AndroidPolice" },
        { "Android UI Patterns", "http://feeds.feedburner.com/AndroidUiDesignPatterns" },
        { "AndroidDevBlog", "http://android.cyrilmottier.com/?feed=rss2" },
        { "BuildMobile » Android", "http://buildmobile.com/category/android/feed/" },
        { "Clients From Hell", "http://clientsfromhell.net/rss" },
        { "Engadget German", "http://de.engadget.com/rss.xml" },
        { "Facebook Blog", "http://blog.facebook.com/atom.php" },
        { "geek-week.de", "http://www.geek-week.de/feed/" },
        { "Gmail Blog", "http://gmailblog.blogspot.com/atom.xml" },
        { "Google Mobile Blog", "http://googlemobile.blogspot.com/atom.xml" },
        { "Gründerszene", "http://www.gruenderszene.de/feed" },
        { "Holo Everywhere", "http://feeds.feedburner.com/HoloEverywhere" },
        { "In web we trust", "http://feeds.feedburner.com/in_web_we_trust" },
        { "mobiFlip.de", "http://feeds.feedburner.com/mobiflip" },
        { "mobile zeitgeist", "http://feeds.feedburner.com/MobileZeitgeist" },
        { "netzpolitik.org", "http://netzpolitik.org/feed/" },
        { "Official Android Blog", "http://feeds.feedburner.com/OfficialAndroidBlog" },
        { "Pushing Pixels", "http://www.pushing-pixels.org/feed" },
        { "Smashing Magazine Feed", "http://rss1.smashingmagazine.com/feed/" },
        { "t3n News", "http://feeds.feedburner.com/aktuell/feeds/rss" },
        { "Techi.com", "http://feeds.feedburner.com/techirss" },
        { "The CommonsBlog", "http://commonsware.com/blog/feed.atom" },
        { "The Oatmeal - Comics, Quizzes, & Stories", "http://theoatmeal.com/feed/rss" },
        { "The Official Google Blog", "http://googleblog.blogspot.com/atom.xml" }, 
        { "SPIEGEL ONLINE - Schlagzeilen", "http://www.spiegel.de/schlagzeilen/index.rss" },
        { "DIE WELT", "http://www.welt.de/?service=Rss" },
        { "tagesschau.de - Die Nachrichten der ARD", "http://www.tagesschau.de/xml/rss2" },
        { "FOCUS Online - News", "http://rss.focus.de/fol/XML/rss_folnews.xml" },
        { "Handelsblatt Online Schlagzeilen", "http://www.handelsblatt.com/contentexport/feed/schlagzeilen" },
        { "RSS-Feed - die neusten Meldungen von STERN.DE", "http://www.stern.de/feed/standard/all/" },
        { "BILD.de alle Artikel", "http://rss.bild.de/bild.xml" },
        { "heise online News", "http://www.heise.de/newsticker/heise-atom.xml" },
        { "Huffington Post", "http://feeds.huffingtonpost.com/huffingtonpost/raw_feed" },
        { "Buzzfeed", "http://www.buzzfeed.com/index.xml" },
        { "TechCrunch", "http://feeds.feedburner.com/TechCrunch/" },
        { "Mashable", "http://feeds.mashable.com/Mashable?format=xml" },
        { "Gawker.com", "http://feeds.gawker.com/Gawker/full" },
        { "Business Insider", "http://feeds2.feedburner.com/businessinsider" },
        { "CNN Top Stories", "http://rss.cnn.com/rss/cnn_topstories.rss" }
    };
    /* @formatter:on */

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

        for (String[] data : mDummydata) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(FeedDAO.NAME, data[0]);
            contentValues.put(FeedDAO.URL, data[1]);

            database.insert(FeedDAO.TABLE, null, contentValues);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FeedDAO.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ArticleDAO.TABLE);
        db.execSQL("DROP VIEW IF EXISTS " + FeedDAO.VIEW);
        db.execSQL("DROP VIEW IF EXISTS " + ArticleDAO.VIEW);
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

    public static int fromBoolean(boolean bool) {
        if (bool == true) {
            return 1;
        } else {
            return 0;
        }
    }

    public static boolean toBoolean(int integer) {
        if (integer == 1) {
            return true;
        } else {
            return false;
        }
    }

}
