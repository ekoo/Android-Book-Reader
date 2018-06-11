package com.github.axet.bookreader.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.github.axet.androidlibrary.crypto.MD5;
import com.github.axet.androidlibrary.widgets.WebViewCustom;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.io.output.WriterOutputStream;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

public class LocalBooksCatalog extends BooksCatalog {
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
        return storage.getDisplayName(u);
    }

    public File getCache() {
        File f = new File(storage.getCache(), MD5.digest(url));
        if (!f.exists() && !f.mkdirs() && !f.exists())
            throw new RuntimeException("unable to write");
        return f;
    }

    public void delete() {
        FileUtils.deleteQuietly(getCache());
    }
}
