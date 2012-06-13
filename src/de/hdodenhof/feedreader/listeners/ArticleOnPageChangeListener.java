package de.hdodenhof.feedreader.listeners;

import de.hdodenhof.feedreader.models.Article;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public interface ArticleOnPageChangeListener {
        /**
         * Is called from within the ArticlePagerFragment after the visible
         * article changed
         * 
         * @param article
         *                Article that has been activated
         * @param position
         *                Position of the article within a list
         * 
         */
        public void onArticleChanged(Article article, int position);
}
