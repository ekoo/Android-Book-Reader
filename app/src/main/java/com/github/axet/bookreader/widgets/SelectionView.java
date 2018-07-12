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
    PluginView pluginview;
    PDFPlugin.Selection.Setter setter;
    PDFPlugin.Selection.Page page;
    RelativeLayout.LayoutParams lp;
    Rect rect = new Rect();

    HotRect startRect;
    ArrayList<Rect> lines = new ArrayList<>();
    HotRect endRect;

    Point touchStart;
    Point touchStartOff;
    Point touchEnd;
    Point touchEndOff;

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

        public void relativeTo(Rect rect) {
            int w = width();
            int h = height();
            left = left - rect.left;
            top = top - rect.top;
            right = left + w;
            bottom = top + h;
        }

        public void makeChild(Point p) {
            p.x = p.x - left;
            p.y = p.y - top;
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
        public void relativeTo(Rect rect) {
            super.relativeTo(rect);
            hotx = hotx - rect.left;
            hoty = hoty - rect.top;
        }
    }

    public SelectionView(Context context, PluginView pluginview, FBReaderView.CustomView custom, PDFPlugin.Selection.Page page, PDFPlugin.Selection.Setter setter) {
        super(context);

        this.paint = new Paint();
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(0x99 << 24 | custom.getSelectionBackgroundColor().intValue());

        this.handles = new Paint();
        this.handles.setStyle(Paint.Style.FILL);
        this.handles.setColor(0xff << 24 | custom.getSelectionBackgroundColor().intValue());

        this.pluginview = pluginview;
        this.page = page;
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
        if (pluginview.selection.getStart() == page.page && page.page == pluginview.selection.getEnd()) {
            android.graphics.Rect[] rr = pluginview.selection.getBounds(page);

            lines.clear();
            int i = 0;
            rect = new Rect(rr[i++]);
            Rect line = new Rect(rect);
            for (; i < rr.length; i++) {
                Rect r = new Rect(rr[i]);
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
            Rect last = lines.get(lines.size() - 1);
            startRect = rectHandle(SelectionCursor.Which.Left, first.left, first.top + first.height() / 2);
            rect.union(startRect);
            if (touchStart != null) {
                rect.union(rectHandle(SelectionCursor.Which.Left, touchStart.x, touchStart.y));
                rect.makeChild(touchStart);
            }
            endRect = rectHandle(SelectionCursor.Which.Right, last.right, last.top + last.height() / 2);
            rect.union(endRect);
            if (touchEnd != null) {
                rect.union(rectHandle(SelectionCursor.Which.Right, touchEnd.x, touchEnd.y));
                rect.makeChild(touchEnd);
            }

            for (Rect r : lines) {
                r.relativeTo(rect);
            }
            startRect.relativeTo(rect);
            endRect.relativeTo(rect);

            lp.width = rect.width();
            lp.height = rect.height();
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (touchStart != null)
            drawHandle(canvas, SelectionCursor.Which.Left, touchStart.x, touchStart.y);
        else
            drawHandle(canvas, SelectionCursor.Which.Left, startRect.hotx, startRect.hoty);
        for (Rect r : lines) {
            canvas.drawRect(r.toRect(), paint);
        }
        if (touchEnd != null)
            drawHandle(canvas, SelectionCursor.Which.Right, touchEnd.x, touchEnd.y);
        else
            drawHandle(canvas, SelectionCursor.Which.Right, endRect.hotx, endRect.hoty);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (startRect.contains(x, y) || touchStart != null) {
            if (touchStart == null)
                touchStartOff = new Point(startRect.hotx - x, startRect.hoty - y);
            touchStart = new Point(rect.left + x + touchStartOff.x, rect.top + y + touchStartOff.y);
            if (event.getAction() == MotionEvent.ACTION_UP)
                touchStart = null;
            setter.setStart(x + touchStartOff.x, y + touchStartOff.y);
            return true;
        }
        if (endRect.contains(x, y) || touchEnd != null) {
            if (touchEnd == null)
                touchEndOff = new Point(endRect.hotx - x, endRect.hoty - y);
            touchEnd = new Point(rect.left + x + touchEndOff.x, rect.top + y + touchEndOff.y);
            if (event.getAction() == MotionEvent.ACTION_UP)
                touchEnd = null;
            setter.setEnd(x + touchEndOff.x, y + touchEndOff.y);
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void close() {
        ;
    }

}
