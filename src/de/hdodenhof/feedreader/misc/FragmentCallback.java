package de.hdodenhof.feedreader.misc;

import android.support.v4.app.Fragment;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public interface FragmentCallback {
    /**
     * Is called from within an RSSFragment's onViewCreated method and inotifies the hosting actitivy about its state
     * 
     * @param fragment
     *            The calling fragment
     */
    public void onFragmentReady(Fragment fragment);

    /**
     * 
     * @return True if fragment is part of a dual pane layout, false is not
     */
    public boolean isDualPane();
    
    public boolean isPrimaryFragment(Fragment fragment);
}
