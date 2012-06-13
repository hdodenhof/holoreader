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
        public static final int FEEDS_UPDATED = 2;
        public static final int FEED_UPDATED = 3;
        public static final int ARTICLE_UPDATED = 4;
        public static final int POSITION_UPDATED = 5;
        public static final int CHOICE_MODE_SINGLE = 9;
        
        public ArrayList<Feed> feeds;
        public Feed feed;
        public Article article;
        public int position;
        public int type;

}
