package com.example.wallnotes.ui.configuration;

import android.os.Bundle;
import android.view.Menu;

import androidx.preference.PreferenceFragmentCompat;

import com.example.wallnotes.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        setHasOptionsMenu(true);
    }
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.change_layout).setVisible(false);
        menu.findItem(R.id.delete_notes).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }
}