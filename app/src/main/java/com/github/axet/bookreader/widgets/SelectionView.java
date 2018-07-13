package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.github.axet.bookreader.app.PDFPlugin;

import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.view.SelectionCursor;

import java.util.ArrayList;

public class SelectionView extends FrameLayout {
    public PluginView.Selection selection;
    PageView touch;
    HotPoint lostTouch;

    public static HotRect rectHandle(SelectionCursor.Which which, int x, int y) {
        final int dpi = ZLibrary.Instance().getDisplayDPI();
        final int unit = dpi / 120;
        final int xCenter = which == SelectionCursor.Which.Left ? x - unit - 1 : x + unit + 1;
        HotRect rect = new HotRect(xCenter - unit, y - dpi / 8, xCenter + unit, y + dpi / 8, x, y);
        if (which == SelectionCursor.Which.Left) {
            rect.union(new CircleRect(xCenter, y - dpi / 8, unit * 6));
        } else {
            rect.union(new CircleRect(xCenter, y + dpi / 8, unit * 6));
        }
        return rect;
    }

    public static void drawHandle(Canvas canvas, SelectionCursor.Which which, int x, int y, Paint handles) { // SelectionCursor.draw
        final int dpi = ZLibrary.Instance().getDisplayDPI();
        final int unit = dpi / 120;
        final int xCenter = which == SelectionCursor.Which.Left ? x - unit - 1 : x + unit + 1;
        canvas.drawRect(xCenter - unit, y - dpi / 8, xCenter + unit, y + dpi / 8, handles);
        if (which == SelectionCursor.Which.Left) {
            canvas.drawCircle(xCenter, y - dpi / 8, unit * 6, handles);
        } else {
            canvas.drawCircle(xCenter, y + dpi / 8, unit * 6, handles);
        }
    }

    public static class Rect {
        public int left;
        public int top;
        public int right;
        public int bottom;

        public Rect() {
        }

        public Rect(android.graphics.Rect rect) {
            this(rect.left, rect.top, rect.right, rect.bottom);
        }

        public Rect(Rect rect) {
            this(rect.left, rect.top, rect.right, rect.bottom);
        }

        public Rect(int left, int top, int right, int bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }

        public void union(Rect rect) {
            union(rect.left, rect.top, rect.right, rect.bottom);
        }

        public void union(int left, int top, int right, int bottom) { // Rect.union
            if ((left < right) && (top < bottom)) {
                if ((this.left < this.right) && (this.top < this.bottom)) {
                    if (this.left > left) this.left = left;
                    if (this.top > top) this.top = top;
                    if (this.right < right) this.right = right;
                    if (this.bottom < bottom) this.bottom = bottom;
                } else {
                    this.left = left;
                    this.top = top;
                    this.right = right;
                    this.bottom = bottom;
                }
            }
        }

        public boolean contains(int x, int y) {
            return left < right && top < bottom  // check for empty first
                    && x >= left && x < right && y >= top && y < bottom;
        }

        public int width() {
            return right - left;
        }

        public int height() {
            return bottom - top;
        }

        public android.graphics.Rect toRect() {
            return new android.graphics.Rect(left, top, right, bottom);
        }

        public void relativeTo(Rect rect) { // make child of rect, abs coords == rect.x + this.x
            left -= rect.left;
            top -= rect.top;
            right -= rect.left;
            bottom -= rect.top;
        }

        public void absTo(Point p) {
            left += p.x;
            right += p.x;
            top += p.y;
            bottom += p.y;
        }
    }

    public static class CircleRect extends Rect {
        public CircleRect(int x, int y, int r) {
            super(x - r, y - r, x + r, y + r);
        }
    }

    public static class HotRect extends Rect {
        public int hotx;
        public int hoty;

        public HotRect(int left, int top, int right, int bottom, int x, int y) {
            super(left, top, right, bottom);
            this.hotx = x;
            this.hoty = y;
        }

        @Override
        public void relativeTo(Rect rect) { // make child of rect, abs coords == rect.x + this.x
            super.relativeTo(rect);
            hotx = hotx - rect.left;
            hoty = hoty - rect.top;
        }

        public HotPoint makePoint(int x, int y) {
            return new HotPoint(x, y, hotx - x, hoty - y);
        }
    }

    public static class TouchRect {
        public HotRect rect;
        public HotPoint touch;
        public HotPoint abs;

        public TouchRect() {
        }

        public void relativeTo(Rect rect) {
            this.rect.relativeTo(rect);
            if (touch != null)
                touch.relativeTo(rect);
        }

