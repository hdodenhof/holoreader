package de.hdodenhof.holoreader.services;

import java.util.HashSet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.hdodenhof.holoreader.misc.Extras;
import de.hdodenhof.holoreader.misc.Helpers;

public class RefreshFeedReceiver extends BroadcastReceiver {

    public RefreshFeedReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            refreshFeeds(context);
        }
        else {
            RefreshFeedService.scheduleRefresh(context, 0l);
        }
    }

    private void refreshFeeds(Context context) {
        if (Helpers.isConnected(context)) {
            HashSet<Integer> feedIDs = Helpers.queryFeeds(context.getContentResolver());
            if (!feedIDs.isEmpty()) {
                for (Integer mFeedID : feedIDs) {
                    Intent serviceIntent = new Intent(context, RefreshFeedService.class);
                    serviceIntent.putExtra(Extras.FEEDID, mFeedID);

                    context.startService(serviceIntent);
                }
            }
        }
    }
}