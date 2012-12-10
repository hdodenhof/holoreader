package de.hdodenhof.holoreader.gcm;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.gson.Gson;

import de.hdodenhof.holoreader.provider.RSSContentProvider;
import de.hdodenhof.holoreader.provider.SQLiteHelper.FeedDAO;

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
        // TODO
        GCMServerUtilities.registerOnServer("henning.dodenhof@gmail.com", registrationId);
        // TODO use local broadcast manager
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_REGISTERED);
        sendBroadcast(broadcastIntent);
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
            contentResolver.insert(RSSContentProvider.URI_FEEDS, contentValues);
        }
    }

}
