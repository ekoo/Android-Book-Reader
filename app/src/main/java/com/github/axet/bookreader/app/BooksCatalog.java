package com.github.axet.bookreader.app;

import android.net.Uri;

import com.github.axet.androidlibrary.widgets.WebViewCustom;

import org.geometerplus.fbreader.network.INetworkLink;
import org.geometerplus.fbreader.network.opds.OPDSNetworkLink;
import org.geometerplus.fbreader.network.opds.OPDSPredefinedNetworkLink;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BooksCatalog {
    public long last;
    public Uri url;
    public Map<String, Object> map = new TreeMap<>();
    public Map<String, String> home;
    public Map<String, String> tops;

    public BooksCatalog(String json) {
        load(json);
    }

    public void load(String json) {
        try {
            JSONObject o = new JSONObject(json);
            map = WebViewCustom.toMap(o);
            home = (Map<String, String>) map.get("home");
            tops = (Map<String, String>) map.get("tops");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject save() {
        try {
            return (JSONObject) WebViewCustom.toJSON(map);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getId() {
        return home.get("get");
    }

    public String getTitle() {
        return (String) map.get("name");
    }
}
