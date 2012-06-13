package de.hdodenhof.feedreader.models;

import java.util.Date;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class Article {

        private int mID;
        private int mFeedID;
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

        public int getId() {
                return mID;
        }

        public void setId(int id) {
                this.mID = id;
        }

        public int getFeedId() {
                return mFeedID;
        }

        public void setFeedId(int feedId) {
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
