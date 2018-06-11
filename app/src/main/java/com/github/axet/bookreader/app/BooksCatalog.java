package com.github.axet.bookreader.app;

import com.github.axet.androidlibrary.widgets.WebViewCustom;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class BooksCatalog {
    public Long last;
    public String url;

    public BooksCatalog() {
    }

    public void load(JSONObject json) {
        try {
            Map<String, Object> map = WebViewCustom.toMap(json);
            last = (Long) map.get("last");
            url = (String) map.get("url");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject save() {
        try {
            JSONObject o = new JSONObject();
            o.put("last", last);
            o.put("url", url);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getId() {
        return url;
    }

    public String getTitle() {
        return null;
    }
}
