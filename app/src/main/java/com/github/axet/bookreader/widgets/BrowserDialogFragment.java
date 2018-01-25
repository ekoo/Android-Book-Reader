package com.github.axet.bookreader.widgets;

import android.os.Bundle;
import android.webkit.WebView;

import com.github.axet.androidlibrary.net.HttpClient;
import com.github.axet.bookreader.activities.MainActivity;

public class BrowserDialogFragment extends com.github.axet.androidlibrary.widgets.BrowserDialogFragment {

    public static BrowserDialogFragment create(String url) {
        BrowserDialogFragment f = new BrowserDialogFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        f.setArguments(args);
        return f;
    }

    @Override
    public HttpClient createHttpClient() {
        HttpClient hc =  super.createHttpClient();
        hc.getCookieStore();
        return hc;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public boolean onDownloadStart(String base, String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        return super.onDownloadStart(base, url, userAgent, contentDisposition, mimetype, contentLength);
    }

    @Override
    public void onErrorMessage(String msg) {
        ((MainActivity) getActivity()).Post(msg);
    }

}
