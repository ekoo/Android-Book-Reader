package com.github.axet.bookreader.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import com.github.axet.bookreader.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            hideSystemUI();
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private boolean fullscreen;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            setFullscreen(false);
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    public Window w;
    public View decorView;
    public DrawerLayout drawer;
    public NavigationView navigationView;
    public Toolbar toolbar;

    public static class WindowCallback implements Window.Callback {
        Window.Callback callback;

        public WindowCallback(Window.Callback callback) {
            this.callback = callback;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return callback.dispatchKeyEvent(event);
        }

        @TargetApi(11)
        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            return callback.dispatchKeyShortcutEvent(event);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            return callback.dispatchTouchEvent(event);
        }

        @Override
        public boolean dispatchTrackballEvent(MotionEvent event) {
            return callback.dispatchTrackballEvent(event);
        }

        @TargetApi(12)
        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            return callback.dispatchGenericMotionEvent(event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            return callback.dispatchPopulateAccessibilityEvent(event);
        }

        @Nullable
        @Override
        public View onCreatePanelView(int featureId) {
            return callback.onCreatePanelView(featureId);
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, Menu menu) {
            return callback.onCreatePanelMenu(featureId, menu);
        }

        @Override
        public boolean onPreparePanel(int featureId, View view, Menu menu) {
            return callback.onPreparePanel(featureId, view, menu);
        }

        @Override
        public boolean onMenuOpened(int featureId, Menu menu) {
            return callback.onMenuOpened(featureId, menu);
        }

        @Override
        public boolean onMenuItemSelected(int featureId, MenuItem item) {
            return callback.onMenuItemSelected(featureId, item);
        }

        @Override
        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
            callback.onWindowAttributesChanged(attrs);
        }

        @Override
        public void onContentChanged() {
            callback.onContentChanged();
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            callback.onWindowFocusChanged(hasFocus);
        }

        @Override
        public void onAttachedToWindow() {
            callback.onAttachedToWindow();
        }

        @Override
        public void onDetachedFromWindow() {
            callback.onDetachedFromWindow();
        }

        @Override
        public void onPanelClosed(int featureId, Menu menu) {
            callback.onPanelClosed(featureId, menu);
        }

        @Override
        public boolean onSearchRequested() {
            return callback.onSearchRequested();
        }

        @TargetApi(23)
        @Override
        public boolean onSearchRequested(SearchEvent searchEvent) {
            return callback.onSearchRequested(searchEvent);
        }

        @Nullable
        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            return null; // callback.onWindowStartingActionMode(callback);
        }

        @Nullable
        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
            return null; // callback.onWindowStartingActionMode(callback, type);
        }

        @TargetApi(11)
        @Override
        public void onActionModeStarted(ActionMode mode) {
            callback.onActionModeStarted(mode);
        }

        @TargetApi(11)
        @Override
        public void onActionModeFinished(ActionMode mode) {
            callback.onActionModeFinished(mode);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        w = getWindow();
        final Window.Callback callback = w.getCallback();
        w.setCallback(new WindowCallback(callback) {
            @Override
            public void onWindowFocusChanged(boolean hasFocus) {
                super.onWindowFocusChanged(hasFocus);
                if (hasFocus)
                    setFullscreen(fullscreen);
            }
        });
        decorView = w.getDecorView();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    public void toggle() {
        setFullscreen(!fullscreen);
    }

    @SuppressLint("InlinedApi")
    public void setFullscreen(boolean b) {
        fullscreen = b;
        if (b) {
            w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // Hide UI first
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }

            // Schedule a runnable to remove the status and navigation bar after a delay
            mHideHandler.removeCallbacks(mShowPart2Runnable);
            mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
        } else {
            w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            showSystemUI();
            // Schedule a runnable to display UI elements after a delay
            mHideHandler.removeCallbacks(mHidePart2Runnable);
            mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
        }
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    public void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        if (Build.VERSION.SDK_INT >= 11)
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        if (Build.VERSION.SDK_INT >= 14)
            drawer.setFitsSystemWindows(false);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        if (Build.VERSION.SDK_INT >= 11)
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (Build.VERSION.SDK_INT >= 14)
            drawer.setFitsSystemWindows(true);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFullscreen(fullscreen); // refresh
    }
}
