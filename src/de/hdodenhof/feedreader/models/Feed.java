package de.hdodenhof.feedreader.models;

import java.util.ArrayList;

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

        public void setArticles(ArrayList<Article> mArticles) {
                this.mArticles = mArticles;
        }

        @Override
        public String toString() {
                return getName();
        }
}
