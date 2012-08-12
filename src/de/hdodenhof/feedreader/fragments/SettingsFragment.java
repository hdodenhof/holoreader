package de.hdodenhof.feedreader.fragments;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import de.hdodenhof.feedreader.R;

@SuppressLint("NewApi")
public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            updatePreferenceSummary(getPreferenceScreen().getPreference(i));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreferenceSummary(findPreference(key));
    }

    private void updatePreferenceSummary(Preference preference) {
        if (preference.getKey().equals("pref_keep_read_articles_days") || preference.getKey().equals("pref_keep_unread_articles_days")) {
            EditTextPreference mEditTextPreference = (EditTextPreference) preference;
            preference.setSummary(getResources().getString(R.string.pref_delete_articles, mEditTextPreference.getText()));
        }
    }
}
