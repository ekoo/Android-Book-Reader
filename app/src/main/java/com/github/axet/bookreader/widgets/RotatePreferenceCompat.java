package com.github.axet.bookreader.widgets;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.util.AttributeSet;
import android.util.Log;

import com.github.axet.bookreader.R;

public class RotatePreferenceCompat extends SwitchPreferenceCompat {

    public static final String TAG = RotatePreferenceCompat.class.getSimpleName();

    public static boolean PHONES_ONLY = true; // hide option for tablets

    public static void onResume(Activity a, String key) {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(a);
        boolean user = shared.getBoolean(key, false);
        boolean system = false;
        try {
            system = Settings.System.getInt(a.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 1;
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "Unable to read settings", e);
        }
        boolean rotate;
        if (PHONES_ONLY && a.getResources().getBoolean(R.bool.isTablet)) { // tables has no user option to disable rotate
            rotate = system;
        } else {
            rotate = user && system;
        }
        if (rotate)
            a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        else
            a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @TargetApi(21)
    public RotatePreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        create();
    }

    @TargetApi(21)
    public RotatePreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create();
    }

    public RotatePreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        create();
    }

    public RotatePreferenceCompat(Context context) {
        super(context);
        create();
    }

    public void create() {
    }

    public void onCreate() {
        if (PHONES_ONLY && getContext().getResources().getBoolean(R.bool.isTablet)) {
            setVisible(false);
        }
    }
}
