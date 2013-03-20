package de.hdodenhof.holoreader.misc;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.webkit.WebView;

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.fragments.DynamicDialogFragment;

public class ChangelogDialog {

    @SuppressWarnings("unused")
    private static final String TAG = ArticleViewPager.class.getSimpleName();

    private static final String STYLE = "h1 { margin-left: 0px; font-size: 12pt; margin-bottom: 0px; }" + "li { margin-left: 0px; font-size: 10pt; }"
            + "ul { padding-left: 30px; }" + ".date { font-size: 9pt; color: #606060;  display: block; }";

    private final Context mContext;

    public ChangelogDialog(Context context) {
        mContext = context;
    }

    private String parseDate(String dateString) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            final Date parsedDate = dateFormat.parse(dateString);
            return DateFormat.getDateFormat(mContext).format(parsedDate);
        } catch (ParseException e) {
            return dateString;
        }
    }

    private void parseReleaseTag(StringBuilder changelogBuilder, XmlPullParser resourceParser) throws XmlPullParserException, IOException {
        changelogBuilder.append("<h1>Version ").append(resourceParser.getAttributeValue(null, "version")).append("</h1>");

        if (resourceParser.getAttributeValue(null, "date") != null) {
            changelogBuilder.append("<span class='date'>").append(parseDate(resourceParser.getAttributeValue(null, "date"))).append("</span>");
        }

        changelogBuilder.append("<ul>");

        int eventType = resourceParser.getEventType();
        while ((eventType != XmlPullParser.END_TAG) || (resourceParser.getName().equals("change"))) {
            if ((eventType == XmlPullParser.START_TAG) && (resourceParser.getName().equals("change"))) {
                eventType = resourceParser.next();
                changelogBuilder.append("<li>" + resourceParser.getText() + "</li>");
            }
            eventType = resourceParser.next();
        }

        changelogBuilder.append("</ul>");
    }

    private String getHTMLChangelog(int resourceId) {
        final StringBuilder changelogBuilder = new StringBuilder();

        changelogBuilder.append("<html>");
        changelogBuilder.append("<head>").append("<style type=\"text/css\">").append(STYLE).append("</style>").append("</head>");
        changelogBuilder.append("<body>");

        final XmlResourceParser xml = mContext.getResources().getXml(resourceId);
        try {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if ((eventType == XmlPullParser.START_TAG) && (xml.getName().equals("release"))) {
                    parseReleaseTag(changelogBuilder, xml);
                }
                eventType = xml.next();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            xml.close();
        }

        changelogBuilder.append("</body>");
        changelogBuilder.append("</html>");

        return changelogBuilder.toString();
    }

    public void show() {
        String title = mContext.getString(R.string.ChangelogTitle);
        String htmlChangelog = getHTMLChangelog(R.xml.changelog);

        if (htmlChangelog.length() == 0) {
            return;
        }

        WebView webView = new WebView(mContext);
        webView.loadDataWithBaseURL(null, htmlChangelog, "text/html", "utf-8", null);

        DynamicDialogFragment dialogFragment = DynamicDialogFragment.Factory.getInstance(mContext);
        dialogFragment.setTitle(title);
        dialogFragment.setView(webView);
        dialogFragment.show(((FragmentActivity) mContext).getSupportFragmentManager(), "changelog");
    }
}
