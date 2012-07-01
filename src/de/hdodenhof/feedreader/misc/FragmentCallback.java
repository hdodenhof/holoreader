package de.hdodenhof.feedreader.misc;

import android.os.Handler;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public interface FragmentCallback {
        /**
         * Is called from within an RSSFragment's onViewCreated method and
         * initializes the calling fragment
         * 
         * @param handler
         *                The fragments handler
         */
        public void onFragmentReady(Handler handler);

        /**
         * 
         * @return True if fragment is part of a dual pane layout, false is not
         */
        public boolean isDualPane();
}
