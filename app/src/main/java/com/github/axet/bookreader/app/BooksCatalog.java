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

    public BooksCatalog() {
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

    public void load(InputStream is) {
        String json = null;
        try {
            json = IOUtils.toString(is, Charset.defaultCharset());
            JSONObject o = new JSONObject(json);
            map = WebViewCustom.toMap(o);
        } catch (JSONException | IOException e) {
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
