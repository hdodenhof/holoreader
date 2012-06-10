package de.hdodenhof.feedreader.model;

import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.text.Html;
import android.text.Spanned;

public class Article {

        private long mID;
        private long mFeedID;
        private Date mPubDate;
        private String mGUID;
        private String mTitle;
        private String mSummary;
        private String mContent;
        private boolean mRead;

        public void setTitle(String title) {
                this.mTitle = title;
        }

        public void setSummary(String text) {
                this.mSummary = text;
        }

        public String getTitle() {
                return this.mTitle;
        }

        public String getSummary() {
                return this.mSummary;
        }

        public String getContent() {
                return this.mContent;
        }

        public long getId() {
                return mID;
        }

        public void setId(long id) {
                this.mID = id;
        }

        public long getFeedId() {
                return mFeedID;
        }

        public void setFeedId(long feedId) {
                this.mFeedID = feedId;
        }

        public String getGuid() {
                return mGUID;
        }

        public void setGuid(String guid) {
                this.mGUID = guid;
        }

        public Date getPubDate() {
                return mPubDate;
        }

        public void setPubDate(Date pubDate) {
                this.mPubDate = pubDate;
        }

        public Spanned getFormatedContent() {

                if (mContent != null && mContent.length() != 0) {
                        Document mDOcument = Jsoup.parse(mContent);
                        mDOcument.select("img").remove();
                        return Html.fromHtml(mDOcument.toString());
                } else {
                        return Html.fromHtml("<h1>Full article not supplied</h1>");
                }

        }

        public void setContent(String content) {
                this.mContent = content;
        }

        public boolean isRead() {
                return mRead;
        }

        public void setRead(boolean read) {
                this.mRead = read;
        }

        public String toString() {
                return this.getTitle();
        }

}
