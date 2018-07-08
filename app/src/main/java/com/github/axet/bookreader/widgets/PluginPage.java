package com.github.axet.bookreader.widgets;

import android.graphics.Rect;

import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;

public abstract class PluginPage {
    public int pageNumber;
    public int pageOffset; // pageBox sizes
    public PluginRect pageBox; // pageBox sizes
    public int w; // display w
    public int h; // display h
    public double hh; // pageBox sizes, visible height
    public double ratio;
    public int pageStep; // pageBox sizes, page step size (fullscreen height == pageStep + pageOverlap)
    public int pageOverlap; // pageBox sizes, page overlap size (fullscreen height == pageStep + pageOverlap)
    public int dpi; // pageBox dpi, set manually

    public PluginPage() {
    }

    public PluginPage(PluginPage r) {
        w = r.w;
        h = r.h;
        hh = r.hh;
        ratio = r.ratio;
        pageNumber = r.pageNumber;
        pageOffset = r.pageOffset;
        if (r.pageBox != null)
            pageBox = new PluginRect(r.pageBox);
        pageStep = r.pageStep;
        pageOverlap = r.pageOverlap;
    }

    public PluginPage(PluginPage r, ZLViewEnums.PageIndex index) {
        this(r);
        load(index);
    }

    public void renderPage() {
        ratio = pageBox.w / (double) w;
        hh = h * ratio;

        pageOverlap = (int) (hh * FBReaderView.PAGE_OVERLAP_PERCENTS / 100);
        pageStep = (int) (hh - pageOverlap); // -5% or lowest base line
    }

    public void load(ZLViewEnums.PageIndex index) {
        switch (index) {
            case next:
                next();
                break;
            case previous:
                prev();
                break;
        }
    }

    public abstract void load();

    public abstract int getPagesCount();

    public boolean next() {
        int pageOffset = this.pageOffset + pageStep;
        int tail = pageBox.h - pageOffset;
        if (pageOffset >= pageBox.h || tail <= pageOverlap) {
            int pageNumber = this.pageNumber + 1;
            if (pageNumber >= getPagesCount())
                return false;
            this.pageOffset = 0;
            this.pageNumber = pageNumber;
            load();
            renderPage();
            return true;
        }
        this.pageOffset = pageOffset;
        return true;
    }

    public boolean prev() {
        int pageOffset = this.pageOffset - pageStep;
        if (this.pageOffset > 0 && pageOffset < 0) { // happens only on screen rotate
            this.pageOffset = pageOffset; // sync to top = 0 or keep negative offset
            return true;
        } else if (pageOffset < 0) {
            int pageNumber = this.pageNumber - 1;
            if (pageNumber < 0)
                return false;
            this.pageNumber = pageNumber;
            load(); // load pageBox
            renderPage(); // calculate pageStep
            int tail = pageBox.h % pageStep;
            pageOffset = pageBox.h - tail;
            if (tail <= pageOverlap)
                pageOffset = pageOffset - pageStep; // skip tail
            this.pageOffset = pageOffset;
            return true;
        }
        this.pageOffset = pageOffset;
        return true;
    }

    public void scale(int w, int h) {
        double ratio = w / (double) pageBox.w;
        this.hh *= ratio;
        this.ratio *= ratio;
        pageBox.w = w;
        pageBox.h = (int) (pageBox.h * ratio);
        pageOffset = (int) (pageOffset * ratio);
        dpi = (int) (dpi * ratio);
    }

    public RenderRect renderRect() {
        RenderRect render = new RenderRect(); // render region

        render.x = 0;
        render.w = pageBox.w;

        if (pageOffset < 0) { // show empty space at beginig
            int tail = (int) (pageBox.h - pageOffset - hh); // tail to cut from the bottom
            if (tail < 0) {
                render.h = pageBox.h;
                render.y = 0;
            } else {
                render.h = pageBox.h - tail;
                render.y = tail;
            }
            render.dst = new Rect(0, (int) (-pageOffset / ratio), w, h);
        } else if (pageOffset == 0 && hh > pageBox.h) {  // show middle vertically
            int t = (int) ((hh - pageBox.h) / ratio / 2);
            render.h = pageBox.h;
            render.dst = new Rect(0, t, w, h - t);
        } else {
            render.h = (int) hh;
            render.y = pageBox.h - render.h - pageOffset - 1;
            if (render.y < 0) {
                render.h += render.y;
                h += render.y / ratio; // convert to display sizes
                render.y = 0;
            }
            render.dst = new Rect(0, 0, w, h);
        }

        render.src = new Rect(0, 0, render.w, render.h);

        return render;
    }

    public boolean equals(int n, int o) {
        return pageNumber == n && pageOffset == o;
    }

    public void load(ZLTextPosition p) {
        if (p == null) {
            load(0, 0);
        } else {
            load(p.getParagraphIndex(), p.getElementIndex());
        }
    }

    public void load(int n, int o) {
        pageNumber = n;
        pageOffset = o;
        load();
    }

    public void updatePage(PluginPage r) {
        w = r.w;
        h = r.h;
        ratio = r.ratio;
        hh = r.hh;
        pageStep = r.pageStep;
        pageOverlap = r.pageOverlap;
    }
}
