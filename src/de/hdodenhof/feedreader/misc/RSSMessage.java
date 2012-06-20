package de.hdodenhof.feedreader.misc;

import java.util.ArrayList;

import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class RSSMessage {

        public static final int INITIALIZE = 1;
        public static final int FEEDLIST_UPDATED = 2;
        public static final int FEED_SELECTED = 3;
        public static final int FEED_UPDATED = 4;
        public static final int ARTICLE_SELECTED = 5;
        public static final int ARTICLE_UPDATED = 6;
        public static final int POSITION_CHANGED = 7;
        public static final int CHOICE_MODE_SINGLE_FEED = 8;
        public static final int CHOICE_MODE_SINGLE_ARTICLE = 9;
        
        public ArrayList<Feed> feeds;
        public Feed feed;
        public Article article;
        public int position;
        public int type;

}
