package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.SparseArray;

import com.github.axet.bookreader.app.MainApplication;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.k2pdfopt.K2PdfOpt;

import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;

import java.util.HashMap;
import java.util.Map;

public class Reflow {
    public K2PdfOpt k2;
    public int current = 0; // current view position
    public int page = 0; // document page
    int w;
    int h;
    int rw;
    Context context;
    public Bitmap bm; // source bm, in case or errors, recycled otherwise
    public Storage.RecentInfo info;
    FBReaderView.CustomView custom;

    public static class Info {
        public Rect bm; // source bitmap size
        public Rect margin; // page margins
        public SparseArray<Map<Rect, Rect>> src = new SparseArray<>();
        public SparseArray<Map<Rect, Rect>> dst = new SparseArray<>();

        public Info(Reflow reflow, Bitmap bm) {
            this.bm = new Rect(0, 0, bm.getWidth(), bm.getHeight());
            margin = new Rect(reflow.getLeftMargin(), 0, reflow.getRightMargin(), 0);
            for (int i = 0; i < reflow.count(); i++) {
                Map<Rect, Rect> s = reflow.k2.getRectMaps(i);
                Map<Rect, Rect> d = new HashMap<>();
                for (Rect k : s.keySet()) {
                    Rect v = s.get(k);
                    d.put(v, k);
                }
                src.put(i, s);
                dst.put(i, d);
            }
        }
    }

    public Reflow(Context context, int w, int h, int page, FBReaderView.CustomView custom, Storage.RecentInfo info) {
        this.context = context;
        this.page = page;
        this.info = info;
        this.custom = custom;
        reset(w, h);
    }

    public void reset() {
        w = 0;
        h = 0;
        if (k2 != null) {
            k2.close();
            k2 = null;
        }
    }

    public int getLeftMargin() {
        return custom.getLeftMargin();
    }

    public int getRightMargin() {
        return custom.getRightMargin();
    }

    public void reset(int w, int h) {
        if (this.w != w || this.h != h) {
            SharedPreferences shared = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
            Float old = shared.getFloat(MainApplication.PREFERENCE_FONTSIZE_REFLOW, MainApplication.PREFERENCE_FONTSIZE_REFLOW_DEFAULT);
            if (info.fontsize != null)
                old = info.fontsize / 100f;
            if (k2 != null) {
                old = k2.getFontSize();
                k2.close();
            }
            int rw = w - getLeftMargin() - getRightMargin();
            k2 = new K2PdfOpt();
            DisplayMetrics d = context.getResources().getDisplayMetrics();
            k2.create(rw, h, d.densityDpi);
            k2.setFontSize(old);
            this.w = w;
            this.h = h;
            this.rw = rw;
            this.current = 0; // size changed, reflow page can overflow total pages
        }
    }

    public void load(Bitmap bm) {
        if (this.bm != null)
            this.bm.recycle();
        this.bm = bm;
        current = 0;
        k2.load(bm);
    }

    public void load(Bitmap bm, int page, int current) {
        if (this.bm != null)
            this.bm.recycle();
        this.bm = bm;
        this.page = page;
        this.current = current;
        k2.load(bm);
    }

    public int count() {
        if (k2 == null)
            return -1;
        return k2.getCount();
    }

    public int emptyCount() {
        int c = count();
        if (c == 0)
            c = 1;
        return c;
    }

    public Bitmap render(int page) {
        return k2.renderPage(page);
    }

    public boolean canScroll(ZLViewEnums.PageIndex index) {
        switch (index) {
            case previous:
                return current > 0;
            case next:
                return current + 1 < count();
            default:
                return true; // current???
        }
    }

    public void onScrollingFinished(ZLViewEnums.PageIndex index) {
        switch (index) {
            case next:
                current++;
                break;
            case previous:
                current--;
                break;
        }
    }

    public void close() {
        if (k2 != null) {
            k2.close();
            k2 = null;
        }
        if (bm != null) {
            bm.recycle();
            bm = null;
        }
    }

}
