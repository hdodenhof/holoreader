package de.hdodenhof.feedreader.listeners;

import de.hdodenhof.feedreader.fragments.RSSFragment;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public interface OnFragmentReadyListener {
        /**
         * Is called from within an RSSFragment's onViewCreated method and
         * initializes the calling fragment
         * 
         * @param fragment
         *                Fragment that called
         */
        public void onFragmentReady(RSSFragment fragment);
}
