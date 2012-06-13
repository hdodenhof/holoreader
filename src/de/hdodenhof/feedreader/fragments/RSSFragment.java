package de.hdodenhof.feedreader.fragments;

import de.hdodenhof.feedreader.misc.RSSMessage;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public interface RSSFragment {
        /**
         * Handles messages from the hosting activity
         * 
         * @param message
         *                Message to be handled
         */
        public void handleMessage(RSSMessage message);
}
