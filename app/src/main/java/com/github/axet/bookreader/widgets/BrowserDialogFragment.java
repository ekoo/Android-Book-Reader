package com.github.axet.bookreader.widgets;

import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;

import com.github.axet.androidlibrary.net.HttpClient;
import com.github.axet.bookreader.activities.MainActivity;

import java.net.URL;

import cz.msebera.android.httpclient.impl.client.BasicCookieStore;

public class BrowserDialogFragment extends com.github.axet.androidlibrary.widgets.BrowserDialogFragment {

    public static BrowserDialogFragment create(String url) {
        BrowserDialogFragment f = new BrowserDialogFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        f.setArguments(args);
        return f;
    }

    public static BrowserDialogFragment createHtml(String base, String html) {
        BrowserDialogFragment f = new BrowserDialogFragment();
        Bundle args = new Bundle();
        args.putString("base", base);
        args.putString("html", html);
        f.setArguments(args);
        return f;
    }

    @Override
    public HttpClient createHttpClient() {
        HttpClient hc = super.createHttpClient();
        hc.setCookieStore(new BasicCookieStore()); // enable cookies
        return hc;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public boolean onDownloadStart(final String base, final String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        final MainActivity main = ((MainActivity) getActivity());
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                main.loadBook(Uri.parse(url), new Runnable() {
                    @Override
                    public void run() {
                        dismiss();
                    }
                });
            }
        });
        return true; // super.onDownloadStart(base, url, userAgent, contentDisposition, mimetype, contentLength);
    }

    @Override
    public void onErrorMessage(String msg) {
        ((MainActivity) getActivity()).Post(msg);
    }

}
