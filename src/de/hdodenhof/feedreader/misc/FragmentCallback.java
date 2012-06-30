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
         * @param fragment
         *                Fragment that called
         */
        public void onFragmentReady(Handler handler);
        
        public boolean isDualPane();
}
