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
    Rect page; // page rect
    Rect box; // box is smaller then 'page rect', it can increace scroll distance
    float sx;
    float sy;
    Bitmap bm;
    Rect src;
    GestureDetectorCompat gestures;
    int clip;
    int rotation = 0;

    View toolbar;
    ImageView image;
    MarginLayoutParams lp;

    public static void rotateRect(final int degrees, final int px, final int py, final Rect rect) {
        final RectF rectF = new RectF(rect);
        final Matrix matrix = new Matrix();
        matrix.setRotate(degrees, px, py);
        matrix.mapRect(rectF);
        rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
    }

    public PinchView(Context context, Rect v, Bitmap bm) {
        super(context);
        this.page = v;
        this.box = new Rect(v);
        this.bm = bm;

        LayoutInflater inflater = LayoutInflater.from(context);

        lp = new MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        image = new AppCompatImageView(context);
        image.setImageBitmap(bm);
        addView(image, lp);
        toolbar = inflater.inflate(R.layout.pinch_toolbar, this, false);
        addView(toolbar);

        View left = toolbar.findViewById(R.id.pinch_left);
        left.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rotation -= 90;
                rotation %= 360;
                ViewCompat.setRotation(image, rotation);
                limitsOff();
                calc();
            }
        });
        View right = toolbar.findViewById(R.id.pinch_right);
        right.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rotation += 90;
                rotation %= 360;
                ViewCompat.setRotation(image, rotation);
                limitsOff();
                calc();
            }
        });
        View close = toolbar.findViewById(R.id.pinch_close);
        close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pinchClose();
            }
        });

        src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
        gestures = new GestureDetectorCompat(context, this);
        gestures.setIsLongpressEnabled(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestures.onTouchEvent(event))
            return true;
        return super.onTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        box.set(page);
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

        centerx = (detector.getFocusX() - lp.leftMargin) / lp.width;
        centery = (detector.getFocusY() - lp.topMargin) / lp.height;

        float ratio = src.height() / (float) src.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        int w = (int) (page.width() + end + current);
        int h = (int) (w * ratio);
        int l = (int) (page.left + sx - currentx);
        int t = (int) (page.top + sy - currenty * ratio);
        int r = l + w;
        int b = t + h;

        Rect p = new Rect(l, t, r, b);
        rotateRect(rotation, p.centerX(), p.centerY(), p);

        if (p.width() > box.width()) {
            if (p.left > box.left)
                centerx = 0;
            if (p.right < box.right)
                centerx = 1;
        }

        if (p.height() > box.height()) {
            if (p.top > box.top)
                centery = 0;
            if (p.bottom < box.bottom)
                centery = 1;
        }

        limitsOff();
        calc();
    }

    public void onScaleEnd() {
        float ratio = page.height() / (float) page.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        sx -= currentx;
        sy -= currenty * ratio;

        end += current;
        current = 0;

        calc();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        sx -= distanceX;
        sy -= distanceY;

        limitsOff();
        calc();

        return true;
    }

    void limitsOff() {
        float ratio = src.height() / (float) src.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        int w = (int) (page.width() + end + current);
        int h = (int) (w * ratio);
        int l = (int) (page.left + sx - currentx);
        int t = (int) (page.top + sy - currenty * ratio);
        int r = l + w;
        int b = t + h;

        Rect p = new Rect(l, t, r, b);
        rotateRect(rotation, p.centerX(), p.centerY(), p);

        if (p.width() < box.width()) {
            end = end - (p.width() - box.width());
            sx = sx - (p.left - box.left);
            limitsOff();
            return;
        }
        if (p.height() < box.height()) {
            end = end - (p.height() - box.height());
            sy = sy - (p.top - box.top);
            limitsOff();
            return;
        }

        if (p.left > box.left)
            sx = sx - (p.left - box.left);
        if (p.top > box.top)
            sy = sy - (p.top - box.top);
        if (p.right < box.right)
            sx = sx - (p.right - box.right);
        if (p.bottom < box.bottom)
            sy = sy - (p.bottom - box.bottom);
    }

    void calc() {
        float ratio = src.height() / (float) src.width();

        float currentx = current * centerx;
        float currenty = current * centery;

        int w = (int) (page.width() + end + current);
        int h = (int) (w * ratio);
        int l = (int) (page.left + sx - currentx);
        int t = (int) (page.top + sy - currenty * ratio);

        lp = (MarginLayoutParams) image.getLayoutParams();
        lp.leftMargin = l;
        lp.topMargin = t;
        lp.width = w;
        lp.height = h;
        image.setLayoutParams(lp);
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
