package com.github.axet.bookreader.app;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.github.axet.androidlibrary.net.HttpClient;
import com.github.axet.androidlibrary.widgets.WebViewCustom;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

public class BooksCatalog {
    public Long last;
    public String url;
    public Map<String, Object> map = new TreeMap<>();
    public Map<String, String> home;
    public Map<String, String> tops;

    public BooksCatalog(String json) {
        load(json);
    }

    public BooksCatalog(Context context, Uri u) {
        load(context, u);
    }

    public void load(String json) {
        try {
            JSONObject o = new JSONObject(json);
            Map<String, Object> map = WebViewCustom.toMap(o);
            last = (Long) map.get("last");
            url = (String) map.get("url");
            this.map = (Map<String, Object>) map.get("map");
            load();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void load(Context context, Uri uri) {
        String json = null;
        String s = uri.getScheme();
        try {
            if (s.equals(ContentResolver.SCHEME_CONTENT)) {
                ContentResolver resolver = context.getContentResolver();
                InputStream is = resolver.openInputStream(uri);
                json = IOUtils.toString(is, Charset.defaultCharset());
                is.close();
            } else if (s.startsWith("http")) {
                HttpClient client = new HttpClient();
                HttpClient.DownloadResponse w = client.getResponse(null, uri.toString());
                if (w.getError() != null)
                    throw new RuntimeException(w.getError() + ": " + uri);
                InputStream is = new BufferedInputStream(w.getInputStream());
                json = IOUtils.toString(is, Charset.defaultCharset());
                is.close();
            } else if (s.startsWith(ContentResolver.SCHEME_FILE)) {
                File f = new File(uri.getPath());
                FileInputStream is = new FileInputStream(f);
                json = IOUtils.toString(is, Charset.defaultCharset());
                is.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            JSONObject o = new JSONObject(json);
            map = WebViewCustom.toMap(o);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        load();
    }

    void load() {
        home = (Map<String, String>) map.get("home");
        tops = (Map<String, String>) map.get("tops");
    }

    public JSONObject save() {
        try {
            JSONObject o = new JSONObject();
            o.put("last", last);
            o.put("url", url);
            o.put("map", WebViewCustom.toJSON(map));
            return (JSONObject) WebViewCustom.toJSON(o);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getId() {
        return url;
    }

    public String getTitle() {
        return (String) map.get("name");
    }
}
