package com.github.axet.bookreader.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;

import com.github.axet.androidlibrary.net.HttpClient;
import com.github.axet.androidlibrary.widgets.WebViewCustom;
import com.github.axet.bookreader.R;

import org.geometerplus.android.fbreader.network.auth.AndroidNetworkContext;
import org.geometerplus.fbreader.network.INetworkLink;
import org.geometerplus.fbreader.network.NetworkLibrary;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BooksCatalogs {
    public Context context;
    public NetworkLibrary nlib;
    public ArrayList<BooksCatalog> list = new ArrayList<>();

    // disable broken, closed, or authorization only repos without free books / or open links
    public static List<String> disabledIds = Arrays.asList(
            "http://data.fbreader.org/catalogs/litres2/index.php5", // authorization
            "http://www.freebookshub.com/feed/", // fake paid links
            "http://ebooks.qumran.org/opds/?lang=en", // timeout
            "http://ebooks.qumran.org/opds/?lang=de", // timeout
            "http://www.epubbud.com/feeds/catalog.atom", // ePub Bud has decided to wind down
            "http://www.shucang.org/s/index.php" // timeout
    );

    public static List<String> libAllIds(NetworkLibrary nlib) {
        List<String> all = nlib.allIds();
        for (String id : disabledIds) {
            nlib.setLinkActive(id, false);
            all.remove(id);
        }
        return all;
    }

    public static NetworkLibrary getLib(final Context context) {
        NetworkLibrary nlib = NetworkLibrary.Instance(new Storage.Info(context));
        if (!nlib.isInitialized()) {
            try {
                nlib.initialize(new NetworkContext(context));
            } catch (ZLNetworkException e) {
                throw new RuntimeException(e);
            }
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
            String json = shared.getString(MainApplication.PREFERENCE_CATALOGS, null);
            if (json != null && !json.isEmpty()) {
                try {
                    List<String> all = nlib.allIds();
                    for (String id : all)
                        nlib.setLinkActive(id, false);
                    JSONArray a = new JSONArray(json);
                    for (int i = 0; i < a.length(); i++) {
                        String id = a.getString(i);
                        if (!disabledIds.contains(id))
                            nlib.setLinkActive(id, true);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return nlib;
    }

    public static class NetworkContext extends AndroidNetworkContext {
        Context context;

        public NetworkContext(Context context) {
            this.context = context;
        }

        @Override
        protected Context getContext() {
            return context;
        }

        @Override
        protected Map<String, String> authenticateWeb(URI uri, String realm, String authUrl, String completeUrl, String verificationUrl) {
            return null;
        }

        @Override
        protected void perform(ZLNetworkRequest request, int socketTimeout, int connectionTimeout) throws ZLNetworkException {
            super.perform(request, HttpClient.CONNECTION_TIMEOUT, HttpClient.CONNECTION_TIMEOUT);
        }
    }

    public BooksCatalogs(Context context) {
        this.context = context;
        load();
    }

    public void openSettings() {
        final List<String> all = libAllIds(nlib);
        List<String> active = nlib.activeIds();

        final String[] nn = new String[all.size()];
        final boolean[] bb = new boolean[all.size()];
        final INetworkLink[] nl = new INetworkLink[all.size()];

        for (int i = 0; i < all.size(); i++) {
            String id = all.get(i);
            INetworkLink link = nlib.getLinkByUrl(id);
            nn[i] = link.getTitle();
            bb[i] = active.contains(id);
            nl[i] = link;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.add_catalog);
        builder.setMultiChoiceItems(nn, bb, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                bb[which] = isChecked;
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                JSONArray a = new JSONArray();
                for (int i = 0; i < all.size(); i++) {
                    nlib.setLinkActive(all.get(i), bb[i]);
                    if (bb[i])
                        a.put(all.get(i));
                }
                SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor edit = shared.edit();
                edit.putString(MainApplication.PREFERENCE_CATALOGS, a.toString());
                edit.commit();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ;
            }
        });
        builder.show();
    }

    public int getCount() {
        return list.size();
    }

    public BooksCatalog getCatalog(int i) {
        return list.get(i);
    }

    public BooksCatalog load(Uri uri) {
        BooksCatalog ct = new BooksCatalog();
        String s = uri.getScheme();
        try {
            if (s.equals(ContentResolver.SCHEME_CONTENT)) {
                ContentResolver resolver = context.getContentResolver();
                InputStream is = resolver.openInputStream(uri);
                ct.load(is);
                is.close();
            } else if (s.startsWith(WebViewCustom.SCHEME_HTTP)) {
                if (Build.VERSION.SDK_INT < 11) {
                    HttpURLConnection conn = HttpClient.openConnection(uri);
                    InputStream is = new BufferedInputStream(conn.getInputStream());
                    ct.load(is);
                    is.close();
                } else {
                    HttpClient client = new HttpClient();
                    HttpClient.DownloadResponse w = client.getResponse(null, uri.toString());
                    if (w.getError() != null)
                        throw new RuntimeException(w.getError() + ": " + uri);
                    InputStream is = new BufferedInputStream(w.getInputStream());
                    ct.load(is);
                    is.close();
                }
            } else if (s.startsWith(ContentResolver.SCHEME_FILE)) {
                File f = new File(uri.getPath());
                FileInputStream is = new FileInputStream(f);
                ct.load(is);
                is.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ct.url = uri.toString();
        ct.last = System.currentTimeMillis();
        BooksCatalog ct2 = find(uri.toString());
        if (ct2 != null)
            list.set(list.indexOf(ct2), ct);
        else
            list.add(ct);
        return ct;
    }

    public void load() {
        list.clear();
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        int count = shared.getInt(MainApplication.PREFERENCE_CATALOGS_PREFIX + MainApplication.PREFERENCE_CATALOGS_COUNT, -1);
        for (int i = 0; i < count; i++) {
            String json = shared.getString(MainApplication.PREFERENCE_CATALOGS_PREFIX + i, "");
            BooksCatalog ct = new BooksCatalog(json);
            if (ct.map.isEmpty())
                continue;
            list.add(ct);
        }
    }

    public void save() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = shared.edit();
        edit.putInt(MainApplication.PREFERENCE_CATALOGS_PREFIX + MainApplication.PREFERENCE_CATALOGS_COUNT, list.size());
        for (int i = 0; i < list.size(); i++) {
            JSONObject o = list.get(i).save();
            edit.putString(MainApplication.PREFERENCE_CATALOGS_PREFIX + i, o.toString());
        }
        edit.commit();
    }

    public BooksCatalog find(String u) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(u)) {
                return list.get(i);
            }
        }
        return null;
    }

    public void delete(String id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                list.remove(i);
            }
        }
    }
}
