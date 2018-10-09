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
    Rect box;
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
        this.box = new Rect(v);
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

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        box.set(v);
        if (box.left < left)
            box.left = left;
        if (box.top < top)
            box.top = top;
        if (box.right > right)
            box.right = right;
        if (box.bottom > bottom)
            box.bottom = bottom;
    }

    public void onScale(ScaleGestureDetector detector) {
        current = detector.getCurrentSpan() - start;

        centerx = (detector.getFocusX() - dst.left) / dst.width();
        centery = (detector.getFocusY() - dst.top) / dst.height();

        float ratio = src.height() / (float) src.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        int w = (int) (v.width() + end + current);
        int h = (int) (w * ratio);
        int l = (int) (v.left + sx - currentx);
        int t = (int) (v.top + sy - currenty * ratio);
        int r = l + w;
        int b = t + h;

        if (w > box.width()) {
            if (l > box.left)
                centerx = 0;
            if (r < box.right)
                centerx = 1;
        }

        if (h > box.height()) {
            if (t > box.top)
                centery = 0;
            if (b < box.bottom)
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

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        sx -= distanceX;
        sy -= distanceY;

        float ratio = src.height() / (float) src.width();

        int w = (int) (v.width() + end);
        int h = (int) (w * ratio);
        int l = (int) (v.left + sx);
        int t = (int) (v.top + sy);
        int r = l + w;
        int b = t + h;

        if (l > box.left)
            sx = sx - (l - box.left);
        if (t > box.top)
            sy = sy - (t - box.top);
        if (r < box.right)
            sx = sx - (r - box.right);
        if (b < box.bottom)
            sy = sy - (b - box.bottom);

        calc();
        invalidate();

        return true;
    }

    void calc() {
        float ratio = src.height() / (float) src.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        int w = (int) (v.width() + end + current);
        int h = (int) (w * ratio);
        int l = (int) (v.left + sx - currentx);
        int t = (int) (v.top + sy - currenty * ratio);
        int r = l + w;
        int b = t + h;

        dst.left = l;
        dst.top = t;
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
        if (closeRect.contains((int) e.getX(), (int) e.getY())) {
            pinchClose();
            return true;
        }
        if (e.getY() < dst.top || e.getX() < dst.left || e.getX() > dst.right || e.getY() > dst.bottom) {
            pinchClose();
            return true;
        }
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
