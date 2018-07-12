package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.github.axet.bookreader.app.PDFPlugin;

import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.view.SelectionCursor;

import java.util.ArrayList;

public class SelectionView extends View {
    Paint paint;
    Paint handles;
    PDFPlugin.Selection.Setter setter;
    RelativeLayout.LayoutParams lp;
    Rect rect = new Rect();
    PluginView.Selection.Bounds bounds;

    TouchRect startRect = new TouchRect();
    ArrayList<Rect> lines = new ArrayList<>();
    TouchRect endRect = new TouchRect();

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
            int w = width();
            int h = height();
            left = left - rect.left;
            top = top - rect.top;
            right = left + w;
            bottom = top + h;
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
    }

    public static class HotPoint extends Point {
        public int offx;
        public int offy;

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

    public SelectionView(Context context, FBReaderView.CustomView custom, PDFPlugin.Selection.Setter setter) {
        super(context);

        this.paint = new Paint();
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(0x99 << 24 | custom.getSelectionBackgroundColor().intValue());

        this.handles = new Paint();
        this.handles.setStyle(Paint.Style.FILL);
        this.handles.setColor(0xff << 24 | custom.getSelectionBackgroundColor().intValue());

        this.setter = setter;

        lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(lp);

        setBackgroundColor(0x33 << 24 | (0xffffff & Color.BLUE));
    }

    public HotRect rectHandle(SelectionCursor.Which which, int x, int y) {
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

    public void drawHandle(Canvas canvas, SelectionCursor.Which which, TouchRect rect) { // SelectionCursor.draw
        if (rect.touch != null)
            drawHandle(canvas, which, rect.touch.x, rect.touch.y);
        else
            drawHandle(canvas, which, rect.rect.hotx, rect.rect.hoty);
    }

    public void drawHandle(Canvas canvas, SelectionCursor.Which which, int x, int y) { // SelectionCursor.draw
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

    public void update() {
        bounds = setter.getBounds();

        if (bounds.rr == null || bounds.rr.length == 0)
            return;

        lines.clear();
        int i = 0;
        rect = new Rect(bounds.rr[i++]);
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
        HotRect f = rectHandle(SelectionCursor.Which.Left, first.left, first.top + first.height() / 2);
        Rect last = lines.get(lines.size() - 1);
        HotRect l = rectHandle(SelectionCursor.Which.Right, last.right, last.top + last.height() / 2);

        if (bounds.reverse) {
            startRect.rect = l;
            endRect.rect = f;
        } else {
            startRect.rect = f;
            endRect.rect = l;
        }

        if (bounds.start) {
            rect.union(startRect.rect);
            if (startRect.touch != null)
                rect.union(rectHandle(bounds.reverse ? SelectionCursor.Which.Right : SelectionCursor.Which.Left, startRect.touch.x, startRect.touch.y));
        }

        if (bounds.end) {
            rect.union(endRect.rect);
            if (endRect.touch != null)
                rect.union(rectHandle(bounds.reverse ? SelectionCursor.Which.Left : SelectionCursor.Which.Right, endRect.touch.x, endRect.touch.y));
        }

        for (Rect r : lines)
            r.relativeTo(rect);
        startRect.relativeTo(rect);
        endRect.relativeTo(rect);

        lp.width = rect.width();
        lp.height = rect.height();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (Rect r : lines)
            canvas.drawRect(r.toRect(), paint);
        if (bounds.start)
            drawHandle(canvas, bounds.reverse ? SelectionCursor.Which.Right : SelectionCursor.Which.Left, startRect);
        if (bounds.end)
            drawHandle(canvas, bounds.reverse ? SelectionCursor.Which.Left : SelectionCursor.Which.Right, endRect);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (bounds.start && startRect.onTouchEvent(event)) {
            x = x + startRect.touch.offx;
            y = y + startRect.touch.offy;
            startRect.touch.absTo(rect);
            startRect.onTouchRelease(event);
            setter.setStart(x, y);
            return true;
        }
        if (bounds.end && endRect.onTouchEvent(event)) {
            x = x + endRect.touch.offx;
            y = y + endRect.touch.offy;
            endRect.touch.absTo(rect);
            endRect.onTouchRelease(event);
            setter.setEnd(x, y);
            return true;
        }
        return super.onTouchEvent(event);
    }

}
