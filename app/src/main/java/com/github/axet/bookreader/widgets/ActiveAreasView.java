package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.axet.androidlibrary.widgets.ThemeUtils;

import org.geometerplus.fbreader.fbreader.TapZoneMap;
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions;

import java.util.HashMap;

public class ActiveAreasView extends FrameLayout {
    public static int PERC = 1000; // precision

    HashMap<String, Rect> maps = new HashMap<>();
    HashMap<String, ZoneView> views = new HashMap<>();

    HashMap<String, String> names = new HashMap<>();

    {
        names.put("menu", "Fullscreen");
        names.put("navigate", "Navigate");
        names.put("nextPage", "Next page");
        names.put("previousPage", "Previous page");
        names.put("brightness", "Brightness");
    }

    public class ZoneView extends FrameLayout {
        TextView text;
        GradientDrawable g;

        public ZoneView(@NonNull Context context) {
            super(context);
            text = new TextView(getContext());
            text.setTextColor(Color.WHITE);
            text.setTypeface(text.getTypeface(), Typeface.BOLD);
            addView(text, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            g = new GradientDrawable();
            g.setCornerRadius(ThemeUtils.dp2px(context, 20));
            g.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            g.setColor(0x22333333);
            setBackgroundDrawable(g);
            MarginLayoutParams lp = new MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            setLayoutParams(lp);
            ViewCompat.setAlpha(text, 0.7f);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public ActiveAreasView(Context context) {
        super(context);
    }

    public TapZoneMap getZoneMap(FBReaderView.FBReaderApp app) {
        final PageTurningOptions prefs = app.PageTurningOptions;
        String id = prefs.TapZoneMap.getValue();
        if ("".equals(id)) {
            id = prefs.Horizontal.getValue() ? "right_to_left" : "up";
        }
        TapZoneMap myZoneMap = null;
        if (myZoneMap == null || !id.equals(myZoneMap.Name)) {
            myZoneMap = TapZoneMap.zoneMap(id);
        }
        return myZoneMap;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        for (String k : maps.keySet()) {
            Rect r = maps.get(k);
            ZoneView v = views.get(k);
            int dp5 = ThemeUtils.dp2px(getContext(), 2);
            MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
            lp.leftMargin = w * r.left / PERC + dp5;
            lp.topMargin = h * r.top / PERC + dp5;
            lp.width = w * r.width() / PERC - dp5 * 2;
            lp.height = h * r.height() / PERC - dp5 * 2;
            if (v.text.getMeasuredWidth() > lp.width) {
                lp = (LayoutParams) v.text.getLayoutParams();
                lp.width = v.text.getMeasuredWidth();
                lp.height = v.text.getMeasuredHeight();
                ViewCompat.setRotation(v.text, 90);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    void substract(Rect r) {
        for (String s : maps.keySet()) {
            Rect z = maps.get(s);
            Rect a = new Rect(r);
            if (a.intersect(z)) {
                int ll = a.left - z.left;
                int rr = z.right - a.right;
                if (ll == 0 && rr == 0) {
                    ;
                } else if (ll < rr) {
                    z.left = a.right;
                } else {
                    z.right = a.left;
                }
                int tt = a.top - z.top;
                int bb = z.bottom - a.bottom;
                if (tt == 0 && bb == 0) {
                    ;
                } else if (tt < bb) {
                    z.top = a.bottom;
                } else {
                    z.bottom = a.top;
                }
            }
        }
    }

    public void create(FBReaderView.FBReaderApp app) {
        TapZoneMap zz = getZoneMap(app);
        int w = PERC / zz.getWidth();
        int h = PERC / zz.getHeight();
        for (int x = 0; x < zz.getWidth(); x++) {
            for (int y = 0; y < zz.getHeight(); y++) {
                String z = zz.getActionByZone(x, y, app.MiscOptions.EnableDoubleTap.getValue() ? TapZoneMap.Tap.singleNotDoubleTap : TapZoneMap.Tap.singleTap);
                if (!app.isActionEnabled(z))
                    continue;
                Rect r = maps.get(z);
                int xx = w * x; // x offset
                int yy = h * y; // y offset
                Rect c = new Rect(xx, yy, xx + w, yy + h);
                if (r == null) {
                    maps.put(z, c);
                } else {
                    r.union(c);
                }
            }
        }
        if (app.MiscOptions.AllowScreenBrightnessAdjustment.getValue()) {
            Rect r = new Rect(0, 0, PERC / 10, PERC);
            substract(r);
            maps.put("brightness", r);
        }
        for (String k : maps.keySet()) {
            ZoneView v = new ZoneView(getContext());
            v.text.setText(names.get(k));
            views.put(k, v);
            addView(v);
        }
        MarginLayoutParams lp = new MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        setLayoutParams(lp);
    }

}
