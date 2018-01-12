package com.github.axet.bookreader.widgets;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.github.axet.bookreader.R;

import org.geometerplus.android.fbreader.network.NetworkBookInfoActivity;

public class BookDialog extends DialogFragment {

    public NetworkBookInfoActivity a = new NetworkBookInfoActivity() {
        @Override
        public WindowManager getWindowManager() {
            return getActivity().getWindowManager();
        }

        @Override
        public Context getApplicationContext() {
            return BookDialog.this.getContext();
        }

        @Override
        public View findViewById(int id) {
            return v.findViewById(id);
        }

        @NonNull
        @Override
        public LayoutInflater getLayoutInflater() {
            return LayoutInflater.from(getApplicationContext());
        }
    };

    View v;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog d = new AlertDialog.Builder(getActivity())
                .setTitle(a.myBook.Title)
                .setNeutralButton(getContext().getString(com.github.axet.androidlibrary.R.string.close),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                )
                .setView(createView(LayoutInflater.from(getContext()), null, savedInstanceState))
                .create();
        return d;
    }

    @Nullable
    @Override
    public View getView() {
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return v;
    }

    public View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.network_book, container);

        a.setupDescription();
        // a.setupExtraLinks();
        a.setupInfo();
        a.setupCover();
        // a.setupButtons();

        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}
