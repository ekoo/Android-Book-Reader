package com.github.axet.bookreader.app;

import android.app.Application;
import android.webkit.MimeTypeMap;

import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;

public class MainApplication extends ZLAndroidApplication {

    public static final String CONTENTTYPE_OCTETSTREAM = "application/octet-stream";
    public static final String CONTENTTYPE_OPUS = "audio/opus";
    public static final String CONTENTTYPE_OGG = "audio/ogg";
    public static final String CONTENTTYPE_FB2 = "application/x-fictionbook";

    public static String getTypeByName(String fileName) {
        String ext = Storage.getExt(fileName);
        if (ext == null || ext.isEmpty()) {
            return CONTENTTYPE_OCTETSTREAM; // replace 'null'
        }
        ext = ext.toLowerCase();
        switch (ext) {
            case "opus":
                return CONTENTTYPE_OPUS; // android missing
            case "ogg":
                return CONTENTTYPE_OGG; // replace 'application/ogg'
            case "fb2":
                return CONTENTTYPE_FB2;
        }
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (type == null || type.isEmpty())
            return CONTENTTYPE_OCTETSTREAM;
        return type;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
