package com.github.axet.bookreader.widgets;

import android.os.Bundle;

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
    public void onErrorMessage(String msg) {
        ((MainActivity) getActivity()).Post(msg);
    }

}
