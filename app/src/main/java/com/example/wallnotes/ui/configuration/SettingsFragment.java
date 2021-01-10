package com.example.wallnotes.ui.configuration;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import androidx.preference.PreferenceFragmentCompat;
import com.example.wallnotes.R;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

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
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(isAdded()){
            if(key.equals("theme")){
                Intent intent = requireActivity().getIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                requireActivity().finish();
                startActivity(intent);
            }
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
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
}