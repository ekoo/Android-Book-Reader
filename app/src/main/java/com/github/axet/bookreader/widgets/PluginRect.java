package com.github.axet.bookreader.widgets;

import android.graphics.Rect;

public class PluginRect {
    public int x; // lower left x
    public int y; // lower left y
    public int w; // x + w = upper right x
    public int h; // y + h = upper right y

    public PluginRect() {
    }

    public PluginRect(PluginRect r) {
        this.x = r.x;
        this.y = r.y;
        this.w = r.w;
        this.h = r.h;
    }

    public PluginRect(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public Rect toRect(int w, int h) {
        return new Rect(x, h - this.h - y, x + this.w, h - y);
    }
}
