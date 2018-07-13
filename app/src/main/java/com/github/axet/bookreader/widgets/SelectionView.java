package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.github.axet.bookreader.app.PDFPlugin;

import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.view.SelectionCursor;

import java.util.ArrayList;

public class SelectionView extends FrameLayout {
    public PluginView.Selection selection;
    PageView touch;
    HandleRect startRect = new HandleRect();
    HandleRect startRectDraw;
    HandleRect endRect = new HandleRect();
    HandleRect endRectDraw;
    Paint handles;
    Rect rect;

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
            right -= rect.left;
            top -= rect.top;
            bottom -= rect.top;
        }

        public void absTo(Point p) {
            left += p.x;
            right += p.x;
            top += p.y;
            bottom += p.y;
        }

        public void absTo(Rect rect) {
            left += rect.left;
            right += rect.left;
            top += rect.top;
            bottom += rect.top;
        }

        public String toString() {
            return left + " " + top + " " + right + " " + bottom;
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

        public HotRect(HotRect r) {
            super(r);
            hotx = r.hotx;
            hoty = r.hoty;
        }

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

        public TouchRect(TouchRect r) {
            rect = new HotRect(r.rect);
            if (r.touch != null)
                touch = new HotPoint(r.touch);
        }

        public void relativeTo(Rect rect) {
            this.rect.relativeTo(rect);
            if (touch != null)
                touch.relativeTo(rect);
        }

        public boolean onTouchEvent(int a, int x, int y) {
            if (a == MotionEvent.ACTION_DOWN && rect.contains(x, y) || touch != null) {
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
        public PageView page;
        public Point draw;

        public HandleRect() {
        }

        public HandleRect(HandleRect r, Rect re) {
            super(r);
            which = r.which;
            page = r.page;
            relativeTo(re);
        }

        public void makeUnion(Rect rect) {
            rect.union(this.rect);
            if (touch != null)
                rect.union(rectHandle(which, touch.x, touch.y));
        }

        public void drawRect(Rect rect) {
            if (touch != null) {
                draw = new HotPoint(touch);
            } else {
                draw = new Point(this.rect.hotx, this.rect.hoty);
            }
            draw.x -= rect.left;
            draw.y -= rect.top;
        }
    }

    public static class PageView extends View {
        Rect rect = new Rect(); // view size
        Rect coords = new Rect(); // absolute coords (parent of SelectionView coords)
        PluginView.Selection.Bounds bounds;

        ArrayList<Rect> lines = new ArrayList<>();

        PDFPlugin.Selection.Setter setter;

        Paint paint;

        public PageView(Context context, FBReaderView.CustomView custom, PDFPlugin.Selection.Setter setter) {
            super(context);
            this.paint = new Paint();
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(0x99 << 24 | custom.getSelectionBackgroundColor().intValue());

            this.setter = setter;

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

            for (Rect r : lines)
                r.relativeTo(rect);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (Rect r : lines)
                canvas.drawRect(r.toRect(), paint);
        }
    }

    public SelectionView(Context context, FBReaderView.CustomView custom, PluginView.Selection s) {
        super(context);

        this.selection = s;

        this.handles = new Paint();
        this.handles.setStyle(Paint.Style.FILL);
        this.handles.setColor(0xff << 24 | custom.getSelectionBackgroundColor().intValue());

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

    public void add(PageView page) {
        addView(page);
    }

    public void remove(PageView page) {
        if (touch == page) {
            touch = null;
        }
        removeView(page);
        update();
    }

    public void update(PageView page, int x, int y) {
        page.update(x, y);
        update();
    }

    public void update() {
        Rect rect = null;

        boolean reverse = false;
        Rect first = null;
        PageView firstSetter = null;
        Rect last = null;
        PageView lastSetter = null;

        for (int i = 0; i < getChildCount(); i++) {
            PageView v = (PageView) getChildAt(i);

            if (rect == null)
                rect = new Rect(v.coords);
            else
                rect.union(v.coords);

            reverse = v.bounds.reverse;

            if (v.bounds.start) {
                first = new Rect(v.lines.get(0));
                first.absTo(v.coords);
                firstSetter = v;
            }
            if (v.bounds.end) {
                last = new Rect(v.lines.get(v.lines.size() - 1));
                last.absTo(v.coords);
                lastSetter = v;
            }
        }

        if (rect == null || first == null || last == null)
            return; // closing selection view

        HotRect left = rectHandle(SelectionCursor.Which.Left, first.left, first.top + first.height() / 2);
        HotRect right = rectHandle(SelectionCursor.Which.Right, last.right, last.top + last.height() / 2);

        if (reverse) {
            startRect.rect = right;
            startRect.which = SelectionCursor.Which.Right;
            startRect.page = lastSetter;
            endRect.rect = left;
            endRect.which = SelectionCursor.Which.Left;
            endRect.page = firstSetter;
        } else {
            startRect.rect = left;
            startRect.which = SelectionCursor.Which.Left;
            startRect.page = firstSetter;
            endRect.rect = right;
            endRect.which = SelectionCursor.Which.Right;
            endRect.page = lastSetter;
        }

        startRect.makeUnion(rect);
        endRect.makeUnion(rect);

        startRect.drawRect(rect);
        endRect.drawRect(rect);

        this.rect = rect;

        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        lp.leftMargin = rect.left;
        lp.topMargin = rect.top;
        lp.width = rect.width();
        lp.height = rect.height();

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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawHandle(canvas, startRect.which, startRect);
        drawHandle(canvas, endRect.which, endRect);
    }

    public void drawHandle(Canvas canvas, SelectionCursor.Which which, HandleRect rect) { // SelectionCursor.draw
        SelectionView.drawHandle(canvas, which, rect.draw.x, rect.draw.y, handles);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX() + getLeft();
        int y = (int) event.getY() + getTop();
        if (startRect.onTouchEvent(event.getAction(), x, y)) {
            x += startRect.touch.offx - startRect.page.getLeft() - getLeft();
            y += startRect.touch.offy - startRect.page.getTop() - getTop();
            startRect.onTouchRelease(event);
            startRect.page.setter.setStart(x, y);
            return true;
        }
        if (endRect.onTouchEvent(event.getAction(), x, y)) {
            x += endRect.touch.offx - endRect.page.getLeft() - getLeft();
            y += endRect.touch.offy - endRect.page.getTop() - getTop();
            endRect.onTouchRelease(event);
            endRect.page.setter.setEnd(x, y);
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void close() {
        if (selection != null) {
            selection.close();
            selection = null;
        }
    }
}
