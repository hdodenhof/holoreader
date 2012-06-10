package de.hdodenhof.feedreader.controller;

import java.sql.SQLException;
import java.util.ArrayList;

import android.content.Context;

import de.hdodenhof.feedreader.dao.FeedsDataSource;
import de.hdodenhof.feedreader.model.Feed;

public class FeedController {
        private FeedsDataSource mFeedsDataSource;

        public FeedController(Context context) {
                mFeedsDataSource = new FeedsDataSource(context);
        }

        private void connect() {
                try {
                        mFeedsDataSource.open();
                } catch (SQLException e) {
                        e.printStackTrace();
                }
        }

        private void disconnect() {
                mFeedsDataSource.close();
        }

        public Feed getFeed(long feedid) {
                connect();
                Feed mFeed = mFeedsDataSource.getFeed(feedid);
                disconnect();
                return mFeed;
        }

        public ArrayList<Feed> getAllFeeds() {
                connect();
                ArrayList<Feed> mFeeds = (ArrayList<Feed>) mFeedsDataSource.getAllFeeds();
                disconnect();
                return mFeeds;
        }

        public void deleteFeed(Feed feed) {
                connect();
                mFeedsDataSource.deleteFeed(feed);
                disconnect();
        }

        public void addFeed(Feed feed) {
                connect();
                mFeedsDataSource.createFeed(feed.getName(), feed.getUrl());
                disconnect();
        }

}
