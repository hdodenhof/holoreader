package de.hdodenhof.holoreader.gcm;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

    @SuppressWarnings("unused")
    private static final String TAG = GCMIntentService.class.getName();

    public static final String SENDER_ID = "";

    public GCMIntentService() {
        super(SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.v(TAG, "onRegistered");
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.v(TAG, "onUnregistered");
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.v(TAG, "onMessage");
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

}
