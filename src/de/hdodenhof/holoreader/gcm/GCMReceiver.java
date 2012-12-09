package de.hdodenhof.holoreader.gcm;

import android.content.Context;

import com.google.android.gcm.GCMBroadcastReceiver;

public class GCMReceiver extends GCMBroadcastReceiver {
    @Override
    protected String getGCMIntentServiceClassName(Context context) {
        return "de.hdodenhof.holoreader.gcm.GCMIntentService";
    }
}
