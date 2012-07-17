package de.hdodenhof.feedreader.misc;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public interface OnArticleChangedListener {
    /**
     * Is called from within the ArticlePagerFragment after the visible article changed
     * 
     * @param article
     *            Article that has been activated
     * @param position
     *            Position of the article within a list
     * 
     */
    public void onArticleChanged(int oldArticle, int currentArticle, int position);
}
