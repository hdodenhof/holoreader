package de.hdodenhof.feedreader.models;

import java.util.ArrayList;
import java.util.Date;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class Feed {
        private int mID;
        private String mName;
        private String mURL;
        private ArrayList<Article> mArticles;
        private Date mUpdated;
        private int mUnread;

        public int getId() {
                return mID;
        }

        public void setId(int id) {
                this.mID = id;
        }

        public String getUrl() {
                return mURL;
        }

        public void setUrl(String comment) {
                this.mURL = comment;
        }

        public String getName() {
                return mName;
        }

        public void setName(String name) {
                this.mName = name;
        }

        public ArrayList<Article> getArticles() {
                return mArticles;
        }

        public void setArticles(ArrayList<Article> articles) {
                this.mArticles = articles;
        }

        public Date getUpdated() {
                return mUpdated;
        }

        public void setUpdated(Date updated) {
                this.mUpdated = updated;
        }

        public int getUnread() {
                return mUnread;
        }

        public void setUnread(int unread) {
                this.mUnread = unread;
        }

        @Override
        public String toString() {
                return getName();
        }
}
