package de.hdodenhof.holoreader.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.activities.HomeActivity;
import de.hdodenhof.holoreader.provider.RSSContentProvider;
import de.hdodenhof.holoreader.provider.SQLiteHelper.FeedDAO;
import de.hdodenhof.holoreader.services.RefreshFeedService;

public class GCMIntentService extends GCMBaseIntentService {

    @SuppressWarnings("unused")
    private static final String TAG = GCMIntentService.class.getName();

    public static final String BROADCAST_REGISTERED = "de.hdodenhof.holoreader.GCM_REGISTERED";
    public static final String SENDER_ID = "";

    private static final String MESSAGETYPE_ADDFEED = "addfeed";

    public GCMIntentService() {
        super(SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.v(TAG, "onRegistered");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String eMail = prefs.getString("eMail", null);

        if (eMail != null) {
            boolean success = GCMServerUtilities.registerOnServer(eMail, registrationId);
            if (success) {
                GCMRegistrar.setRegisteredOnServer(this, true);
            }
            // TODO use local broadcast manager
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(BROADCAST_REGISTERED);
            sendBroadcast(broadcastIntent);
        }
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.v(TAG, "onUnregistered");
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.v(TAG, "onMessage");

        String messageType = intent.getStringExtra("type");
        if (messageType.equals(MESSAGETYPE_ADDFEED)) {
            Log.v(TAG, "... handling addFeed message");
            handleAddFeedMessage(intent.getStringExtra("data"));
        }
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
        Log.v(TAG, "onDeletedMessages");
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.v(TAG, "onError, errorId: " + errorId);
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        Log.v(TAG, "onRecoverableError, errorId: " + errorId);
        return super.onRecoverableError(context, errorId);
    }

    private void handleAddFeedMessage(String data) {
        VOFeed[] feeds = new Gson().fromJson(data, VOFeed[].class);

        ContentResolver contentResolver = getContentResolver();

        for (VOFeed voFeed : feeds) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(FeedDAO.NAME, voFeed.getTitle());
            contentValues.put(FeedDAO.URL, voFeed.getUrl());

            Uri newFeed = contentResolver.insert(RSSContentProvider.URI_FEEDS, contentValues);
            int feedId = Integer.parseInt(newFeed.getLastPathSegment());

            Intent intent = new Intent(this, RefreshFeedService.class);
            intent.putExtra("feedid", feedId);

            WakefulIntentService.sendWakefulWork(this, intent);
        }

        // TODO proper flags
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
        nb.setContentTitle(getResources().getString(R.string.FeedsAddedViaPush));
        nb.setContentText(getResources().getQuantityString(R.plurals.numberOfFeedsReceived, feeds.length, feeds.length));
        nb.setSmallIcon(R.drawable.notification);
        nb.setContentIntent(contentIntent);

        Notification notification = nb.getNotification();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0x1, notification);
    }

}