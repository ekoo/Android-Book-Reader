package com.github.axet.bookreader.widgets;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.axet.androidlibrary.services.FileProvider;
import com.github.axet.androidlibrary.services.StorageProvider;
import com.github.axet.androidlibrary.widgets.AboutPreferenceCompat;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.app.ComicsPlugin;
import com.github.axet.bookreader.app.DjvuPlugin;
import com.github.axet.bookreader.app.MainApplication;
import com.github.axet.bookreader.app.PDFPlugin;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.services.ImagesProvider;
import com.github.johnpersano.supertoasts.SuperActivityToast;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.OnClickWrapper;
import com.github.johnpersano.supertoasts.util.OnDismissWrapper;

import org.geometerplus.android.fbreader.NavigationPopup;
import org.geometerplus.android.fbreader.PopupPanel;
import org.geometerplus.android.fbreader.SelectionPopup;
import org.geometerplus.android.fbreader.TextSearchPopup;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.bookmark.EditBookmarkActivity;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.android.util.UIMessageUtil;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.FBHyperlinkType;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.DictionaryHighlighting;
import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.fbreader.options.ColorProfile;
import org.geometerplus.fbreader.fbreader.options.FooterOptions;
import org.geometerplus.fbreader.fbreader.options.ImageOptions;
import org.geometerplus.fbreader.fbreader.options.MiscOptions;
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.util.AutoTextSnippet;
import org.geometerplus.fbreader.util.TextSnippet;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.core.options.StringPair;
import org.geometerplus.zlibrary.core.options.ZLOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.ZLTextElementAreaVector;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlinkRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextImageElement;
import org.geometerplus.zlibrary.text.view.ZLTextImageRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextRegion;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.ZLTextWordRegionSoul;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FBReaderView extends RelativeLayout {

    public static final String ACTION_MENU = FBReaderView.class.getCanonicalName() + ".ACTION_MENU";

    public static final int PAGE_OVERLAP_PERCENTS = 5; // percents
    public static final int PAGE_PAPER_COLOR = 0x80ffffff;

    public enum Widgets {PAGING, CONTINUOUS}

    public FBReaderApp app;
    public ConfigShadow config;
    public ZLViewWidget widget;
    public int battery;
    public String title;
    public Window w;
    public Storage.FBook book;
    public PluginView pluginview;
    public PageTurningListener pageTurningListener;
    PinchView pinch;
    SelectionView selection;
    DrawerLayout drawer;

    public interface PageTurningListener {
        void onScrollingFinished(ZLViewEnums.PageIndex index);
    }

    public class CustomView extends FBView {
        public CustomView(FBReaderApp reader) {
            super(reader);
        }

        public ZLAndroidPaintContext createContext(Canvas c) {
            return new ZLAndroidPaintContext(
                    app.SystemInfo,
                    c,
                    new ZLAndroidPaintContext.Geometry(
                            getWidth(),
                            getHeight(),
                            getWidth(),
                            getHeight(),
                            0,
                            0
                    ),
                    getVerticalScrollbarWidth()
            );
        }

        public ZLAndroidPaintContext setContext() {
            ZLAndroidPaintContext context = createContext(new Canvas());
            setContext(context);
            return context;
        }

        public void setScalingType(ZLTextImageElement imageElement, ZLPaintContext.ScalingType s) {
            book.info.scales.put(imageElement.Id, s);
        }

        @Override
        protected ZLPaintContext.ScalingType getScalingType(ZLTextImageElement imageElement) {
            ZLPaintContext.ScalingType s = book.info.scales.get(imageElement.Id);
            if (s != null)
                return s;
            return super.getScalingType(imageElement);
        }

        @Override
        public void hideOutline() {
            super.hideOutline();
            if (widget instanceof ScrollView) {
                ((ScrollView) widget).adapter.processInvalidate();
                ((ScrollView) widget).adapter.processClear();
            }
        }

        @Override
        protected ZLTextRegion findRegion(int x, int y, int maxDistance, ZLTextRegion.Filter filter) {
            return super.findRegion(x, y, maxDistance, filter);
        }

        @Override
        public void onFingerSingleTapLastResort(int x, int y) {
            if (widget instanceof ScrollView)
                onFingerSingleTapLastResort(((ScrollView) widget).gesturesListener.e);
            else
                super.onFingerSingleTapLastResort(x, y);
        }

        public void onFingerSingleTapLastResort(MotionEvent e) {
            setContext();
            super.onFingerSingleTapLastResort((int) e.getX(), (int) e.getY());
        }

        @Override
        public boolean twoColumnView() {
            if (widget instanceof ScrollView)
                return false;
            return super.twoColumnView();
        }

        @Override
        public boolean canScroll(PageIndex index) {
            if (pluginview != null)
                return pluginview.canScroll(index);
            else
                return super.canScroll(index);
        }

        @Override
        public synchronized void onScrollingFinished(PageIndex pageIndex) {
            if (pluginview != null) {
                if (pluginview.onScrollingFinished(pageIndex))
                    widget.reset();
            } else {
                super.onScrollingFinished(pageIndex);
            }
            if (pageTurningListener != null)
                pageTurningListener.onScrollingFinished(pageIndex);
        }

        @Override
        public synchronized PagePosition pagePosition() { // Footer draw
            if (pluginview != null)
                return pluginview.pagePosition();
            else
                return super.pagePosition();
        }

        @Override
        public void gotoHome() {
            if (pluginview != null)
                pluginview.gotoPosition(new ZLTextFixedPosition(0, 0, 0));
            else
                super.gotoHome();
            resetNewPosition();
        }

        @Override
        public synchronized void gotoPage(int page) {
            if (pluginview != null)
                pluginview.gotoPosition(new ZLTextFixedPosition(page - 1, 0, 0));
            else
                super.gotoPage(page);
            resetNewPosition();
        }

        @Override
        public synchronized void paint(ZLPaintContext context, PageIndex pageIndex) {
            super.paint(context, pageIndex);
        }
    }

    public class FBAndroidWidget extends ZLAndroidWidget {
        PinchGesture pinch;

        public FBAndroidWidget() {
            super(FBReaderView.this.getContext());

            ZLApplication = new ZLAndroidWidget.ZLApplicationInstance() {
                public ZLApplication Instance() {
                    return app;
                }
            };
            setFocusable(true);

            config.setValue(app.PageTurningOptions.FingerScrolling, PageTurningOptions.FingerScrollingType.byTapAndFlick);

            if (Looper.myLooper() != null) { // render view only
                pinch = new PinchGesture(FBReaderView.this.getContext()) {
                    @Override
                    public void onScaleBegin(float x, float y) {
                        Rect dst;
                        PluginPage p = pluginview.current; // current.renderRect() show partial page
                        if (p.pageOffset < 0) { // show empty space at beginig
                            int t = (int) (-p.pageOffset / p.ratio);
                            dst = new Rect(0, t, p.w, t + (int) (p.pageBox.h / p.ratio));
                        } else if (p.pageOffset == 0 && p.hh > p.pageBox.h) {  // show middle vertically
                            int t = (int) ((p.hh - p.pageBox.h) / p.ratio / 2);
                            dst = new Rect(0, t, p.w, p.h - t);
                        } else {
                            int t = (int) (-p.pageOffset / p.ratio);
                            dst = new Rect(0, t, p.w, t + (int) (p.pageBox.h / p.ratio));
                        }
                        pinchOpen(pluginview.current.pageNumber, dst);
                    }
                };
            }
        }

        @Override
        public void setScreenBrightness(int percent) {
            if (percent < 1) {
                percent = 1;
            } else if (percent > 100) {
                percent = 100;
            }

            final float level;
            final Integer oldColorLevel = myColorLevel;
            if (percent >= 25) {
                // 100 => 1f; 25 => .01f
                level = .01f + (percent - 25) * .99f / 75;
                myColorLevel = null;
            } else {
                level = .01f;
                myColorLevel = 0x60 + (0xFF - 0x60) * Math.max(percent, 0) / 25;
            }

            final WindowManager.LayoutParams attrs = w.getAttributes();
            attrs.screenBrightness = level;
            w.setAttributes(attrs);

            if (oldColorLevel != myColorLevel) {
                updateColorLevel();
                postInvalidate();
            }
        }

        @Override
        public int getScreenBrightness() {
            if (myColorLevel != null) {
                return (myColorLevel - 0x60) * 25 / (0xFF - 0x60);
            }

            float level = w.getAttributes().screenBrightness;
            level = level >= 0 ? level : .5f;

            // level = .01f + (percent - 25) * .99f / 75;
            return 25 + (int) ((level - .01f) * 75 / .99f);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public void drawOnBitmap(Bitmap bitmap, ZLViewEnums.PageIndex index) {
            if (pluginview != null)
                pluginview.drawOnBitmap(getContext(), bitmap, getWidth(), getMainAreaHeight(), index, (CustomView) app.BookTextView, book.info);
            else
                super.drawOnBitmap(bitmap, index);
        }

        @Override
        public void repaint() {
            super.repaint();
        }

        @Override
        public void reset() {
            super.reset();
            if (pluginview != null) {
                if (pluginview.reflower != null) {
                    pluginview.reflower.reset();
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (pluginview != null && !pluginview.reflow) {
                if (pinch.onTouchEvent(event))
                    return true;
            }
            return super.onTouchEvent(event);
        }
    }

    public class FBApplicationWindow implements ZLApplicationWindow {
        @Override
        public void setWindowTitle(String title) {
            FBReaderView.this.title = title;
        }

        @Override
        public void showErrorMessage(String resourceKey) {
        }

        @Override
        public void showErrorMessage(String resourceKey, String parameter) {
        }

        @Override
        public ZLApplication.SynchronousExecutor createExecutor(String key) {
            return null;
        }

        @Override
        public void processException(Exception e) {
        }

        @Override
        public void refresh() {
        }

        @Override
        public ZLViewWidget getViewWidget() {
            return widget;
        }

        @Override
        public void close() {
        }

        @Override
        public int getBatteryLevel() {
            return battery;
        }
    }

    public class FBReaderApp extends org.geometerplus.fbreader.fbreader.FBReaderApp {
        public FBReaderApp(Context context) {
            super(new Storage.Info(context), new BookCollectionShadow());
        }

        @Override
        public TOCTree getCurrentTOCElement() {
            if (pluginview != null)
                return pluginview.getCurrentTOCElement(Model.TOCTree);
            else
                return super.getCurrentTOCElement();
        }
    }

    public static class ConfigShadow extends Config { // disable config changes across this app view instancies and fbreader
        public Map<String, String> map = new TreeMap<>();

        public ConfigShadow() {
        }

        public void setValue(ZLOption opt, int i) {
            apply(opt);
            setValue(opt.myId, String.valueOf(i));
        }

        public void setValue(ZLOption opt, boolean b) {
            apply(opt);
            setValue(opt.myId, String.valueOf(b));
        }

        public void setValue(ZLOption opt, Enum v) {
            apply(opt);
            setValue(opt.myId, String.valueOf(v));
        }

        public void setValue(ZLOption opt, String v) {
            apply(opt);
            setValue(opt.myId, v);
        }

        public void apply(ZLOption opt) {
            opt.Config = new ZLOption.ConfigInstance() {
                public Config Instance() {
                    return ConfigShadow.this;
                }
            };
        }

        @Override
        public String getValue(StringPair id, String defaultValue) {
            String v = map.get(id.Group + ":" + id.Name);
            if (v != null)
                return v;
            return super.getValue(id, defaultValue);
        }

        @Override
        public void setValue(StringPair id, String value) {
            map.put(id.Group + ":" + id.Name, value);
        }

        @Override
        public void unsetValue(StringPair id) {
            map.remove(id.Group + ":" + id.Name);
        }

        @Override
        protected void setValueInternal(String group, String name, String value) {
        }

        @Override
        protected void unsetValueInternal(String group, String name) {
        }

        @Override
        protected Map<String, String> requestAllValuesForGroupInternal(String group) throws NotAvailableException {
            return null;
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public void runOnConnect(Runnable runnable) {
        }

        @Override
        public List<String> listGroups() {
            return null;
        }

        @Override
        public List<String> listNames(String group) {
            return null;
        }

        @Override
        public void removeGroup(String name) {
        }

        @Override
        public boolean getSpecialBooleanValue(String name, boolean defaultValue) {
            return false;
        }

        @Override
        public void setSpecialBooleanValue(String name, boolean value) {
        }

        @Override
        public String getSpecialStringValue(String name, String defaultValue) {
            return null;
        }

        @Override
        public void setSpecialStringValue(String name, String value) {
        }

        @Override
        protected String getValueInternal(String group, String name) throws NotAvailableException {
            throw new NotAvailableException("default");
        }
    }

    public class ScrollView extends RecyclerView implements ZLViewWidget {
        public LinearLayoutManager lm;
        public ScrollAdapter adapter = new ScrollAdapter();
        Gestures gesturesListener = new Gestures(getContext());

        public class ScrollAdapter extends RecyclerView.Adapter<ScrollAdapter.PageHolder> {
            public ArrayList<PageCursor> pages = new ArrayList<>();
            final Object lock = new Object();
            Thread thread;
            PluginRect size = new PluginRect(); // ScrollView size, after reset
            Set<PageHolder> invalidates = new HashSet<>(); // pending invalidates

            public class PageView extends View {
                public PageHolder holder;
                TimeAnimatorCompat time;
                FrameLayout progress;
                ProgressBar progressBar;
                TextView progressText;
                Bitmap bm; // cache bitmap
                PageCursor cache; // cache cursor
                Paint paint = new Paint(); // cache paint

                ZLTextElementAreaVector text;
                Reflow.Info info;
                SelectionView.PageView selection;

                public PageView(ViewGroup parent) {
                    super(parent.getContext());
                    progress = new FrameLayout(getContext());

                    progressBar = new ProgressBar(getContext()) {
                        Handler handler = new Handler();

                        @Override
                        public void draw(Canvas canvas) {
                            super.draw(canvas);
                            onAttachedToWindow(); // startAnimation
                        }

                        @Override
                        public int getVisibility() {
                            return VISIBLE;
                        }

                        @Override
                        public int getWindowVisibility() {
                            return VISIBLE;
                        }

                        @Override
                        public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
                            if (time != null)
                                handler.postAtTime(what, when);
                            else
                                onDetachedFromWindow(); // stopAnimation
                        }
                    };
                    progressBar.setIndeterminate(true);
                    progress.addView(progressBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                    progressText = new TextView(getContext());
                    progressText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    progress.addView(progressText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
                }

                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int w = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
                    int h = ScrollView.this.getMainAreaHeight();
                    if (pluginview != null) {
                        if (!pluginview.reflow) {
                            PageCursor c = current();
                            h = (int) Math.ceil(pluginview.getPageHeight(w, c));
                        }
                    }
                    setMeasuredDimension(w, h);
                }

                PageCursor current() {
                    int page = holder.getAdapterPosition();
                    if (page == -1)
                        return null;
                    return pages.get(page);
                }

                @Override
                protected void onDraw(Canvas draw) {
                    final PageCursor c = current();
                    if (c == null) {
                        invalidate();
                        return;
                    }
                    if (isCached(c)) {
                        drawCache(draw);
                        return;
                    }
                    if (pluginview != null) {
                        if (pluginview.reflow) {
                            final int page;
                            final int index;
                            if (c.start == null) {
                                int p = c.end.getParagraphIndex();
                                int i = c.end.getElementIndex();
                                i = i - 1;
                                if (i < 0)
                                    p = p - 1;
                                else
                                    c.start = new ZLTextFixedPosition(p, i, 0);
                                page = p;
                                index = i;
                            } else {
                                page = c.start.getParagraphIndex();
                                index = c.start.getElementIndex();
                            }
                            synchronized (lock) {
                                final int w = getWidth();
                                final int h = getHeight();
                                if (thread == null) {
                                    if (pluginview.reflower != null) {
                                        if (pluginview.reflower.page != page || pluginview.reflower.count() == -1 || pluginview.reflower.w != w || pluginview.reflower.h != h) {
                                            pluginview.reflower.close();
                                            pluginview.reflower = null;
                                        }
                                    }
                                }
                                if (pluginview.reflower == null) {
                                    if (thread == null) {
                                        thread = new Thread() {
                                            @Override
                                            public void run() {
                                                int i = index;
                                                Reflow reflower = new Reflow(getContext(), w, h, page, (CustomView) app.BookTextView, book.info);
                                                Bitmap bm = pluginview.render(reflower.w, reflower.h, page);
                                                reflower.load(bm);
                                                if (reflower.count() > 0)
                                                    bm.recycle();
                                                if (i < 0) {
                                                    i = reflower.count() + i;
                                                    c.start = new ZLTextFixedPosition(page, i, 0);
                                                }
                                                reflower.current = i;
                                                synchronized (lock) {
                                                    pluginview.reflower = reflower;
                                                    thread = null;
                                                }
                                            }
                                        };
                                        thread.setPriority(Thread.MIN_PRIORITY);
                                        thread.start();
                                    }
                                }
                                if (thread != null) {
                                    if (time == null) {
                                        time = new TimeAnimatorCompat();
                                        time.start();
                                        time.setTimeListener(new TimeAnimatorCompat.TimeListener() {
                                            @Override
                                            public void onTimeUpdate(TimeAnimatorCompat animation, long totalTime, long deltaTime) {
                                                invalidate();
                                            }
                                        });
                                    }
                                    drawProgress(draw, page, index);
                                    return;
                                }
                                if (time != null) {
                                    time.cancel();
                                    time = null;
                                }
                                Canvas canvas = getCanvas(draw, c);
                                pluginview.current.pageNumber = page;
                                pluginview.reflower.current = c.start.getElementIndex();
                                if (pluginview.reflower.count() > 0) { // empty source page?
                                    Bitmap bm = pluginview.reflower.render(c.start.getElementIndex());
                                    Rect src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
                                    Rect dst = new Rect(app.BookTextView.getLeftMargin(), 0, app.BookTextView.getLeftMargin() + pluginview.reflower.rw, pluginview.reflower.h);
                                    canvas.drawColor(Color.WHITE);
                                    canvas.drawBitmap(bm, src, dst, pluginview.paint);
                                    info = new Reflow.Info(pluginview.reflower, c.start.getElementIndex());
                                } else {
                                    pluginview.drawWallpaper(canvas);
                                    pluginview.drawPage(canvas, getWidth(), getHeight(), pluginview.reflower.bm);
                                }
                                update();
                                drawCache(draw);
                            }
                            return;
                        }
                        open(c);
                        pluginview.drawOnCanvas(getContext(), draw, getWidth(), getHeight(), ZLViewEnums.PageIndex.current, (CustomView) app.BookTextView, book.info);
                        update();
                    } else {
                        open(c);
                        final ZLAndroidPaintContext context = new ZLAndroidPaintContext(
                                app.SystemInfo,
                                draw,
                                new ZLAndroidPaintContext.Geometry(
                                        getWidth(),
                                        getHeight(),
                                        getWidth(),
                                        getHeight(),
                                        0,
                                        0
                                ),
                                getVerticalScrollbarWidth()
                        );
                        app.BookTextView.paint(context, ZLViewEnums.PageIndex.current);
                        text = app.BookTextView.myCurrentPage.TextElementMap;
                        app.BookTextView.myCurrentPage.TextElementMap = new ZLTextElementAreaVector();
                        update();
                    }
                }

                void drawProgress(Canvas canvas, int page, int index) {
                    canvas.drawColor(Color.GRAY);
                    canvas.save();
                    canvas.translate(getWidth() / 2 - progressBar.getMeasuredWidth() / 2, getHeight() / 2 - progressBar.getMeasuredHeight() / 2);

                    String t = (page + 1) + "." + (index == -1 ? "*" : index);
                    progressText.setText(t);

                    int dp60 = ThemeUtils.dp2px(getContext(), 60);
                    progress.measure(MeasureSpec.makeMeasureSpec(dp60, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(dp60, MeasureSpec.EXACTLY));
                    progress.layout(0, 0, dp60, dp60);
                    progress.draw(canvas);

                    canvas.restore();
                }

                void recycle() {
                    if (bm != null) {
                        bm.recycle();
                        bm = null;
                    }
                    info = null;
                    text = null;
                    if (time != null) {
                        time.cancel();
                        time = null;
                    }
                }

                boolean isCached(PageCursor c) {
                    if (cache == null || cache != c) // should be same 'cache' memory ref
                        return false;
                    if (bm == null)
                        return false;
                    return true;
                }

                void drawCache(Canvas draw) {
                    Rect src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
                    Rect dst = new Rect(0, 0, getWidth(), getHeight());
                    draw.drawBitmap(bm, src, dst, paint);
                }

                Canvas getCanvas(Canvas draw, PageCursor c) {
                    if (bm != null)
                        recycle();
                    bm = Bitmap.createBitmap(draw.getWidth(), draw.getHeight(), Bitmap.Config.RGB_565);
                    cache = c;
                    return new Canvas(bm);
                }
            }

            public class PageHolder extends RecyclerView.ViewHolder {
                public PageView page;

                public PageHolder(PageView p) {
                    super(p);
                    page = p;
                }
            }

            public class PageCursor {
                public ZLTextPosition start;
                public ZLTextPosition end;

                public PageCursor(ZLTextPosition s, ZLTextPosition e) {
                    if (s != null)
                        start = new ZLTextFixedPosition(s);
                    if (e != null)
                        end = new ZLTextFixedPosition(e);
                }

                public boolean equals(ZLTextPosition p1, ZLTextPosition p2) {
                    return p1.getCharIndex() == p2.getCharIndex() && p1.getElementIndex() == p2.getElementIndex() && p1.getParagraphIndex() == p2.getParagraphIndex();
                }

                @Override
                public boolean equals(Object obj) {
                    PageCursor p = (PageCursor) obj;
                    if (start != null && p.start != null) {
                        if (equals(start, p.start))
                            return true;
                    }
                    if (end != null && p.end != null) {
                        if (equals(end, p.end))
                            return true;
                    }
                    return false;
                }

                public void update(PageCursor c) {
                    if (c.start != null)
                        start = c.start;
                    if (c.end != null)
                        end = c.end;
                }
            }

            public ScrollAdapter() {
            }

            void open(PageCursor c) {
                if (c.start == null) {
                    if (pluginview != null) {
                        pluginview.gotoPosition(c.end);
                        pluginview.onScrollingFinished(ZLViewEnums.PageIndex.previous);
                        pluginview.current.pageOffset = 0; // widget instanceof ScrollView
                        c.update(getCurrent());
                    } else {
                        app.BookTextView.gotoPosition(c.end);
                        app.BookTextView.onScrollingFinished(ZLViewEnums.PageIndex.previous);
                        c.update(getCurrent());
                    }
                } else {
                    if (pluginview != null)
                        pluginview.gotoPosition(c.start);
                    else {
                        PageCursor cc = getCurrent();
                        if (!cc.equals(c)) {
                            app.BookTextView.gotoPosition(c.start, c.end);
                        }
                    }
                }
            }

            @Override
            public PageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new PageHolder(new PageView(parent));
            }

            @Override
            public void onBindViewHolder(PageHolder holder, int position) {
                holder.page.holder = holder;
            }

            @Override
            public void onViewRecycled(PageHolder holder) {
                super.onViewRecycled(holder);
                holder.page.recycle();
            }

            @Override
            public int getItemCount() {
                return pages.size();
            }

            public void reset() { // read current position
                size.w = getWidth();
                size.h = getHeight();
                if (pluginview != null) {
                    if (pluginview.reflower != null) {
                        pluginview.reflower.reset();
                    }
                }
                getRecycledViewPool().clear();
                pages.clear();
                if (app.Model != null) {
                    app.BookTextView.preparePage(((CustomView) app.BookTextView).createContext(new Canvas()), ZLViewEnums.PageIndex.current);
                    PageCursor c = getCurrent();
                    pages.add(c);
                }
                postInvalidate();
                notifyDataSetChanged();
            }

            PageCursor getCurrent() {
                if (pluginview != null) {
                    if (pluginview.reflow && pluginview.reflower != null) {
                        ZLTextFixedPosition s = new ZLTextFixedPosition(pluginview.reflower.page, pluginview.reflower.current, 0);
                        int c = s.ParagraphIndex;
                        int p = s.ElementIndex + 1;
                        if (p >= pluginview.reflower.count()) { // current points to next page +1
                            c = pluginview.reflower.page + 1;
                            p = 0;
                        }
                        ZLTextFixedPosition e = new ZLTextFixedPosition(c, p, 0);
                        return new PageCursor(s, e);
                    } else {
                        return new PageCursor(pluginview.getPosition(), pluginview.getNextPosition());
                    }
                } else {
                    return new PageCursor(app.BookTextView.getStartCursor(), app.BookTextView.getEndCursor());
                }
            }

            void update() {
                if (app.Model == null)
                    return;
                PageCursor c = getCurrent();
                int page;
                for (page = 0; page < pages.size(); page++) {
                    PageCursor p = pages.get(page);
                    if (p.equals(c)) {
                        p.update(c);
                        break;
                    }
                }
                if (page == pages.size()) { // not found == 0
                    pages.add(c);
                    notifyItemInserted(page);
                }
                if (app.BookTextView.canScroll(ZLViewEnums.PageIndex.previous)) {
                    if (page == 0) {
                        pages.add(page, new PageCursor(null, c.start));
                        notifyItemInserted(page);
                        page++; // 'c' page moved to + 1
                    }
                }
                if (app.BookTextView.canScroll(ZLViewEnums.PageIndex.next)) {
                    if (page == pages.size() - 1) {
                        page++;
                        pages.add(page, new PageCursor(c.end, null));
                        notifyItemInserted(page);
                    }
                }
            }

            void processInvalidate() {
                for (ScrollView.ScrollAdapter.PageHolder h : invalidates) {
                    h.page.recycle();
                    h.page.invalidate();
                }
            }

            void processClear() {
                invalidates.clear();
            }
        }

        public class Gestures implements GestureDetector.OnGestureListener {
            MotionEvent e;
            int x;
            int y;
            ScrollAdapter.PageView v;
            ScrollAdapter.PageCursor c;
            PinchGesture pinch;
            GestureDetectorCompat gestures;

            Gestures(Context context) {
                gestures = new GestureDetectorCompat(context, this);

                if (Looper.myLooper() != null) {
                    pinch = new PinchGesture(context) {
                        @Override
                        public void onScaleBegin(float x, float y) {
                            ScrollView.ScrollAdapter.PageView v = ScrollView.this.findView(x, y);
                            if (v == null)
                                return;
                            int pos = v.holder.getAdapterPosition();
                            if (pos == -1)
                                return;
                            ScrollView.ScrollAdapter.PageCursor c = adapter.pages.get(pos);
                            int page;
                            if (c.start == null)
                                page = c.end.getParagraphIndex() - 1;
                            else
                                page = c.start.getParagraphIndex();
                            pinchOpen(page, new Rect(v.getLeft(), v.getTop(), v.getLeft() + v.getWidth(), v.getTop() + v.getHeight()));
                        }
                    };
                }
            }

            boolean open(MotionEvent e) {
                if (!openCursor(e))
                    return false;
                return openText(e);
            }

            boolean openCursor(MotionEvent e) {
                this.e = e;
                v = findView(e);
                if (v == null)
                    return false;
                x = (int) (e.getX() - v.getLeft());
                y = (int) (e.getY() - v.getTop());
                int pos = v.holder.getAdapterPosition();
                if (pos == -1)
                    return false;
                c = adapter.pages.get(pos);
                return true;
            }

            boolean openText(MotionEvent e) {
                if (v.text == null)
                    return false;
                if (!app.BookTextView.getStartCursor().samePositionAs(c.start))
                    app.BookTextView.gotoPosition(c.start);
                app.BookTextView.myCurrentPage.TextElementMap = v.text;
                return true;
            }

            void closeText() {
                app.BookTextView.myCurrentPage.TextElementMap = new ZLTextElementAreaVector();
            }

            @Override
            public boolean onDown(MotionEvent e) {
                if (app.BookTextView.mySelection.isEmpty())
                    return false;
                if (!open(e))
                    return false;
                app.BookTextView.onFingerPress(x, y);
                v.invalidate();
                closeText();
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (!open(e)) { // pluginview or reflow
                    ((CustomView) app.BookTextView).onFingerSingleTapLastResort(e);
                    return true;
                }
                app.BookTextView.onFingerSingleTap(x, y);
                v.invalidate();
                adapter.invalidates.add(v.holder);
                closeText();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (app.BookTextView.mySelection.isEmpty())
                    return false;
                if (!open(e))
                    return false;
                app.BookTextView.onFingerMove(x, y);
                v.invalidate();
                adapter.invalidates.add(v.holder);
                closeText();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (!openCursor(e))
                    return;
                if (pluginview != null) {
                    PluginView.Selection s = pluginview.select(c.start, v.info, v.getWidth(), v.getHeight(), x, y);
                    if (s != null) {
                        selectionOpen(s);
                        return;
                    }
                    FBReaderView.this.selectionClose();
                }
                if (!openText(e))
                    return;
                app.BookTextView.onFingerLongPress(x, y);
                app.BookTextView.onFingerReleaseAfterLongPress(x, y);
                v.invalidate();
                app.BookTextView.myCurrentPage.TextElementMap = new ZLTextElementAreaVector();
                adapter.invalidates.add(v.holder);
                closeText();
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }

            public boolean onRelease(MotionEvent e) {
                if (app.BookTextView.mySelection.isEmpty())
                    return false;
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    if (!open(e))
                        return false;
                    app.BookTextView.onFingerRelease(x, y);
                    v.invalidate();
                    closeText();
                    return true;
                }
                return false;
            }

            public boolean onCancel(MotionEvent e) {
                if (app.BookTextView.mySelection.isEmpty())
                    return false;
                if (e.getAction() == MotionEvent.ACTION_CANCEL) {
                    app.BookTextView.onFingerEventCancelled();
                    v.invalidate();
                    return true;
                }
                return false;
            }

            public boolean onFilter(MotionEvent e) {
                if (app.BookTextView.mySelection.isEmpty())
                    return false;
                return true;
            }

            public boolean onTouchEvent(MotionEvent e) {
                if (pinch.onTouchEvent(e))
                    return true;
                onRelease(e);
                onCancel(e);
                if (gestures.onTouchEvent(e))
                    return true;
                if (onFilter(e))
                    return true;
                return false;
            }
        }

        class TopSnappedSmoothScroller extends LinearSmoothScroller {
            public TopSnappedSmoothScroller(Context context) {
                super(context);
            }

            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return lm.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_ANY;
            }

            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int
                    snapPreference) {
                switch (snapPreference) {
                    case SNAP_TO_START:
                        return boxStart - viewStart;
                    case SNAP_TO_END:
                        return boxEnd - viewEnd;
                    case SNAP_TO_ANY:
                        int dtBox = boxEnd - boxStart;
                        int dtView = viewEnd - viewStart;
                        if (dtBox < dtView) {
                            return -viewStart;
                        }
                        final int dtStart = boxStart - viewStart;
                        if (dtStart > 0) {
                            return dtStart;
                        }
                        final int dtEnd = boxEnd - viewEnd;
                        if (dtEnd < 0) {
                            return dtEnd;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("snap preference should be one of the"
                                + " constants defined in SmoothScroller, starting with SNAP_");
                }
                return 0;
            }
        }

        class TopAlwaysSmoothScroller extends LinearSmoothScroller {
            public TopAlwaysSmoothScroller(Context context) {
                super(context);
            }

            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return lm.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }
        }

        public ScrollView(Context context) {
            super(context);

            lm = new LinearLayoutManager(context) {
                @Override
                public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
                    int off = super.scrollVerticallyBy(dy, recycler, state);
                    if (selection != null)
                        updateSelection();
                    return off;
                }

                @Override
                public void smoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
                    RecyclerView.SmoothScroller smoothScroller = new TopAlwaysSmoothScroller(recyclerView.getContext());
                    smoothScroller.setTargetPosition(position);
                    startSmoothScroll(smoothScroller);
                }
            };

            setLayoutManager(lm);
            setAdapter(adapter);

            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            addItemDecoration(dividerItemDecoration);

            FBView.Footer footer = app.BookTextView.getFooterArea();
            if (footer != null)
                setPadding(0, 0, 0, footer.getHeight());

            setItemAnimator(null);

            config.setValue(app.PageTurningOptions.FingerScrolling, PageTurningOptions.FingerScrollingType.byFlick);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (gesturesListener.onTouchEvent(e))
                return true;
            return super.onTouchEvent(e);
        }

        ScrollAdapter.PageView findView(MotionEvent e) {
            return findView(e.getX(), e.getY());
        }

        ScrollAdapter.PageView findView(float x, float y) {
            for (int i = 0; i < lm.getChildCount(); i++) {
                ScrollAdapter.PageView view = (ScrollAdapter.PageView) lm.getChildAt(i);
                if (view.getLeft() < view.getRight() && view.getTop() < view.getBottom() && x >= view.getLeft() && x < view.getRight() && y >= view.getTop() && y < view.getBottom())
                    return view;
            }
            return null;
        }

        ScrollAdapter.PageView findRegionView(ZLTextRegion.Soul soul) {
            for (int i = 0; i < lm.getChildCount(); i++) {
                ScrollAdapter.PageView view = (ScrollAdapter.PageView) lm.getChildAt(i);
                if (view.text != null && view.text.getRegion(soul) != null)
                    return view;
            }
            return null;
        }

        public void reset() {
            postInvalidate();
        }

        @Override
        public void repaint() {
        }

        public int getViewPercent(View view) {
            int h = 0;
            if (view.getBottom() > 0)
                h = view.getBottom(); // visible height
            if (getBottom() < view.getBottom())
                h -= view.getBottom() - getBottom();
            if (view.getTop() > 0)
                h -= view.getTop();
            int hp = h * 100 / view.getHeight();
            return hp;
        }

        public int findFirstPage() {
            Map<Integer, View> hp15 = new TreeMap<>();
            Map<Integer, View> hp100 = new TreeMap<>();
            Map<Integer, View> hp0 = new TreeMap<>();
            for (int i = 0; i < lm.getChildCount(); i++) {
                View view = lm.getChildAt(i);
                int hp = getViewPercent(view);
                if (hp > 15) // add only views atleast 15% visible
                    hp15.put(view.getTop(), view);
                if (hp == 100) {
                    hp100.put(view.getTop(), view);
                }
                if (hp > 0) {
                    hp0.put(view.getTop(), view);
                }
            }
            View v = null;
            for (Integer key : hp100.keySet()) {
                v = hp15.get(key);
                break;
            }
            if (v == null) {
                for (Integer key : hp15.keySet()) {
                    v = hp15.get(key);
                    break;
                }
            }
            if (v == null) {
                for (Integer key : hp15.keySet()) {
                    v = hp0.get(key);
                    break;
                }
            }
            if (v != null)
                return ((ScrollAdapter.PageView) v).holder.getAdapterPosition();
            return -1;
        }

        @Override
        public void startManualScrolling(int x, int y, ZLViewEnums.Direction direction) {
        }

        @Override
        public void scrollManuallyTo(int x, int y) {
        }

        @Override
        public void startAnimatedScrolling(ZLViewEnums.PageIndex pageIndex, int x, int y, ZLViewEnums.Direction direction, int speed) {
            startAnimatedScrolling(pageIndex, direction, speed);
        }

        @Override
        public void startAnimatedScrolling(ZLViewEnums.PageIndex pageIndex, ZLViewEnums.Direction direction, int speed) {
            int pos = findFirstPage();
            if (pos == -1)
                return;
            switch (pageIndex) {
                case next:
                    pos++;
                    break;
                case previous:
                    pos--;
                    break;
            }
            if (pos < 0 || pos >= adapter.pages.size())
                return;
            smoothScrollToPosition(pos);
            pinchClose();
        }

        @Override
        public void startAnimatedScrolling(int x, int y, int speed) {
        }

        @Override
        public void setScreenBrightness(int percent) {
        }

        @Override
        public int getScreenBrightness() {
            return 0;
        }

        @Override
        public void onDraw(Canvas c) {
            super.onDraw(c);
        }

        @Override
        public void draw(Canvas c) {
            if (adapter.size.w != getWidth() || adapter.size.h != getHeight()) { // reset for textbook and reflow mode only
                adapter.reset();
                pinchClose();
            }
            super.draw(c);
            updatePosition();
            drawFooter(c);
        }

        void updatePosition() { // position can vary depend on which page drawn, restore it after every draw
            int first = findFirstPage();
            if (first == -1)
                return;
            if (pluginview != null && pluginview.reflow) {
                ScrollView.ScrollAdapter.PageCursor cc = adapter.pages.get(first);
                if (cc.start == null) {
                    int p = cc.end.getParagraphIndex();
                    int i = cc.end.getElementIndex() - 1;
                    if (i < 0)
                        p = p - 1;
                    pluginview.current.pageNumber = p;
                } else {
                    pluginview.current.pageNumber = cc.start.getParagraphIndex();
                }
                clearReflowPage(); // reset reflow page, since we treat pageOffset differently for reflower/full page view
            } else {
                ScrollView.ScrollAdapter.PageCursor c = adapter.pages.get(first);
                adapter.open(c);
            }
        }

        void drawFooter(Canvas c) {
            if (app.Model != null) {
                FBView.Footer footer = app.BookTextView.getFooterArea();
                if (footer == null)
                    return;
                ZLAndroidPaintContext context = new ZLAndroidPaintContext(
                        app.SystemInfo,
                        c,
                        new ZLAndroidPaintContext.Geometry(
                                getWidth(),
                                getHeight(),
                                getWidth(),
                                footer.getHeight(),
                                0,
                                getMainAreaHeight()
                        ),
                        0
                );
                int voffset = getHeight() - footer.getHeight();
                c.save();
                c.translate(0, voffset);
                footer.paint(context);
                c.restore();
            }
        }

        public int getMainAreaHeight() {
            final ZLView.FooterArea footer = ZLApplication.Instance().getCurrentView().getFooterArea();
            return footer != null ? getHeight() - footer.getHeight() : getHeight();
        }

        public void selectionClose() {
            for (int i = 0; i < lm.getChildCount(); i++) {
                ScrollView.ScrollAdapter.PageView view = (ScrollView.ScrollAdapter.PageView) lm.getChildAt(i);
                selectionRemove(view);
            }
        }

        public void selectionRemove(ScrollAdapter.PageView view) {
            if (view.selection != null) {
                selection.remove(view.selection);
                view.selection = null;
            }
        }

        public void updateSelection() {
            for (int i = 0; i < lm.getChildCount(); i++) {
                final ScrollAdapter.PageView view = (ScrollAdapter.PageView) lm.getChildAt(i);
                selectionUpdate(view);
            }
        }

        public void selectionUpdate(final ScrollAdapter.PageView view) {
            int pos = view.holder.getAdapterPosition();
            if (pos == -1) {
                selectionRemove(view);
            } else {
                final ScrollAdapter.PageCursor c = adapter.pages.get(pos);
                boolean selected = true;
                final PluginView.Selection.Page page;

                if (c.start == null || c.end == null) {
                    selected = false;
                    page = null;
                } else {
                    page = pluginview.selectPage(c.start, view.info, view.getWidth(), view.getHeight());
                }

                int s = selection.selection.getStart();
                int e = selection.selection.getEnd();
                if (s > e) {
                    s = selection.selection.getEnd();
                    e = selection.selection.getStart();
                }

                if (selected)
                    selected = (s <= page.page && page.page <= e);

                final Rect first;
                final Rect last;

                if (selected && pluginview.reflow && pluginview.reflower != null) {
                    Map<Rect, Rect> src = view.info.src;
                    Rect[] all = src.keySet().toArray(new Rect[0]);
                    Arrays.sort(all, new SelectionView.LinesUL(all));
                    first = all[0];
                    last = all[all.length - 1];

                    Boolean b = selection.selection.isBelow(page, new PluginView.Selection.Point(first.left, first.top));
                    if (b == null)
                        b = selection.selection.isBelow(page, new PluginView.Selection.Point(first.left + 1, first.top + 1));
                    if (b == null)
                        b = selection.selection.isBelow(page, new PluginView.Selection.Point(first.left, first.centerY()));
                    if (b == null)
                        b = selection.selection.isBelow(page, new PluginView.Selection.Point(first.centerX(), first.centerY()));

                    Boolean a = selection.selection.isAbove(page, new PluginView.Selection.Point(last.right, last.bottom));
                    if (a == null)
                        a = selection.selection.isAbove(page, new PluginView.Selection.Point(last.right - 1, last.bottom - 1));
                    if (a == null)
                        a = selection.selection.isAbove(page, new PluginView.Selection.Point(last.right, last.centerY()));
                    if (a == null)
                        a = selection.selection.isAbove(page, new PluginView.Selection.Point(last.centerX(), last.centerY()));

                    selected = b != null && b && a != null && a;
                } else {
                    first = null;
                    last = null;
                }

                if (selected) {
                    if (view.selection == null) {
                        PluginView.Selection.Setter setter = new PDFPlugin.Selection.Setter() {
                            @Override
                            public void setStart(int x, int y) {
                                ScrollAdapter.PageView v = findView(x, y);
                                if (v != null) {
                                    int pos = v.holder.getAdapterPosition();
                                    if (pos != -1) {
                                        ScrollAdapter.PageCursor c = adapter.pages.get(pos);
                                        x = x - v.getLeft();
                                        y = y - v.getTop();
                                        PluginView.Selection.Page page = pluginview.selectPage(c.start, v.info, v.getWidth(), v.getHeight());
                                        if (pluginview.reflow) { // full word selection in reflow mode
                                            Rect rect = pluginview.selectRect(v.info, x, y);
                                            if (rect != null)
                                                selection.selection.setStart(page, new PluginView.Selection.Point(rect.left + 1, rect.centerY()));
                                        } else {
                                            PluginView.Selection.Point point = pluginview.selectPoint(v.info, x, y);
                                            selection.selection.setStart(page, point);
                                        }
                                    }
                                }
                                selectionUpdate(view);
                                if (v != null && v != view)
                                    selectionUpdate(v);
                            }

                            @Override
                            public void setEnd(int x, int y) {
                                ScrollAdapter.PageView v = findView(x, y);
                                if (v != null) {
                                    int pos = v.holder.getAdapterPosition();
                                    if (pos != -1) {
                                        ScrollAdapter.PageCursor c = adapter.pages.get(pos);
                                        x = x - v.getLeft();
                                        y = y - v.getTop();
                                        PluginView.Selection.Page page = pluginview.selectPage(c.start, v.info, v.getWidth(), v.getHeight());
                                        if (pluginview.reflow) { // full word selection in reflow mode
                                            Rect rect = pluginview.selectRect(v.info, x, y);
                                            if (rect != null)
                                                selection.selection.setEnd(page, new PluginView.Selection.Point(rect.right - 1, rect.centerY()));
                                        } else {
                                            PluginView.Selection.Point point = pluginview.selectPoint(v.info, x, y);
                                            selection.selection.setEnd(page, point);
                                        }
                                    }
                                }
                                selectionUpdate(view);
                                if (v != null && v != view)
                                    selectionUpdate(v);
                            }

                            @Override
                            public PluginView.Selection.Bounds getBounds() {
                                PluginView.Selection.Bounds bounds = selection.selection.getBounds(page);
                                if (pluginview.reflow) {
                                    ArrayList<Rect> list = new ArrayList<>();
                                    for (int i = 0; i < bounds.rr.length; i++) {
                                        Rect r = bounds.rr[i];
                                        int area = 0;
                                        Rect b = null;
                                        for (Rect s : view.info.src.keySet()) {
                                            if (SelectionView.area(r, s) > area) {
                                                area = SelectionView.area(r, s);
                                                b = s;
                                            }
                                        }
                                        if (b != null)
                                            list.add(view.info.src.get(b));
                                    }
                                    bounds.rr = list.toArray(new Rect[0]);

                                    Boolean a = selection.selection.isAbove(page, new PluginView.Selection.Point(first.left, first.top));
                                    if (a == null)
                                        a = selection.selection.isAbove(page, new PluginView.Selection.Point(first.left + 1, first.top + 1));
                                    if (a == null)
                                        a = selection.selection.isAbove(page, new PluginView.Selection.Point(first.left, first.centerY()));
                                    if (a == null)
                                        a = selection.selection.isAbove(page, new PluginView.Selection.Point(first.centerX(), first.centerY()));

                                    Boolean b = selection.selection.isBelow(page, new PluginView.Selection.Point(last.right, last.bottom));
                                    if (b == null)
                                        b = selection.selection.isBelow(page, new PluginView.Selection.Point(last.right - 1, last.bottom - 1));
                                    if (b == null)
                                        b = selection.selection.isBelow(page, new PluginView.Selection.Point(last.right, last.centerY()));
                                    if (b == null)
                                        b = selection.selection.isBelow(page, new PluginView.Selection.Point(last.centerX(), last.centerY()));

                                    bounds.start = a != null && a == false;
                                    bounds.end = b != null && b == false;
                                }
                                return bounds;
                            }
                        };
                        view.selection = new SelectionView.PageView(getContext(), (CustomView) app.BookTextView, setter);
                        selection.add(view.selection);
                    }
                    int x = view.getLeft();
                    int y = view.getTop();
                    if (pluginview.reflow)
                        x += view.info.margin.left;
                    selection.update(view.selection, x, y);
                } else {
                    selectionRemove(view);
                }
            }
        }
    }

    public class PinchGesture implements ScaleGestureDetector.OnScaleGestureListener {
        ScaleGestureDetector scale;
        boolean scaleTouch = false;

        public PinchGesture(Context context) {
            scale = new ScaleGestureDetector(context, this);
            ScaleGestureDetectorCompat.setQuickScaleEnabled(scale, false);
        }

        boolean isScaleTouch(MotionEvent e) {
            if (pinch != null)
                return true;
            if (pluginview == null || pluginview.reflow)
                return false;
            if (e.getPointerCount() >= 2) {
                return true;
            }
            return false;
        }

        public boolean onTouchEvent(MotionEvent e) {
            if (isScaleTouch(e)) {
                if (scaleTouch && (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_DOWN) && e.getPointerCount() == 1)
                    scaleTouch = false;
                if (e.getPointerCount() == 2)
                    scaleTouch = true;
                scale.onTouchEvent(e);
                if (pinch != null) {
                    if (!scaleTouch)
                        pinch.onTouchEvent(e);
                    return true;
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleTouch = true;
            if (pinch == null)
                return false;
            pinch.onScale(detector);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            scaleTouch = true;
            if (pinch == null) {
                float x = detector.getFocusX();
                float y = detector.getFocusY();
                onScaleBegin(x, y);
            }
            pinch.start = detector.getCurrentSpan();
            return true;
        }

        public void onScaleBegin(float x, float y) {
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            scaleTouch = true;
            pinch.onScaleEnd();
            if (pinch.end < 0)
                pinchClose();
        }
    }

    public FBReaderView(Context context) { // create child view
        super(context);
        create();
    }

    public FBReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        create();
    }

    public FBReaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create();
    }

    @TargetApi(21)
    public FBReaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        create();
    }

    public void create() {
        config = new ConfigShadow();
        app = new FBReaderApp(getContext());

        app.setWindow(new FBApplicationWindow());
        app.initWindow();

        if (app.getPopupById(TextSearchPopup.ID) == null) {
            new TextSearchPopup(app);
        }
        if (app.getPopupById(NavigationPopup.ID) == null) {
            new NavigationPopup(app);
        }
        if (app.getPopupById(SelectionPopup.ID) == null) {
            new SelectionPopup(app) {
                @Override
                public void createControlPanel(Activity activity, RelativeLayout root) {
                    super.createControlPanel(activity, root);
                    View t = myWindow.findViewById(org.geometerplus.zlibrary.ui.android.R.id.selection_panel_translate);
                    t.setVisibility(View.GONE);
                    t = myWindow.findViewById(org.geometerplus.zlibrary.ui.android.R.id.selection_panel_bookmark);
                    t.setVisibility(View.GONE);
                }
            };
        }

        config();

        app.BookTextView = new CustomView(app);
        app.setView(app.BookTextView);

        setWidget(Widgets.PAGING);
    }

    public void configColorProfile(SharedPreferences shared) {
        if (shared.getString(MainApplication.PREFERENCE_THEME, "").equals(getContext().getString(R.string.Theme_Dark))) {
            config.setValue(app.ViewOptions.ColorProfileName, ColorProfile.NIGHT);
        } else {
            config.setValue(app.ViewOptions.ColorProfileName, ColorProfile.DAY);
        }
    }

    public void configWidget(SharedPreferences shared) {
        String mode = shared.getString(MainApplication.PREFERENCE_VIEW_MODE, "");
        setWidget(mode.equals(FBReaderView.Widgets.CONTINUOUS.toString()) ? FBReaderView.Widgets.CONTINUOUS : FBReaderView.Widgets.PAGING);
    }

    public void config() {
        SharedPreferences shared = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
        configColorProfile(shared);

        int d = shared.getInt(MainApplication.PREFERENCE_FONTSIZE_FBREADER, app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption.getValue());
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption, d);

        String f = shared.getString(MainApplication.PREFERENCE_FONTFAMILY_FBREADER, app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption.getValue());
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption, f);

        config.setValue(app.MiscOptions.AllowScreenBrightnessAdjustment, false);
        config.setValue(app.ViewOptions.ScrollbarType, FBView.SCROLLBAR_SHOW_AS_FOOTER);
        config.setValue(app.ViewOptions.getFooterOptions().ShowProgress, FooterOptions.ProgressDisplayType.asPages);

        config.setValue(app.ImageOptions.TapAction, ImageOptions.TapActionEnum.openImageView);
        config.setValue(app.ImageOptions.FitToScreen, FBView.ImageFitting.covers);

        config.setValue(app.MiscOptions.WordTappingAction, MiscOptions.WordTappingActionEnum.startSelecting);
    }

    public void setWidget(Widgets w) {
        switch (w) {
            case CONTINUOUS:
                setWidget(new ScrollView(getContext()));
                break;
            case PAGING:
                setWidget(new FBAndroidWidget());
                break;
        }
    }

    public void setWidget(ZLViewWidget v) {
        pinchClose();
        if (widget != null)
            removeView((View) widget);
        widget = v;
        addView((View) v, 0, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (pluginview != null)
            gotoPluginPosition(getPosition());
    }

    public void loadBook(Storage.FBook fbook) {
        try {
            this.book = fbook;
            if (book.info == null)
                book.info = new Storage.RecentInfo();
            FormatPlugin plugin = Storage.getPlugin((Storage.Info) app.SystemInfo, fbook);
            if (plugin instanceof PDFPlugin) {
                pluginview = new PDFPlugin.PdfiumView(BookUtil.fileByBook(fbook.book));
                BookModel Model = BookModel.createModel(fbook.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info.position != null)
                    gotoPluginPosition(book.info.position);
            } else if (plugin instanceof DjvuPlugin) {
                pluginview = new DjvuPlugin.DjvuView(BookUtil.fileByBook(fbook.book));
                BookModel Model = BookModel.createModel(fbook.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info.position != null)
                    gotoPluginPosition(book.info.position);
            } else if (plugin instanceof ComicsPlugin) {
                pluginview = new ComicsPlugin.ComicsView(BookUtil.fileByBook(fbook.book));
                BookModel Model = BookModel.createModel(fbook.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info.position != null)
                    gotoPluginPosition(book.info.position);
            } else {
                BookModel Model = BookModel.createModel(fbook.book, plugin);
                ZLTextHyphenator.Instance().load(fbook.book.getLanguage());
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info.position != null)
                    app.BookTextView.gotoPosition(book.info.position);
                if (book.info.scale != null)
                    config.setValue(app.ImageOptions.FitToScreen, book.info.scale);
                if (book.info.fontsize != null)
                    config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption, book.info.fontsize);
            }
            widget.repaint();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void closeBook() {
        if (pluginview != null) {
            pluginview.close();
            pluginview = null;
        }
        app.BookTextView.setModel(null);
        app.Model = null;
        book = null;
    }

    public ZLTextFixedPosition getPosition() {
        if (pluginview != null)
            return pluginview.getPosition();
        else
            return new ZLTextFixedPosition(app.BookTextView.getStartCursor());
    }

    public void setWindow(Window w) {
        this.w = w;
        if (widget instanceof FBAndroidWidget)
            config.setValue(app.MiscOptions.AllowScreenBrightnessAdjustment, true);
    }

    public void setActivity(final Activity a) {
        PopupPanel.removeAllWindows(app, a);

        app.addAction(ActionCode.SEARCH, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                app.hideActivePopup();
                final String pattern = (String) params[0];
                final Runnable runnable = new Runnable() {
                    public void run() {
                        final TextSearchPopup popup = (TextSearchPopup) app.getPopupById(TextSearchPopup.ID);
                        popup.initPosition();
                        config.setValue(app.MiscOptions.TextSearchPattern, pattern);
                        if (app.getTextView().search(pattern, true, false, false, false) != 0) {
                            a.runOnUiThread(new Runnable() {
                                public void run() {
                                    app.showPopup(popup.getId());
                                }
                            });
                        } else {
                            a.runOnUiThread(new Runnable() {
                                public void run() {
                                    UIMessageUtil.showErrorMessage(a, "textNotFound");
                                    popup.StartPosition = null;
                                }
                            });
                        }
                    }
                };
                UIUtil.wait("search", runnable, getContext());
            }
        });

        app.addAction(ActionCode.DISPLAY_BOOK_POPUP, new FBAction(app) { //  new DisplayBookPopupAction(this, myFBReaderApp))
            @Override
            protected void run(Object... params) {
            }
        });
        app.addAction(ActionCode.PROCESS_HYPERLINK, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final ZLTextRegion region = Reader.getTextView().getOutlinedRegion();
                if (region == null) {
                    return;
                }

                final ZLTextRegion.Soul soul = region.getSoul();
                if (soul instanceof ZLTextHyperlinkRegionSoul) {
                    app.BookTextView.hideOutline();
                    final ZLTextHyperlink hyperlink = ((ZLTextHyperlinkRegionSoul) soul).Hyperlink;
                    switch (hyperlink.Type) {
                        case FBHyperlinkType.EXTERNAL:
                            AboutPreferenceCompat.openUrlDialog(getContext(), hyperlink.Id);
                            break;
                        case FBHyperlinkType.INTERNAL:
                        case FBHyperlinkType.FOOTNOTE: {
                            final AutoTextSnippet snippet = Reader.getFootnoteData(hyperlink.Id);
                            if (snippet == null) {
                                break;
                            }

                            Reader.Collection.markHyperlinkAsVisited(Reader.getCurrentBook(), hyperlink.Id);
                            final boolean showToast;
                            switch (Reader.MiscOptions.ShowFootnoteToast.getValue()) {
                                default:
                                case never:
                                    showToast = false;
                                    break;
                                case footnotesOnly:
                                    showToast = hyperlink.Type == FBHyperlinkType.FOOTNOTE;
                                    break;
                                case footnotesAndSuperscripts:
                                    showToast =
                                            hyperlink.Type == FBHyperlinkType.FOOTNOTE ||
                                                    region.isVerticallyAligned();
                                    break;
                                case allInternalLinks:
                                    showToast = true;
                                    break;
                            }
                            if (showToast) {
                                final SuperActivityToast toast;
                                if (snippet.IsEndOfText) {
                                    toast = new SuperActivityToast(a, SuperToast.Type.STANDARD);
                                } else {
                                    toast = new SuperActivityToast(a, SuperToast.Type.BUTTON);
                                    toast.setButtonIcon(
                                            android.R.drawable.ic_menu_more,
                                            ZLResource.resource("toast").getResource("more").getValue()
                                    );
                                    toast.setOnClickWrapper(new OnClickWrapper("ftnt", new SuperToast.OnClickListener() {
                                        @Override
                                        public void onClick(View view, Parcelable token) {
                                            showPopup(hyperlink);
                                        }
                                    }));
                                }
                                toast.setText(snippet.getText());
                                toast.setDuration(Reader.MiscOptions.FootnoteToastDuration.getValue().Value);
                                toast.setOnDismissWrapper(new OnDismissWrapper("ftnt", new SuperToast.OnDismissListener() {
                                    @Override
                                    public void onDismiss(View view) {
                                        Reader.getTextView().hideOutline();
                                    }
                                }));
                                Reader.getTextView().outlineRegion(region);
                                showToast(toast);
                            } else {
                                book.info.position = getPosition();
                                showPopup(hyperlink);
                            }
                            break;
                        }
                    }
                } else if (soul instanceof ZLTextImageRegionSoul) {
                    final ZLTextImageRegionSoul image = ((ZLTextImageRegionSoul) soul);
                    final View anchor = new View(getContext());
                    LayoutParams lp = new LayoutParams(region.getRight() - region.getLeft(), region.getBottom() - region.getTop());
                    lp.leftMargin = region.getLeft();
                    lp.topMargin = region.getTop();
                    if (widget instanceof ScrollView) {
                        ScrollView.ScrollAdapter.PageView p = ((ScrollView) widget).findRegionView(soul);
                        lp.leftMargin += p.getLeft();
                        lp.topMargin += p.getTop();
                    }
                    FBReaderView.this.addView(anchor, lp);
                    final PopupMenu menu = new PopupMenu(getContext(), anchor, Gravity.BOTTOM);
                    menu.inflate(R.menu.image_menu);
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.action_open: {
                                    String t = image.ImageElement.Id;
                                    String type = Storage.getTypeByExt(ImagesProvider.EXT);
                                    Uri uri = ImagesProvider.share(getContext(), Uri.parse(image.ImageElement.URL), t);
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(uri, type);
                                    FileProvider.grantPermissions(getContext(), intent, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    getContext().startActivity(intent);
                                    break;
                                }
                                case R.id.action_share: {
                                    String t = image.ImageElement.Id;
                                    String type = Storage.getTypeByExt(ImagesProvider.EXT);
                                    Uri uri = StorageProvider.share(getContext(), Uri.parse(image.ImageElement.URL), t);
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType(type);
                                    intent.putExtra(Intent.EXTRA_EMAIL, "");
                                    intent.putExtra(Intent.EXTRA_SUBJECT, t);
                                    intent.putExtra(Intent.EXTRA_TEXT, getContext().getString(R.string.shared_via, getContext().getString(R.string.app_name)));
                                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                                    FileProvider.grantPermissions(getContext(), intent, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    getContext().startActivity(intent);
                                    break;
                                }
                                case R.id.action_original:
                                    ((CustomView) app.BookTextView).setScalingType(image.ImageElement, ZLPaintContext.ScalingType.OriginalSize);
                                    resetCaches();
                                    break;
                                case R.id.action_zoom:
                                    ((CustomView) app.BookTextView).setScalingType(image.ImageElement, ZLPaintContext.ScalingType.FitMaximum);
                                    resetCaches();
                                    break;
                                case R.id.action_original_all:
                                    book.info.scales.clear();
                                    book.info.scale = FBView.ImageFitting.covers;
                                    config.setValue(app.ImageOptions.FitToScreen, FBView.ImageFitting.covers);
                                    resetCaches();
                                    break;
                                case R.id.action_zoom_all:
                                    book.info.scales.clear();
                                    book.info.scale = FBView.ImageFitting.all;
                                    config.setValue(app.ImageOptions.FitToScreen, FBView.ImageFitting.all);
                                    resetCaches();
                                    break;
                            }
                            return true;
                        }
                    });
                    menu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            Reader.getTextView().hideOutline();
                            Reader.getViewWidget().repaint();
                            FBReaderView.this.removeView(anchor);
                        }
                    });
                    getHandler().post(new Runnable() { // allow anchor view to be placed
                        @Override
                        public void run() {
                            menu.show();
                        }
                    });
                } else if (soul instanceof ZLTextWordRegionSoul) {
                    DictionaryUtil.openTextInDictionary(
                            a,
                            ((ZLTextWordRegionSoul) soul).Word.getString(),
                            true,
                            region.getTop(),
                            region.getBottom(),
                            new Runnable() {
                                public void run() {
                                    // a.outlineRegion(soul);
                                }
                            }
                    );
                }
            }
        });
        app.addAction(ActionCode.SHOW_MENU, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                a.sendBroadcast(new Intent(ACTION_MENU));
            }
        });
        app.addAction(ActionCode.SHOW_NAVIGATION, new FBAction(app) {
            @Override
            public boolean isVisible() {
                if (pluginview != null)
                    return true;
                final ZLTextView view = (ZLTextView) Reader.getCurrentView();
                final ZLTextModel textModel = view.getModel();
                return textModel != null && textModel.getParagraphsNumber() != 0;
            }

            @Override
            protected void run(Object... params) {
                ((NavigationPopup) app.getPopupById(NavigationPopup.ID)).runNavigation();
            }
        });
        app.addAction(ActionCode.SELECTION_SHOW_PANEL, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final ZLTextView view = app.getTextView();
                ((SelectionPopup) app.getPopupById(SelectionPopup.ID))
                        .move(view.getSelectionStartY(), view.getSelectionEndY());
                app.showPopup(SelectionPopup.ID);
            }
        });
        app.addAction(ActionCode.SELECTION_HIDE_PANEL, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final FBReaderApp.PopupPanel popup = app.getActivePopup();
                if (popup != null && popup.getId() == SelectionPopup.ID) {
                    app.hideActivePopup();
                }
            }
        });
        app.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final FBView fbview = Reader.getTextView();
                final TextSnippet snippet = fbview.getSelectedSnippet();
                if (snippet == null) {
                    return;
                }

                final String text = snippet.getText();
                fbview.clearSelection();

                final ClipboardManager clipboard =
                        (ClipboardManager) getContext().getApplicationContext().getSystemService(Application.CLIPBOARD_SERVICE);
                clipboard.setText(text);
                UIMessageUtil.showMessageText(
                        a,
                        ZLResource.resource("selection").getResource("textInBuffer").getValue().replace("%s", clipboard.getText())
                );

                if (widget instanceof ScrollView) {
                    ((ScrollView) widget).adapter.processInvalidate();
                    ((ScrollView) widget).adapter.processClear();
                }
            }
        });
        app.addAction(ActionCode.SELECTION_SHARE, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final FBView fbview = Reader.getTextView();
                final TextSnippet snippet = fbview.getSelectedSnippet();
                if (snippet == null) {
                    return;
                }

                final String text = snippet.getText();
                final String title = Reader.getCurrentBook().getTitle();
                fbview.clearSelection();

                final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        ZLResource.resource("selection").getResource("quoteFrom").getValue().replace("%s", title)
                );
                intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
                a.startActivity(Intent.createChooser(intent, null));

                if (widget instanceof ScrollView) {
                    ((ScrollView) widget).adapter.processInvalidate();
                    ((ScrollView) widget).adapter.processClear();
                }
            }
        });
        app.addAction(ActionCode.SELECTION_TRANSLATE, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final FBView fbview = Reader.getTextView();
                final DictionaryHighlighting dictionaryHilite = DictionaryHighlighting.get(fbview);
                final TextSnippet snippet = fbview.getSelectedSnippet();

                if (dictionaryHilite == null || snippet == null) {
                    return;
                }

                DictionaryUtil.openTextInDictionary(
                        a,
                        snippet.getText(),
                        fbview.getCountOfSelectedWords() == 1,
                        fbview.getSelectionStartY(),
                        fbview.getSelectionEndY(),
                        new Runnable() {
                            public void run() {
                                fbview.addHighlighting(dictionaryHilite);
                                Reader.getViewWidget().repaint();
                            }
                        }
                );
                fbview.clearSelection();
            }
        });
        app.addAction(ActionCode.SELECTION_BOOKMARK, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final Bookmark bookmark;
                if (params.length != 0) {
                    bookmark = (Bookmark) params[0];
                } else {
                    bookmark = Reader.addSelectionBookmark();
                }
                if (bookmark == null) {
                    return;
                }

                final SuperActivityToast toast =
                        new SuperActivityToast(a, SuperToast.Type.BUTTON);
                toast.setText(bookmark.getText());
                toast.setDuration(SuperToast.Duration.EXTRA_LONG);
                toast.setButtonIcon(
                        android.R.drawable.ic_menu_edit,
                        ZLResource.resource("dialog").getResource("button").getResource("edit").getValue()
                );
                toast.setOnClickWrapper(new OnClickWrapper("bkmk", new SuperToast.OnClickListener() {
                    @Override
                    public void onClick(View view, Parcelable token) {
                        final Intent intent =
                                new Intent(getContext().getApplicationContext(), EditBookmarkActivity.class);
                        FBReaderIntents.putBookmarkExtra(intent, bookmark);
                        OrientationUtil.startActivity(a, intent);
                    }
                }));
                showToast(toast);
            }
        });
        app.addAction(ActionCode.SELECTION_CLEAR, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                app.BookTextView.clearSelection();
                if (widget instanceof ScrollView) {
                    ((ScrollView) widget).adapter.processInvalidate();
                    ((ScrollView) widget).adapter.processClear();
                }
            }
        });

        app.addAction(ActionCode.FIND_PREVIOUS, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                Reader.getTextView().findPrevious();
                resetNewPosition();
            }
        });
        app.addAction(ActionCode.FIND_NEXT, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                Reader.getTextView().findNext();
                resetNewPosition();
            }
        });
        app.addAction(ActionCode.CLEAR_FIND_RESULTS, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                Reader.getTextView().clearFindResults();
                resetNewPosition();
            }
        });

        app.addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final PageTurningOptions preferences = Reader.PageTurningOptions;
                Reader.getViewWidget().startAnimatedScrolling(
                        FBView.PageIndex.next,
                        preferences.Horizontal.getValue()
                                ? FBView.Direction.rightToLeft : FBView.Direction.up,
                        preferences.AnimationSpeed.getValue()
                );
            }
        });
        app.addAction(ActionCode.VOLUME_KEY_SCROLL_BACK, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final PageTurningOptions preferences = Reader.PageTurningOptions;
                Reader.getViewWidget().startAnimatedScrolling(
                        FBView.PageIndex.previous,
                        preferences.Horizontal.getValue()
                                ? FBView.Direction.rightToLeft : FBView.Direction.up,
                        preferences.AnimationSpeed.getValue()
                );
            }
        });

        ((PopupPanel) app.getPopupById(TextSearchPopup.ID)).setPanelInfo(a, this);
        ((NavigationPopup) app.getPopupById(NavigationPopup.ID)).setPanelInfo(a, this);
        ((PopupPanel) app.getPopupById(SelectionPopup.ID)).setPanelInfo(a, this);
    }

    public void setDrawer(DrawerLayout drawer) {
        this.drawer = drawer;
    }

    void showPopup(final ZLTextHyperlink hyperlink) {
        Context context = getContext();

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        WallpaperLayout f = new WallpaperLayout(context);
        ImageButton c = new ImageButton(context);
        c.setImageResource(R.drawable.ic_close_black_24dp);
        c.setColorFilter(ThemeUtils.getThemeColor(context, R.attr.colorAccent));
        f.addView(c, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP));

        final FBReaderView r = new FBReaderView(context) {
            @Override
            public void config() {
                super.config();
                config.setValue(app.ViewOptions.ScrollbarType, 0);
                config.setValue(app.MiscOptions.WordTappingAction, MiscOptions.WordTappingActionEnum.doNothing);
                config.setValue(app.ImageOptions.TapAction, ImageOptions.TapActionEnum.doNothing);
            }
        };

        SharedPreferences shared = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
        r.configWidget(shared);

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        ll.addView(f, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ll.addView(r, rlp);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(ll);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                r.loadBook(book);
                final BookModel.Label label = r.app.Model.getLabel(hyperlink.Id);
                if (label != null) {
                    if (label.ModelId == null) {
                        r.app.BookTextView.gotoPosition(0, 0, 0);
                        r.app.setView(r.app.BookTextView);
                    } else {
                        final ZLTextModel model = r.app.Model.getFootnoteModel(label.ModelId);
                        r.app.BookTextView.setModel(model);
                        r.app.setView(r.app.BookTextView);
                        r.app.BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
                    }
                }
                r.app.tryOpenFootnote(hyperlink.Id);
            }
        });
        c.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    void showToast(SuperActivityToast toast) {
        toast.show();
    }

    void gotoPluginPosition(ZLTextPosition p) {
        if (p == null)
            return;
        if (widget instanceof ScrollView) {
            if (p.getElementIndex() != 0)
                p = new ZLTextFixedPosition(p.getParagraphIndex(), 0, 0);
        }
        pluginview.gotoPosition(p);
    }

    public void gotoPosition(TOCTree.Reference p) {
        if (p.Model != null)
            app.BookTextView.setModel(p.Model);
        gotoPosition(new ZLTextFixedPosition(p.ParagraphIndex, 0, 0));
    }

    public void gotoPosition(ZLTextPosition p) {
        if (pluginview != null)
            gotoPluginPosition(p);
        else
            app.BookTextView.gotoPosition(p);
        resetNewPosition();
    }

    public void resetNewPosition() { // get position from new loaded page, then reset
        if (widget instanceof ScrollView) {
            ((ScrollView) widget).adapter.reset();
        } else {
            widget.reset();
            widget.repaint();
        }
    }

    public void reset() { // keep current position, then reset
        if (widget instanceof ScrollView) {
            ((ScrollView) widget).updatePosition();
            ((ScrollView) widget).adapter.reset();
            ((ScrollView) widget).updateSelection();
        } else {
            widget.reset();
            widget.repaint();
        }
    }

    public void resetCaches() {
        app.clearTextCaches();
        reset();
    }

    public void invalidateFooter() {
        if (widget instanceof ScrollView) {
            ((ScrollView) widget).invalidate();
        } else {
            widget.repaint();
        }
    }

    public void clearReflowPage() {
        pluginview.current.pageOffset = 0;
        if (pluginview.reflower != null)
            pluginview.reflower.current = 0;
    }

    public boolean isPinch() {
        return pinch != null;
    }

    public void pinchOpen(int page, Rect v) {
        Bitmap bm = pluginview.render(v.width(), v.height(), page);
        pinch = new PinchView(getContext(), v, bm) {
            @Override
            public void pinchClose() {
                FBReaderView.this.pinchClose();
            }
        };
        addView(pinch);
    }

    public void pinchClose() {
        if (pinch != null) {
            removeView(pinch);
            pinch.close();
            pinch = null;
        }
    }

    public void selectionOpen(PluginView.Selection s) {
        selectionClose();
        selection = new SelectionView(getContext(), (CustomView) app.BookTextView, s) {
            @Override
            public void onTouchLock() {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            @Override
            public void onTouchUnlock() {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        };
        addView(selection);
        if (widget instanceof ScrollView) {
            ((ScrollView) widget).updateSelection();
            selection.setClipHeight(((ScrollView) widget).getMainAreaHeight());
        } else {
            selection.setClipHeight(((ZLAndroidWidget) widget).getMainAreaHeight());
        }
    }

    public void selectionClose() {
        if (widget instanceof ScrollView)
            ((ScrollView) widget).selectionClose();
        if (selection != null) {
            selection.close();
            removeView(selection);
            selection = null;
        }
    }

}
