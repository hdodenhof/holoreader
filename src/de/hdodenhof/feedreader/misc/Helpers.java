package de.hdodenhof.feedreader.misc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Helpers {

    /**
     * Checks the devices connectivity
     * 
     * @return True if connection is available, false if not
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        return mNetworkInfo == null ? false : mNetworkInfo.isAvailable();
    }
}