        public boolean onTouchEvent(MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN && rect.contains(x, y) || touch != null) {
                if (touch == null)
                    touch = rect.makePoint(x, y);
                else
                    touch = new HotPoint(x, y, touch);
                return true;
            }
            return false;
        }

        public void onTouchRelease(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                touch = null;
        }

        public void absTo(Rect rect, Rect coords) {
            abs = new HotPoint(touch);
            abs.absTo(coords);
            touch.absTo(rect);
        }
    }

    public static class HotPoint extends Point {
        public int offx;
        public int offy;

        public HotPoint(HotPoint r) {
            super(r);
            offx = r.offx;
            offy = r.offy;
        }

        public HotPoint(int x, int y, int hx, int hy) {
            super(x + hx, y + hy);
            offx = hx;
            offy = hy;
        }

        public HotPoint(int x, int y, HotPoint h) {
            super(x + h.offx, y + h.offy);
            offx = h.offx;
            offy = h.offy;
        }

        public void relativeTo(Rect r) { // make child of rect, abs coords == r.x + p.x
            x = x - r.left;
            y = y - r.top;
        }

        public void absTo(Rect r) { // make abs coords
            x = x + r.left;
            y = y + r.top;
        }
    }

    public static class HandleRect extends TouchRect {
        public SelectionCursor.Which which;

        public void makeUnion(Rect rect) {
            rect.union(this.rect);
            if (touch != null)
                rect.union(rectHandle(which, touch.x, touch.y));
        }
    }

    public static class PageView extends View {
        Rect rect = new Rect(); // view size
        Rect coords = new Rect(); // absolute coords (parent of SelectionView coords)
        PluginView.Selection.Bounds bounds;

        HandleRect startRect = new HandleRect();
        ArrayList<Rect> lines = new ArrayList<>();
        HandleRect endRect = new HandleRect();

        PDFPlugin.Selection.Setter setter;

        Paint paint;
        Paint handles;

        HotPoint lostTouch;

        public PageView(Context context, FBReaderView.CustomView custom, PDFPlugin.Selection.Setter setter) {
            super(context);
            this.paint = new Paint();
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(0x99 << 24 | custom.getSelectionBackgroundColor().intValue());

            this.handles = new Paint();
            this.handles.setStyle(Paint.Style.FILL);
            this.handles.setColor(0xff << 24 | custom.getSelectionBackgroundColor().intValue());

            this.setter = setter;

            setEnabled(false);

            setBackgroundColor(0x33 << 24 | (0xffffff & Color.BLUE));
            setLayoutParams(new MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        public void update(int x, int y) {
            update();
            coords.left = rect.left + x;
            coords.right = rect.right + x;
            coords.top = rect.top + y;
            coords.bottom = rect.bottom + y;
        }

        public void update() {
            bounds = setter.getBounds();

            if (bounds.rr == null || bounds.rr.length == 0)
                return;

            lines.clear();
            int i = 0;
            Rect rect = new Rect(bounds.rr[i++]);
            Rect line = new Rect(rect);
            for (; i < bounds.rr.length; i++) {
                Rect r = new Rect(bounds.rr[i]);
                rect.union(r);
                if (line.bottom < r.top) {
                    lines.add(line);
                    line = new Rect(r);
                } else {
                    line.union(r);
                }
            }
            lines.add(line);

            Rect first = lines.get(0);
            HotRect left = rectHandle(SelectionCursor.Which.Left, first.left, first.top + first.height() / 2);
            Rect last = lines.get(lines.size() - 1);
            HotRect right = rectHandle(SelectionCursor.Which.Right, last.right, last.top + last.height() / 2);

            if (bounds.reverse) {
                startRect.rect = right;
                startRect.which = SelectionCursor.Which.Right;
                endRect.rect = left;
                endRect.which = SelectionCursor.Which.Left;
            } else {
                startRect.rect = left;
                startRect.which = SelectionCursor.Which.Left;
                endRect.rect = right;
                endRect.which = SelectionCursor.Which.Right;
            }

            if (bounds.start)
                startRect.makeUnion(rect);
            else
                lostTouch = getLostTouch(startRect);

            if (bounds.end)
                endRect.makeUnion(rect);
            else
                lostTouch = getLostTouch(endRect);

            for (Rect r : lines)
                r.relativeTo(rect);
            startRect.relativeTo(rect);
            endRect.relativeTo(rect);

            this.rect = rect; // prevent getLostTouch confused
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (Rect r : lines)
                canvas.drawRect(r.toRect(), paint);
            if (bounds.start)
                drawHandle(canvas, startRect.which, startRect);
            if (bounds.end)
                drawHandle(canvas, endRect.which, endRect);
        }

        public void drawHandle(Canvas canvas, SelectionCursor.Which which, TouchRect rect) { // SelectionCursor.draw
            if (rect.touch != null)
                SelectionView.drawHandle(canvas, which, rect.touch.x, rect.touch.y, handles);
            else
                SelectionView.drawHandle(canvas, which, rect.rect.hotx, rect.rect.hoty, handles);
        }

        @Override
        public boolean onFilterTouchEventForSecurity(MotionEvent event) {
            return isEnabled(); // disable onTouchEvent
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (bounds.start && startRect.onTouchEvent(event)) {
                x += startRect.touch.offx;
                y += startRect.touch.offy;
                startRect.absTo(rect, coords);
                startRect.onTouchRelease(event);
                setter.setStart(x, y);
                return true;
            }
            if (bounds.end && endRect.onTouchEvent(event)) {
                x += endRect.touch.offx;
                y += endRect.touch.offy;
                endRect.absTo(rect, coords);
                endRect.onTouchRelease(event);
                setter.setEnd(x, y);
                return true;
            }
            return super.onTouchEvent(event);
        }

        public HotPoint getLostTouch() {
            HotPoint e = null;
            if (startRect.touch != null) {
                e = getLostTouch(startRect);
            }
            if (endRect.touch != null) {
                e = getLostTouch(endRect);
            }
            return e;
        }

        public HotPoint getLostTouch(TouchRect rect) {
            HotPoint e = null;
            if (rect.touch != null) {
                e = rect.abs;
                rect.abs = null;
                rect.touch = null;
            }
            return e;
        }
    }

    public SelectionView(Context context, PluginView.Selection s) {
        super(context);

        this.selection = s;

        setLayoutParams(new MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setBackgroundColor(0x33 << 24 | (0xffffff & Color.GREEN));
    }

    PageView findView(int x, int y) {
        for (int i = 0; i < getChildCount(); i++) {
            PageView view = (PageView) getChildAt(i);
            if (view.getLeft() < view.getRight() && view.getTop() < view.getBottom() && x >= view.getLeft() && x < view.getRight() && y >= view.getTop() && y < view.getBottom())
                return view;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (touch == null)
            touch = findView(x, y);
        if (touch != null) {
            MotionEvent e = MotionEvent.obtain(event);
            e.offsetLocation(-touch.getLeft(), -touch.getTop());
            if (touch.onTouchEvent(e))
                return true;
            touch = null;
        }
        return super.onTouchEvent(event);
    }

    public void add(PageView page) {
        addView(page);
    }

    public void remove(PageView page) {
        if (touch == page) {
            lostTouch = touch.getLostTouch();
            touch = null;
        }
        removeView(page);
        update();
    }

    public void update(PageView page, int x, int y) {
        page.update(x, y);
        if (page.lostTouch != null) {
            lostTouch = page.lostTouch;
            page.lostTouch = null;
        }
        if (lostTouch != null) {
            int lx = lostTouch.x;
            int ly = lostTouch.y;
            if (page.coords.contains(lx, ly)) {
                lx -= page.coords.left + lostTouch.offx;
                ly -= page.coords.top + lostTouch.offy;
                lostTouch = null;
                page.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, lx, ly, 0));
            }
        }
        update();
    }

    public void update() {
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        Rect rect = null;
        for (int i = 0; i < getChildCount(); i++) {
            PageView v = (PageView) getChildAt(i);
            if (rect == null)
                rect = new Rect(v.coords);
            else
                rect.union(v.coords);
        }
        if (rect == null) {
            lp.leftMargin = 0;
            lp.topMargin = 0;
            lp.width = 0;
            lp.height = 0;
        } else {
            lp.leftMargin = rect.left;
            lp.topMargin = rect.top;
            lp.width = rect.width();
            lp.height = rect.height();
        }
        for (int i = 0; i < getChildCount(); i++) {
            PageView v = (PageView) getChildAt(i);
            MarginLayoutParams vlp = (MarginLayoutParams) v.getLayoutParams();
            vlp.leftMargin = v.coords.left - lp.leftMargin;
            vlp.topMargin = v.coords.top - lp.topMargin;
            vlp.width = v.coords.width();
            vlp.height = v.coords.height();
            v.requestLayout();
        }
        requestLayout();
    }

    public void close() {
        if (selection != null) {
            selection.close();
            selection = null;
        }
    }
}
