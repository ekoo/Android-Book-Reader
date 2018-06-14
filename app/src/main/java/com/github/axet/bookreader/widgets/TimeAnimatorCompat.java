package com.github.axet.bookreader.widgets;

import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;

public class TimeAnimatorCompat {

    TimeListener listener;

    TimeAnimator t;
    ValueAnimator v;

    interface TimeListener {
        void onTimeUpdate(TimeAnimatorCompat animation, long totalTime, long deltaTime);
    }

    public TimeAnimatorCompat() {
        if (Build.VERSION.SDK_INT >= 16) {
            t = new TimeAnimator();
        } else if (Build.VERSION.SDK_INT >= 11) {
            v = new ValueAnimator();
        }
    }

    public void start() {
        if (Build.VERSION.SDK_INT >= 16) {
            t.start();
        } else if (Build.VERSION.SDK_INT >= 11) {
            v.start();
        }
    }

    public void cancel() {
        if (Build.VERSION.SDK_INT >= 16) {
            t.cancel();
        } else if (Build.VERSION.SDK_INT >= 11) {
            v.cancel();
        }
    }

    public void setTimeListener(TimeListener l) {
        if (Build.VERSION.SDK_INT >= 16) {
            t.setTimeListener(new TimeAnimator.TimeListener() {
                @Override
                public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                    listener.onTimeUpdate(TimeAnimatorCompat.this, totalTime, deltaTime);
                }
            });
        } else if (Build.VERSION.SDK_INT >= 11) {
            v.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @TargetApi(11)
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (listener != null) {
                        listener.onTimeUpdate(TimeAnimatorCompat.this, 0, 0);
                    }
                }
            });
        }
        listener = l;
    }

}
