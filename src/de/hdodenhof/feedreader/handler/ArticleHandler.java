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
        private Article mArticle = null;

        private ArrayList<Article> mArticleList;
        private StringBuffer mSb;

        private boolean mIsGuid = false;
        private boolean mIsPubdate = false;
        private boolean mIsArticle = false;
        private boolean mIsTitle = false;
        private boolean mIsContent = false;
        private boolean mIsSummary = false;

        private static final String DATE_FORMATS[] = { "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss z", "yyyy-MM-dd'T'HH:mm:ssz",
                        "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSZ" };
        private SimpleDateFormat mSimpleDateFormats[] = new SimpleDateFormat[DATE_FORMATS.length];

        public ArticleHandler() {
                super();
                mArticleList = new ArrayList<Article>();

                for (int i = 0; i < DATE_FORMATS.length; i++) {
                        mSimpleDateFormats[i] = new SimpleDateFormat(DATE_FORMATS[i], Locale.US);
                        mSimpleDateFormats[i].setTimeZone(TimeZone.getTimeZone("GMT"));
                }
        }

        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {

                mSb = new StringBuffer();
                String mValue = localName.trim();

                if (mValue.equalsIgnoreCase("TITLE") && mIsArticle == true) {
                        mIsTitle = true;
                }

                else if ((mValue.equalsIgnoreCase("DESCRIPTION") || mValue.equalsIgnoreCase("SUMMARY")) && mIsArticle == true) {
                        mIsSummary = true;
                }

                else if ((mValue.equalsIgnoreCase("ENCODED") || mValue.equalsIgnoreCase("CONTENT")) && mIsArticle == true) {
                        mIsContent = true;
                }

                else if ((mValue.equalsIgnoreCase("GUID") || mValue.equalsIgnoreCase("ID")) && mIsArticle == true) {
                        mIsGuid = true;
                }

                else if (mValue.equalsIgnoreCase("PUBDATE") || mValue.equalsIgnoreCase("PUBLISHED") || mValue.equalsIgnoreCase("DATE"))
                        mIsPubdate = true;

                else if (mValue.equalsIgnoreCase("ITEM") || mValue.equalsIgnoreCase("ENTRY")) {
                        mArticle = new Article();
                        mIsArticle = true;
                }

        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

                if (mIsArticle) {
                        if (localName.equalsIgnoreCase("ITEM") || localName.equalsIgnoreCase("ENTRY")) {
                                mArticleList.add(mArticle);
                                mArticle = null;
                                mIsArticle = false;
                        }

                        else if (mIsTitle) {
                                mArticle.setTitle(mSb.toString().trim());
                                mIsTitle = false;
                        }

                        else if (mIsSummary) {
                                mArticle.setSummary(mSb.toString().trim());
                                mIsSummary = false;
                        }

                        else if (mIsGuid) {
                                mArticle.setGuid(mSb.toString().trim());
                                mIsGuid = false;
                        }

                        else if (mIsPubdate) {
                                for (int i = 0; i < DATE_FORMATS.length; i++) {
                                        try {
                                                mArticle.setPubDate(mSimpleDateFormats[i].parse(mSb.toString().trim()));
                                                break;
                                        } catch (ParseException pe) {
                                                if (i == DATE_FORMATS.length - 1) {
                                                        throw new SAXException(pe);
                                                }
                                        }
                                }

                                mIsPubdate = false;
                        }

                        else if (mIsContent) {
                                mArticle.setContent(mSb.toString().trim());
                                mIsContent = false;
                        }

                }

        }

        public void characters(char[] ch, int start, int length) throws SAXException {

                if (mIsArticle && (mIsTitle || mIsContent || mIsSummary || mIsGuid || mIsPubdate)) {
                        mSb.append(new String(ch, start, length));
                }

        }

        public Object getResult() {
                return this.mArticleList;
        }
}
