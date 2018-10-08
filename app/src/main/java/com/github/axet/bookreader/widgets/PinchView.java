package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.github.axet.bookreader.R;

public class PinchView extends FrameLayout implements GestureDetector.OnGestureListener {
    float start; // starting sloop
    float end;
    float current;
    float centerx = 0.5f;
    float centery = 0.5f;
    Rect v;
    float sx;
    float sy;
    Bitmap bm;
    GestureDetectorCompat gestures;
    int rotation = 0;
    int clip;
    View toolbar;
    ImageView image;
    View pinchLeft;
    View pinchRight;
    View pinchClose;

    public static void rotateRect(final int degrees, final int px, final int py, final Rect rect) {
        final RectF rectF = new RectF(rect);
        final Matrix matrix = new Matrix();
        matrix.setRotate(degrees, px, py);
        matrix.mapRect(rectF);
        rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
    }

    public PinchView(Context context, Rect v, Bitmap bm) {
        super(context);
        this.v = v;
        this.bm = bm;

        LayoutInflater inflater = LayoutInflater.from(context);

        image = new AppCompatImageView(context);
        image.setImageBitmap(bm);
        addView(image, new MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        toolbar = inflater.inflate(R.layout.pinch_toolbar, this);
        pinchLeft = toolbar.findViewById(R.id.pinch_left);
        pinchRight = toolbar.findViewById(R.id.pinch_right);
        pinchClose = toolbar.findViewById(R.id.pinch_close);

        pinchLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rotation -= 90;
                rotation %= 360;
                ViewCompat.setRotation(image, rotation);
                limitsOff();
                calc();
            }
        });
        pinchRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rotation += 90;
                rotation %= 360;
                ViewCompat.setRotation(image, rotation);
                limitsOff();
                calc();
            }
        });
        pinchClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pinchClose();
            }
        });

        gestures = new GestureDetectorCompat(context, this);
        gestures.setIsLongpressEnabled(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestures.onTouchEvent(event))
            return true;
        return super.onTouchEvent(event);
    }

    public void onScale(ScaleGestureDetector detector) {
        current = detector.getCurrentSpan() - start;

        centerx = (detector.getFocusX() - image.getLeft()) / image.getWidth();
        centery = (detector.getFocusY() - image.getTop()) / image.getHeight();

        limitsScale();
        calc();
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
    }

    void limitsScale() {
        float ratio = v.height() / (float) v.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        int w = (int) (v.width() + end + current);
        int h = (int) (v.height() + end * ratio + current * ratio);
        int l = (int) (v.left + sx - currentx);
        int t = (int) (v.top + sy - currenty * ratio);
        int r = l + w;
        int b = t + h;

        if (w > v.width()) {
            if (l > v.left)
                centerx = 0;
            if (r < v.right)
                centerx = 1;
        }

        if (h > v.height()) {
            if (t > v.top)
                centery = 0;
            if (b < v.bottom)
                centery = 1;
        }
    }

    void limitsOff() {
        float ratio = v.height() / (float) v.width();

        int w = (int) (v.width() + end);
        int h = (int) (v.height() + end * ratio);
        int l = (int) (v.left + sx);
        int t = (int) (v.top + sy);
        int r = l + w;
        int b = t + h;

        Rect p = new Rect(l, t, r, b);
        rotateRect(rotation, p.centerX(), p.centerY(), p);

        if (p.left > v.left)
            sx = sx - (p.left - v.left);
        if (p.top > v.top)
            sy = sy - (p.top - v.top);
        if (p.right < v.right)
            sx = sx - (p.right - v.right);
        if (p.bottom < v.bottom)
            sy = sy - (p.bottom - v.bottom);
    }

    void calc() {
        float ratio = v.height() / (float) v.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        int l = (int) (v.left + sx - currentx);
        int t = (int) (v.top + sy - currenty * ratio);
        int w = (int) (v.width() + end + current);
        int h = (int) (v.height() + end * ratio + current * ratio);
        int r = l + w;
        int b = t + h;

        MarginLayoutParams dst = (MarginLayoutParams) image.getLayoutParams();
        dst.leftMargin = l;
        dst.topMargin = t;
        dst.width = w;
        dst.height = h;
        image.setLayoutParams(dst);
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
        Rect r = new Rect();
        image.getHitRect(r);
        if (!r.contains((int) e.getX(), (int) e.getY())) {
            pinchClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        sx -= distanceX;
        sy -= distanceY;

        limitsOff();
        calc();

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
    protected void dispatchDraw(Canvas canvas) {
        Rect c = canvas.getClipBounds();
        c.bottom = clip - getTop();
        canvas.clipRect(c);
        super.dispatchDraw(canvas);
    }
}
