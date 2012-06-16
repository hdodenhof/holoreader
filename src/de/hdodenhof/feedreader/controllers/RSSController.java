package de.hdodenhof.feedreader.controllers;

import java.util.ArrayList;

import android.content.Context;

import de.hdodenhof.feedreader.daos.ArticleDAO;
import de.hdodenhof.feedreader.daos.FeedDAO;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.models.Feed;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class RSSController {

        @SuppressWarnings("unused")
        private static final String TAG = RSSController.class.getSimpleName();

        private FeedDAO mFeedDAO;
        private ArticleDAO mArticleDAO;

        public RSSController(Context context){
                mFeedDAO = new FeedDAO(context);
                mArticleDAO = new ArticleDAO(context);
        }
        
        public ArrayList<Feed> getFeeds(){
                ArrayList<Feed> mFeeds = mFeedDAO.getAll();
                for (Feed mFeed : mFeeds) {
                        mFeed.setArticles(mArticleDAO.getAllWithFeedID(mFeed.getId()));
                        
                        int mUnread = 0;
                        for (Article article : mFeed.getArticles()) {
                                if (!article.isRead()){
                                        mUnread++;
                                }
                        }
                        mFeed.setUnread(mUnread);
                }
                return mFeeds;
        }

        public Feed getFeed(int feedID){
                Feed mFeed = mFeedDAO.get(feedID);
                mFeed.setArticles(mArticleDAO.getAllWithFeedID(mFeed.getId()));

                return mFeed;
        }     
        
        public Article getArticle(int articleID){
                return mArticleDAO.get(articleID);
        }
        
        public void updateArticle(Article article){
                mArticleDAO.update(article);
        }
        
        public void updateFeed(Feed feed){
                mFeedDAO.update(feed);
        }
        
        public void addFeed(Feed feed){
                mFeedDAO.insert(feed);  
        }
        
        public void deleteFeed(Feed feed){
                
        }
        
        public void deleteArticles(int feedID){
                mArticleDAO.deleteWithFeedID(feedID);
        }
        
        public void createOrUpdateArticles(ArrayList<Article> articles){
                for (Article article : articles) {
                        Article mExistingArticle = mArticleDAO.getWithGUID(article.getGuid());
                        if(mExistingArticle != null){
                                article.setRead(mExistingArticle.isRead());
                                mArticleDAO.update(article);
                        } else {
                                mArticleDAO.insert(article);    
                        }
                }
        }
        
}
