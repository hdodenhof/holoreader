package de.hdodenhof.holoreader.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import de.hdodenhof.holoreader.misc.Helpers;

public class RefreshFeedListener implements WakefulIntentService.AlarmListener {
    public static final long INTERVAL_MILIS = 14400000; // 4h
    private static final long WAIT_MILIS = 60000; // 1min

    private Long mWaitMilis = null;

    public RefreshFeedListener() {
    }

    public RefreshFeedListener(long waitMilis) {
        mWaitMilis = waitMilis;
    }

    public void scheduleAlarms(AlarmManager alarmManager, PendingIntent pendingIntent, Context context) {
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ((mWaitMilis == null) ? WAIT_MILIS : mWaitMilis),
                INTERVAL_MILIS, pendingIntent);
    }

    public void sendWakefulWork(Context context) {
        boolean isConnected = Helpers.isConnected(context);

        if (isConnected) {
            for (Integer mFeedID : Helpers.queryFeeds(context.getContentResolver())) {
                Intent intent = new Intent(context, RefreshFeedService.class);
                intent.putExtra("feedid", mFeedID);

                WakefulIntentService.sendWakefulWork(context, intent);
            }
        }
    }

    public long getMaxAge() {
        return (INTERVAL_MILIS + AlarmManager.INTERVAL_FIFTEEN_MINUTES);
    }
}