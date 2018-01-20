package com.github.axet.bookreader.app;

import android.support.multidex.MultiDexApplication;

public class MainApplication extends MultiDexApplication {

    public static String PREFERENCE_THEME = "theme";
    public static String PREFERENCE_CATALOGS = "catalogs";

    @Override
    public void onCreate() {
        super.onCreate();

        Storage.getApp(this); // init context
    }

}
