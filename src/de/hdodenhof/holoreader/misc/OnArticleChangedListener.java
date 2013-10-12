package de.hdodenhof.holoreader.misc;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public interface OnArticleChangedListener {
    /**
     * Is called from within the ArticlePagerFragment after the visible article changed
     *
     * @param oldArticle
     * @param currentArticle
     * @param position
     *            Position of the article within a list
     *
     */
    public void onArticleChanged(int oldArticle, int currentArticle, int position);
}
