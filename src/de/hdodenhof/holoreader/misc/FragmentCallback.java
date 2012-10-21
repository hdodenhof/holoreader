package de.hdodenhof.holoreader.misc;

import android.support.v4.app.Fragment;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public interface FragmentCallback {
    /**
     * Is called from within an RSSFragment's onViewCreated method and notifies the hosting activity about its state
     * 
     * @param fragment
     *            The calling fragment
     */
    public void onFragmentReady(Fragment fragment);

    /**
     * 
     * @return true if fragment is part of a dual pane layout, false if not
     */
    public boolean isDualPane();

    /**
     * 
     * @param fragment
     * @return true if calling fragment is the primary one in the current layout
     */
    public boolean isPrimaryFragment(Fragment fragment);
}
