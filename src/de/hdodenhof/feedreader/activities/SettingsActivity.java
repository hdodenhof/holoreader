package de.hdodenhof.feedreader.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import de.hdodenhof.feedreader.fragments.SettingsFragment;

@SuppressLint("NewApi")
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

}
