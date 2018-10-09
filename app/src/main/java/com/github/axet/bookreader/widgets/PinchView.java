package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.bookreader.R;

public class PinchView extends View implements GestureDetector.OnGestureListener {
    float start; // starting sloop
    float end;
    float current;
    float centerx = 0.5f;
    float centery = 0.5f;
    Rect v;
    float sx;
    float sy;
    Bitmap bm;
    Rect src;
    Rect dst;
    GestureDetectorCompat gestures;
    Drawable close;
    Rect closeRect;
    Paint closePaint;
    Paint paint = new Paint();
    int clip;

    public PinchView(Context context, Rect v, Bitmap bm) {
        super(context);
        this.v = v;
        this.dst = new Rect(v);
        this.bm = bm;

        close = ContextCompat.getDrawable(context, R.drawable.ic_close_black_24dp);
        int closeSize = ThemeUtils.dp2px(context, 50);
        int closePadding = ThemeUtils.dp2px(context, 5);
        closeRect = new Rect(v.width() - closeSize + closePadding, closePadding, v.width() - closePadding, closeSize - closePadding);
        close.setBounds(new Rect(0, 0, closeRect.width(), closeRect.height()));
        DrawableCompat.setTint(DrawableCompat.wrap(close), Color.WHITE);
        closePaint = new Paint();
        closePaint.setStyle(Paint.Style.FILL);
        closePaint.setColor(0x33333333);

        src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
        gestures = new GestureDetectorCompat(context, this);
        gestures.setIsLongpressEnabled(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bm, src, dst, paint);
        canvas.drawRect(closeRect, closePaint);
        canvas.save();
        canvas.translate(closeRect.left, closeRect.top);
        close.draw(canvas);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestures.onTouchEvent(event))
            return true;
        return super.onTouchEvent(event);
    }

    public void onScale(ScaleGestureDetector detector) {
        current = detector.getCurrentSpan() - start;

        centerx = (detector.getFocusX() - dst.left) / dst.width();
        centery = (detector.getFocusY() - dst.top) / dst.height();

        float ratio = v.height() / (float) v.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        int x = (int) (v.left + sx - currentx);
        int y = (int) (v.top + sy - currenty * ratio);
        int w = (int) (v.width() + end + current);
        int h = (int) (v.height() + end * ratio + current * ratio);
        int r = x + w;
        int b = y + h;

        if (w > v.width()) {
            if (x > v.left)
                centerx = 0;
            if (r < v.right)
                centerx = 1;
        }

        if (h > v.height()) {
            if (y > v.top)
                centery = 0;
            if (b < v.bottom)
                centery = 1;
        }

        calc();

        invalidate();
    }

    public void onScaleEnd() {
        float ratio = v.height() / (float) v.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        sx -= currentx;
        sy -= currenty * ratio;

        end += current;
        current = 0;

        calc();

        invalidate();
    }

    void calc() {
        float ratio = v.height() / (float) v.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        int x = (int) (v.left + sx - currentx);
        int y = (int) (v.top + sy - currenty * ratio);
        int w = (int) (v.width() + end + current);
        int h = (int) (v.height() + end * ratio + current * ratio);
        int r = x + w;
        int b = y + h;

        dst.left = x;
        dst.top = y;
        dst.right = r;
        dst.bottom = b;
    }

    public void close() {
        if (bm != null) {
            bm.recycle();
            bm = null;
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (e.getY() < dst.top || e.getX() < dst.left || e.getX() > dst.right || e.getY() > dst.bottom)
            pinchClose();
        if (closeRect.contains((int) e.getX(), (int) e.getY()))
            pinchClose();
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        sx -= distanceX;
        sy -= distanceY;

        float ratio = v.height() / (float) v.width();

        int x = (int) (v.left + sx);
        int y = (int) (v.top + sy);
        int w = (int) (v.width() + end);
        int h = (int) (v.height() + end * ratio);
        int r = x + w;
        int b = y + h;

        if (x > v.left)
            sx = 0;
        if (y > v.top)
            sy = 0;
        if (r < v.right)
            sx = v.right - w - v.left;
        if (b < v.bottom)
            sy = v.bottom - h - v.top;

        calc();

        invalidate();

        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public void pinchClose() {
    }

    @Override
    public void draw(Canvas canvas) {
        Rect c = canvas.getClipBounds();
        c.bottom = clip - getTop();
        canvas.clipRect(c);
        super.draw(canvas);
    }
}
