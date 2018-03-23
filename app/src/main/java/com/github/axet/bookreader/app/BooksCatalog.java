package com.github.axet.bookreader.app;

import com.github.axet.androidlibrary.widgets.WebViewCustom;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.io.output.WriterOutputStream;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

public class BooksCatalog {
    public Long last;
    public String url;
    public String cookies;
    public Map<String, Object> map = new TreeMap<>();
    public Map<String, String> home;
    public Map<String, Object> opds;
    public Map<String, String> tops;

    public static class FileJSON extends Storage.FileTxt {

        public FileJSON() {
            super("json");
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            if (done && !detected)
                return;
            int end = off + len;
            for (int i = off; i < end; i++) {
                int c = buf[i];
                if (Character.isWhitespace(c))
                    continue;
                done = true;
                detected = (c == '{' || c == '['); // first symbol after spaces ends
                return;
            }
        }
    }

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
            StringBuilderWriter sw = new StringBuilderWriter();
            WriterOutputStream os = new WriterOutputStream(sw, Charset.defaultCharset());
            FileJSON d = new FileJSON();
            byte[] buf = new byte[Storage.BUF_SIZE];
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
            json = sw.toString();
            JSONObject o = new JSONObject(json);
            map = WebViewCustom.toMap(o);
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
        load();
    }

    void load() {
        home = (Map<String, String>) map.get("home");
        if (map.get("opds") instanceof Map)
            opds = (Map<String, Object>) map.get("opds");
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

    public void setCookies(String s) {
        cookies = s;
    }
}
