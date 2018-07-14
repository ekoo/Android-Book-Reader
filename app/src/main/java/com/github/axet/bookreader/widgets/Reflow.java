package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.SparseArray;

import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.bookreader.app.MainApplication;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.k2pdfopt.K2PdfOpt;

import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Reflow {
    public K2PdfOpt k2;
    public int current = 0; // current view position
    public int page = 0; // document page
    int w;
    int h;
    int rw;
    Context context;
    public Bitmap bm; // source bm, in case or errors, recycled otherwise
    public Storage.RecentInfo rinfo;
    FBReaderView.CustomView custom;
    public Info info;

    public class Info {
        public Rect bm; // source bitmap size
        public Rect margin; // page margins
        public SparseArray<Map<Rect, Rect>> src = new SparseArray<>();
        public SparseArray<Map<Rect, Rect>> dst = new SparseArray<>();

        public Info(Bitmap bm) {
            this.bm = new Rect(0, 0, bm.getWidth(), bm.getHeight());
            margin = new Rect(getLeftMargin(), 0, getRightMargin(), 0);
            for (int i = 0; i < count(); i++) {
                Map<Rect, Rect> s = k2.getRectMaps(i);
                Map<Rect, Rect> d = new HashMap<>();
                for (Rect k : s.keySet()) {
                    Rect v = s.get(k);
                    d.put(v, k);
                }
                src.put(i, s);
                dst.put(i, d);
            }
        }

        public Bitmap drawSrc(PluginView pluginview, int page, Rect r) {
            Bitmap bm = drawSrc(pluginview, page);
            Canvas c = new Canvas(bm);
            Paint paint = new Paint();
            paint.setColor(Color.MAGENTA);
            paint.setStyle(Paint.Style.STROKE);
            int dp1 = ThemeUtils.dp2px(context, 1);
            paint.setStrokeWidth(dp1);
            c.drawRect(r, paint);
            return bm;
        }

        public Bitmap drawSrc(PluginView pluginview, int page, Point p) {
            Bitmap bm = drawSrc(pluginview, page);
            Canvas c = new Canvas(bm);
            Paint paint = new Paint();
            paint.setColor(Color.MAGENTA);
            paint.setStyle(Paint.Style.STROKE);
            int dp1 = ThemeUtils.dp2px(context, 2);
            paint.setStrokeWidth(dp1);
            c.drawPoint(p.x, p.y, paint);
            return bm;
        }

        public Bitmap drawSrc(PluginView pluginview, int page) {
            Bitmap b = pluginview.render(w, h, Reflow.this.page);
            Canvas canvas = new Canvas(b);
            draw(canvas, src.get(page).keySet());
            return b;
        }

        public Bitmap drawDst(int page, Rect r) {
            Bitmap bm = drawDst(page);
            Canvas c = new Canvas(bm);
            Paint paint = new Paint();
            paint.setColor(Color.MAGENTA);
            paint.setStyle(Paint.Style.STROKE);
            int dp1 = ThemeUtils.dp2px(context, 1);
            paint.setStrokeWidth(dp1);
            c.drawRect(r, paint);
            return bm;
        }

        public Bitmap drawDst(int page, Point p) {
            Bitmap bm = drawDst(page);
            Canvas c = new Canvas(bm);
            Paint paint = new Paint();
            paint.setColor(Color.MAGENTA);
            paint.setStyle(Paint.Style.STROKE);
            int dp1 = ThemeUtils.dp2px(context, 2);
            paint.setStrokeWidth(dp1);
            c.drawPoint(p.x, p.y, paint);
            return bm;
        }

        public Bitmap drawDst(int page) {
            Bitmap b = render(page);
            Canvas canvas = new Canvas(b);
            draw(canvas, dst.get(page).keySet());
            return b;
        }

        public void draw(Canvas canvas, Set<Rect> keys) {
            Rect[] kk = keys.toArray(new Rect[0]);
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            int dp1 = ThemeUtils.dp2px(context, 1);
            paint.setStrokeWidth(dp1);
            Paint text = new Paint();
            for (int i = 0; i < kk.length; i++) {
                Rect k = kk[i];
                canvas.drawRect(k, paint);

                String t = "" + i;
                text.setColor(Color.RED);

                int size = dp1;
                Rect bounds = new Rect();
                do {
                    text.setTextSize(size);
                    text.getTextBounds(t, 0, t.length(), bounds);
                    size++;
                } while (bounds.height() < (k.height()));

                float m = text.
                        measureText(t);
                canvas.drawText(t, k.centerX() - m / 2, k.top + k.height(), text);
            }
        }
    }

    public Reflow(Context context, int w, int h, int page, FBReaderView.CustomView custom, Storage.RecentInfo rinfo) {
        this.context = context;
        this.page = page;
        this.rinfo = rinfo;
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
            if (rinfo.fontsize != null)
                old = rinfo.fontsize / 100f;
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
        info = new Reflow.Info(bm);
    }

    public void load(Bitmap bm, int page, int current) {
        if (this.bm != null)
            this.bm.recycle();
        this.bm = bm;
        this.page = page;
        this.current = current;
        k2.load(bm);
        info = new Reflow.Info(bm);
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
