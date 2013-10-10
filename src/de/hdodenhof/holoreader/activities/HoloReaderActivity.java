package de.hdodenhof.holoreader.activities;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.hdodenhof.holoreader.services.RefreshFeedService;

public class HoloReaderActivity extends SherlockFragmentActivity {

    @SuppressWarnings("unused")
    private static final String TAG = HoloReaderActivity.class.getSimpleName();

    @Override
    protected void onPause() {
        long waitMillis;
        long millisSinceBoot = SystemClock.elapsedRealtime();
        long lastRefreshed = PreferenceManager.getDefaultSharedPreferences(this).getLong("lastRefreshed", millisSinceBoot);
        if (lastRefreshed + RefreshFeedService.INTERVAL_MILLIS < millisSinceBoot) {
            waitMillis = RefreshFeedService.WAIT_MILLIS;
        } else {
            waitMillis = (lastRefreshed + RefreshFeedService.INTERVAL_MILLIS - millisSinceBoot);
        }
        RefreshFeedService.scheduleRefresh(this, waitMillis);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        RefreshFeedService.cancelPendingRefresh(this);
    }

    protected int getVersion() {
        int version = 0;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
        }
        return version;
    }

}
