package com.github.axet.bookreader.app;

import com.github.axet.androidlibrary.app.FileTypeDetector;
import com.github.axet.androidlibrary.widgets.WebViewCustom;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.io.output.WriterOutputStream;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

public class NetworkBooksCatalog extends BooksCatalog {
    public String cookies;
    public Map<String, Object> map = new TreeMap<>();
    public Map<String, String> home;
    public Map<String, Object> opds;
    public Map<String, String> tops;

    public NetworkBooksCatalog(JSONObject json) {
        load(json);
    }

    public NetworkBooksCatalog() {
    }

    @SuppressWarnings("unchecked")
    public void load(JSONObject json) {
        super.load(json);
        try {
            Map<String, Object> map = WebViewCustom.toMap(json);
            this.map = (Map<String, Object>) map.get("map");
            load();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void load(InputStream is) {
        try {
            StringBuilderWriter sw = new StringBuilderWriter();
            WriterOutputStream os = new WriterOutputStream(sw, Charset.defaultCharset());
            FileTypeDetector.FileJSON d = new FileTypeDetector.FileJSON();
            byte[] buf = new byte[FileTypeDetector.BUF_SIZE];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
                if (!d.done) {
                    d.write(buf, 0, len);
                    if (d.done && !d.detected) {
                        throw new RuntimeException("Unsupported catalog format");
                    }
                }
            }
            os.close();
            String json = sw.toString();
            JSONObject o = new JSONObject(json);
            map = WebViewCustom.toMap(o);
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
        load();
    }

    @SuppressWarnings("unchecked")
    void load() {
        home = (Map<String, String>) map.get("home");
        if (map.get("opds") instanceof Map)
            opds = (Map<String, Object>) map.get("opds");
        tops = (Map<String, String>) map.get("tops");
    }

    public JSONObject save() {
        JSONObject o = super.save();
        try {
            o.put("type", NetworkBooksCatalog.class.getSimpleName());
            o.put("map", WebViewCustom.toJSON(map));
            return (JSONObject) WebViewCustom.toJSON(o);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getId() {
        return url;
    }

    @Override
    public String getTitle() {
        return (String) map.get("name");
    }

    public void setCookies(String s) {
        cookies = s;
    }
}
