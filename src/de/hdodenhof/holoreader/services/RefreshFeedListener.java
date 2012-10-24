package de.hdodenhof.holoreader.services;

import java.util.HashSet;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import de.hdodenhof.holoreader.misc.Helpers;

public class RefreshFeedListener implements WakefulIntentService.AlarmListener {
    public static final long INTERVAL_MILLIS = 14400000; // 4h
    public static final long WAIT_MILLIS = 60000; // 1min

    private Long mWaitMillis = null;

    public RefreshFeedListener() {
    }

    public RefreshFeedListener(long waitMillis) {
        mWaitMillis = waitMillis;
    }

    public void scheduleAlarms(AlarmManager alarmManager, PendingIntent pendingIntent, Context context) {
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                + ((mWaitMillis == null) ? WAIT_MILLIS : mWaitMillis), INTERVAL_MILLIS, pendingIntent);
    }

    public void sendWakefulWork(Context context) {
        boolean isConnected = Helpers.isConnected(context);

        if (isConnected) {
            HashSet<Integer> feedIDs = Helpers.queryFeeds(context.getContentResolver());
            if (!feedIDs.isEmpty()) {
                for (Integer mFeedID : feedIDs) {
                    Intent intent = new Intent(context, RefreshFeedService.class);
                    intent.putExtra("feedid", mFeedID);

                    WakefulIntentService.sendWakefulWork(context, intent);
                }
            }
        }
    }

    public long getMaxAge() {
        return (INTERVAL_MILLIS + AlarmManager.INTERVAL_FIFTEEN_MINUTES);
    }
}