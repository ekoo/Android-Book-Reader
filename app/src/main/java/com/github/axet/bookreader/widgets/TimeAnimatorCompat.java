package com.github.axet.bookreader.widgets;

import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;

public class TimeAnimatorCompat {

    TimeListener listener;

    TimeAnimator t;
    ValueAnimator v;
    Runnable run = new Runnable() {
        @Override
        public void run() {
            if (listener != null)
                listener.onTimeUpdate(TimeAnimatorCompat.this, 0, 0);
            handler.postDelayed(run, 10);
        }
    };
    Handler handler = new Handler();

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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        cancel();
    }

    public void start() {
        if (Build.VERSION.SDK_INT >= 16) {
            t.start();
        } else if (Build.VERSION.SDK_INT >= 11) {
            v.start();
        } else {
            run.run();
        }
    }

    public void cancel() {
        if (Build.VERSION.SDK_INT >= 16) {
            t.cancel();
        } else if (Build.VERSION.SDK_INT >= 11) {
            v.cancel();
        } else {
            handler.removeCallbacks(run);
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
