package de.hdodenhof.feedreader.handler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.hdodenhof.feedreader.model.Article;

public class ArticleHandler extends DefaultHandler implements GenericHandler {
    private Article a = null;

    private ArrayList<Article> articleList;
    private StringBuffer mSb;

    private boolean isGuid = false;
    private boolean isPubdate = false;
    private boolean isArticle = false;
    private boolean isTitle = false;
    private boolean isContent = false;
    private boolean isSummary = false;

    private static final String DATE_FORMATS[] = { "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss z", "yyyy-MM-dd'T'HH:mm:ssz",
            "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSZ" };
    private SimpleDateFormat mSimpleDateFormats[] = new SimpleDateFormat[DATE_FORMATS.length];

    public ArticleHandler() {
        super();
        articleList = new ArrayList<Article>();

        for (int i = 0; i < DATE_FORMATS.length; i++) {
            mSimpleDateFormats[i] = new SimpleDateFormat(DATE_FORMATS[i], Locale.US);
            mSimpleDateFormats[i].setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {

        mSb = new StringBuffer();
        String value = localName.trim();

        if (value.equalsIgnoreCase("TITLE") && isArticle == true) {
            isTitle = true;
        }

        else if ((value.equalsIgnoreCase("DESCRIPTION") || value.equalsIgnoreCase("SUMMARY")) && isArticle == true) {
            isSummary = true;
        }

        else if ((value.equalsIgnoreCase("ENCODED") || value.equalsIgnoreCase("CONTENT")) && isArticle == true) {
            isContent = true;
        }

        else if ((value.equalsIgnoreCase("GUID") || value.equalsIgnoreCase("ID")) && isArticle == true) {
            isGuid = true;
        }

        else if (value.equalsIgnoreCase("PUBDATE") || value.equalsIgnoreCase("PUBLISHED") || value.equalsIgnoreCase("DATE"))
            isPubdate = true;

        else if (value.equalsIgnoreCase("ITEM") || value.equalsIgnoreCase("ENTRY")) {
            a = new Article();
            isArticle = true;
        }

    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

        if (isArticle) {
            if (localName.equalsIgnoreCase("ITEM") || localName.equalsIgnoreCase("ENTRY")) {
                articleList.add(a);
                a = null;
                isArticle = false;
            }

            else if (isTitle) {
                a.setTitle(mSb.toString().trim());
                isTitle = false;
            }

            else if (isSummary) {
                a.setSummary(mSb.toString().trim());
                isSummary = false;
            }

            else if (isGuid) {
                a.setGuid(mSb.toString().trim());
                isGuid = false;
            }

            else if (isPubdate) {
                for (int i = 0; i < DATE_FORMATS.length; i++) {
                    try {
                        a.setPubDate(mSimpleDateFormats[i].parse(mSb.toString().trim()));
                        break;
                    } catch (ParseException pe) {
                        if (i == DATE_FORMATS.length - 1) {
                            throw new SAXException(pe);
                        }
                    }
                }

                isPubdate = false;
            }

            else if (isContent) {
                a.setContent(mSb.toString().trim());
                isContent = false;
            }

        }

    }

    public void characters(char[] ch, int start, int length) throws SAXException {

        if (isArticle && (isTitle || isContent || isSummary || isGuid || isPubdate)) {
            mSb.append(new String(ch, start, length));
        }

    }

    public Object getResult() {
        return this.articleList;
    }
}
