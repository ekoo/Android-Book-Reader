package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.view.SelectionCursor;

public class SelectionView extends View {
    Paint paint;

    public SelectionView(Context context, int color, Rect rect, PluginView pluginview, PluginView.Selection s) {
        super(context);
        this.paint = new Paint();
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(color);
    }

    public void close() {
    }

    public void drawHandle(Canvas canvas, SelectionCursor.Which which, int x, int y) { // SelectionCursor.draw
        final int dpi = ZLibrary.Instance().getDisplayDPI();
        final int unit = dpi / 120;
        final int xCenter = which == SelectionCursor.Which.Left ? x - unit - 1 : x + unit + 1;
        canvas.drawRect(xCenter - unit, y + dpi / 8, xCenter + unit, y - dpi / 8, paint);
        if (which == SelectionCursor.Which.Left) {
            canvas.drawCircle(xCenter, y - dpi / 8, unit * 6, paint);
        } else {
            canvas.drawCircle(xCenter, y + dpi / 8, unit * 6, paint);
        }
    }

}
