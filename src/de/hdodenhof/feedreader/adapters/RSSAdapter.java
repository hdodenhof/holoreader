package de.hdodenhof.feedreader.adapters;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public interface RSSAdapter {
        
        public static final int TYPE_FEED = 1;
        public static final int TYPE_ARTICLE = 2;
        
        public int getType();
}
