package com.github.axet.bookreader.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.github.axet.androidlibrary.crypto.MD5;
import com.github.axet.androidlibrary.widgets.WebViewCustom;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;

public class LocalBooksCatalog extends BooksCatalog {
    public static String CATALOG_NAME = "localcatalog_";

    Storage storage;

    public LocalBooksCatalog(Context context) {
        storage = new Storage(context);
    }

    public LocalBooksCatalog(Context context, JSONObject o) {
        storage = new Storage(context);
        load(o);
    }

    public void load(Uri folder) {
        url = folder.toString();
        load();
    }

    @Override
    public void load(JSONObject json) {
        super.load(json);
        load();
    }

    void load() {
        Uri u = Uri.parse(url);
        String s = u.getScheme();
        if (Build.VERSION.SDK_INT >= 21 && s.equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver resolver = storage.getContext().getContentResolver();
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            resolver.takePersistableUriPermission(u, flags);
        }
    }

    public JSONObject save() {
        JSONObject o = super.save();
        try {
            o.put("type", LocalBooksCatalog.class.getSimpleName());
            return (JSONObject) WebViewCustom.toJSON(o);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getId() {
        return url;
    }

    public String getTitle() {
        Uri u = Uri.parse(url);
        return getDisplayName(u);
    }

    public String getPrefix() {
        return CATALOG_NAME + MD5.digest(url) + "_";
    }

    public File getFile(String name) {
        return new File(storage.getCache(), getPrefix() +  name);
    }

    public void delete() {
        File cache = storage.getCache();
        final String prefix = getPrefix();
        File[] ff = cache.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(prefix);
            }
        });
        if (ff == null)
            return;
        for (File f : ff)
            FileUtils.deleteQuietly(f);
    }

    public String getDisplayName(Uri u) {
        String s = u.getScheme();
        if (s.equals(ContentResolver.SCHEME_CONTENT))
            return Storage.getDisplayName(storage.getContext(), u);
        else
            return ".../" + u.getLastPathSegment();
    }
}
