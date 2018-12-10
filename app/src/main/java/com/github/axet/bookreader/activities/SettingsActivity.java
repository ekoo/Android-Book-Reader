package com.github.axet.bookreader.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

import com.github.axet.androidlibrary.widgets.AppCompatSettingsThemeActivity;
import com.github.axet.androidlibrary.widgets.StoragePathPreferenceCompat;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.app.BookApplication;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.widgets.RotatePreferenceCompat;

public class SettingsActivity extends AppCompatSettingsThemeActivity {

    public static final int RESULT_STORAGE = 1;

    Storage storage;

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    public int getAppTheme() {
        return BookApplication.getTheme(this, R.style.AppThemeLight, R.style.AppThemeDark);
    }

    @Override
    public String getAppThemeKey() {
        return BookApplication.PREFERENCE_THEME;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = new Storage(this);
        RotatePreferenceCompat.onCreate(this, BookApplication.PREFERENCE_ROTATE);
        setupActionBar();
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);
        if (key.equals(BookApplication.PREFERENCE_STORAGE))
            storage.migrateLocalStorageDialog(this);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(BookApplication.PREFERENCE_SCREENLOCK));
            bindPreferenceSummaryToValue(findPreference(BookApplication.PREFERENCE_THEME));
            bindPreferenceSummaryToValue(findPreference(BookApplication.PREFERENCE_VIEW_MODE));

            StoragePathPreferenceCompat s = (StoragePathPreferenceCompat) findPreference(BookApplication.PREFERENCE_STORAGE);
            s.setStorage(new Storage(getContext()));
            s.setPermissionsDialog(this, Storage.PERMISSIONS_RW, RESULT_STORAGE);
            if (Build.VERSION.SDK_INT >= 21)
                s.setStorageAccessFramework(this, RESULT_STORAGE);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        }

        @Override
        public void onResume() {
            super.onResume();
            RotatePreferenceCompat r = (RotatePreferenceCompat) findPreference(BookApplication.PREFERENCE_ROTATE);
            r.onResume();
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            StoragePathPreferenceCompat s = (StoragePathPreferenceCompat) findPreference(BookApplication.PREFERENCE_STORAGE);
            switch (requestCode) {
                case RESULT_STORAGE:
                    s.onRequestPermissionsResult(permissions, grantResults);
                    break;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            StoragePathPreferenceCompat s = (StoragePathPreferenceCompat) findPreference(BookApplication.PREFERENCE_STORAGE);
            switch (requestCode) {
                case RESULT_STORAGE:
                    s.onActivityResult(resultCode, data);
                    break;
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                Activity a = getActivity();
                a.finish();
                startActivity(new Intent(getActivity(), MainActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
