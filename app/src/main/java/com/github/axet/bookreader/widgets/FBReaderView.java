package com.github.axet.bookreader.widgets;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.github.axet.bookreader.app.DjvuPlugin;
import com.github.axet.bookreader.app.PDFPlugin;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.djvulibre.DjvuLibre;

import org.geometerplus.android.fbreader.NavigationPopup;
import org.geometerplus.android.fbreader.SelectionPopup;
import org.geometerplus.android.fbreader.TextSearchPopup;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.fbreader.options.FooterOptions;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

public class FBReaderView extends RelativeLayout {

    public static final int PAGE_OVERLAP_PERCENTS = 5; // percents
    public static final int PAGE_PAPER_COLOR = 0x80ffffff;

    public FBReaderApp app;
    public ZLAndroidWidget widget;
    public int battery;
    public String title;
    public Window w;
    public Storage.Book book;
    public PluginView pluginview;

    public static class PluginRect {
        public int x; // lower left x
        public int y; // lower left y
        public int w; // x + w = upper right x
        public int h; // y + h = upper right y

        public PluginRect() {
        }

        public PluginRect(PluginRect r) {
            this.x = r.x;
            this.y = r.y;
            this.w = r.w;
            this.h = r.h;
        }

        public PluginRect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    public static abstract class PluginPage {
        public int pageNumber;
        public int pageOffset; // pageBox sizes
        public FBReaderView.PluginRect pageBox; // pageBox sizes
        public int pageStep; // pageBox sizes, page step size (fullscreen height == pageStep + pageOverlap)
        public int pageOverlap; // pageBox sizes, page overlap size (fullscreen height == pageStep + pageOverlap)

        public PluginPage() {
        }

        public PluginPage(PluginPage r) {
            pageNumber = r.pageNumber;
            pageOffset = r.pageOffset;
            if (r.pageBox != null)
                pageBox = new PluginRect(r.pageBox);
            pageStep = r.pageStep;
            pageOverlap = r.pageOverlap;
        }

        public void load(ZLViewEnums.PageIndex index) {
            switch (index) {
                case next:
                    next();
                    break;
                case previous:
                    prev();
                    break;
            }
        }

        public abstract void load();

        public abstract int getPagesCount();

        public boolean next() {
            int pageOffset = this.pageOffset + pageStep;
            int tail = pageBox.h - pageOffset;
            if (pageOffset >= pageBox.h || tail <= pageOverlap) {
                int pageNumber = this.pageNumber + 1;
                if (pageNumber >= getPagesCount())
                    return false;
                this.pageOffset = 0;
                this.pageNumber = pageNumber;
                load();
                return true;
            }
            this.pageOffset = pageOffset;
            return true;
        }

        public boolean prev() {
            int pageOffset = this.pageOffset - pageStep;
            if (this.pageOffset > 0 && pageOffset < 0) { // happens only on screen rotate
                this.pageOffset = pageOffset; // sync to top = 0 or keep negative offset
                return true;
            } else if (pageOffset < 0) {
                int pageNumber = this.pageNumber - 1;
                if (pageNumber < 0)
                    return false;
                int tail = pageBox.h % pageStep;
                pageOffset = pageBox.h - tail;
                if (tail <= pageOverlap)
                    pageOffset = pageOffset - pageStep; // skip tail
                this.pageOffset = pageOffset;
                this.pageNumber = pageNumber;
                load();
                return true;
            }
            this.pageOffset = pageOffset;
            return true;
        }

        public void scale(int w, int h) {
            float ratio = w / (float) pageBox.w;
            pageBox.w = w;
            pageBox.h = (int) (pageBox.h * ratio);
            pageOffset = (int) (pageOffset * ratio);
        }

        public RenderRect renderRect(int w, int h) {
            RenderRect render = new RenderRect();
            render.ratio = pageBox.w / (float) w;
            render.hh = h * render.ratio; // pageBox sizes

            pageOverlap = (int) (render.hh * PAGE_OVERLAP_PERCENTS / 100);
            pageStep = (int) (render.hh - pageOverlap); // -5% or lowest base line

            render.w = pageBox.w;
            render.h = (int) render.hh;
            render.y = pageBox.h - render.h - pageOffset - 1;
            if (render.y <= 0) {
                render.h += render.y;
                h += render.y / render.ratio;
                render.y = 0;
            }

            render.src = new Rect(0, 0, render.w, render.h);
            if (pageOffset == 0 && render.hh > pageBox.h) {
                int t = (int) ((render.hh - pageBox.h) / render.ratio / 2);
                render.dst = new Rect(0, t, w, t + h); // show middle vertically
            } else {
                render.dst = new Rect(0, 0, w, h);
            }
            return render;
        }

        public boolean equals(int n, int o) {
            return pageNumber == n && pageOffset == o;
        }

        public void load(ZLTextPosition p) {
            if (p == null) {
                load(0, 0);
            } else {
                load(p.getParagraphIndex(), p.getElementIndex());
            }
        }

        public void load(int n, int o) {
            pageNumber = n;
            pageOffset = o;
            load();
        }

        public void updatePage(PluginPage r) {
            pageStep = r.pageStep;
            pageOverlap = r.pageOverlap;
        }
    }

    public static class RenderRect extends FBReaderView.PluginRect {
        public float ratio;
        public float hh; // pageBox sizes, visible height of page
        public Rect src;
        public Rect dst;
    }

    public interface PluginView {
        boolean canScroll(ZLViewEnums.PageIndex index);

        void onScrollingFinished(ZLViewEnums.PageIndex pageIndex);

        ZLTextView.PagePosition pagePosition();

        void drawOnBitmap(Bitmap bitmap, int w, int h, ZLView.PageIndex index);

        ZLTextFixedPosition getPosition();

        void gotoPosition(ZLTextPosition position);

        void close();
    }

    public class CustomView extends FBView {

        public CustomView(FBReaderApp reader) {
            super(reader);
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
            if (pluginview != null)
                pluginview.onScrollingFinished(pageIndex);
            else
                super.onScrollingFinished(pageIndex);
        }

        @Override
        public synchronized PagePosition pagePosition() {
            if (pluginview != null)
                return pluginview.pagePosition();
            else
                return super.pagePosition();
        }
    }

    public FBReaderView(Context context) {
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
//        android:fadeScrollbars="false"
//        android:scrollbarAlwaysDrawVerticalTrack="true"
//        android:scrollbars="vertical"
        widget = new ZLAndroidWidget(getContext()) {

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
                    pluginview.drawOnBitmap(bitmap, getWidth(), getMainAreaHeight(), index);
                else
                    super.drawOnBitmap(bitmap, index);
            }
        };
        widget.setFocusable(true);
        addView(widget, new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        app = Storage.getApp(getContext());

        app.setWindow(new ZLApplicationWindow() {
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
        });
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

        app.ViewOptions.ScrollbarType.setValue(FBView.SCROLLBAR_SHOW_AS_FOOTER);
        app.ViewOptions.getFooterOptions().ShowProgress.setValue(FooterOptions.ProgressDisplayType.asPages);

        app.BookTextView = new CustomView(app);
        app.setView(app.BookTextView);
    }

    public void loadBook(Storage.Book book) {
        try {
            setEnabled(true);
            widget.setEnabled(true);
            this.book = book;
            final PluginCollection pluginCollection = PluginCollection.Instance(app.SystemInfo);
            FormatPlugin plugin = Storage.getPlugin(pluginCollection, book);
            if (plugin instanceof PDFPlugin) {
                if (Build.VERSION.SDK_INT >= 21)
                    pluginview = new PDFPlugin.PDFNativeView(book.book);
                else
                    pluginview = new PDFPlugin.PDFView(book.book);
                BookModel Model = BookModel.createModel(book.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info != null)
                    pluginview.gotoPosition(book.info.position);
            } else if (plugin instanceof DjvuPlugin) {
                pluginview = new DjvuPlugin.DjvuView(book.book);
                BookModel Model = BookModel.createModel(book.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info != null)
                    pluginview.gotoPosition(book.info.position);
            } else {
                BookModel Model = BookModel.createModel(book.book, plugin);
                ZLTextHyphenator.Instance().load(book.book.getLanguage());
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info != null)
                    app.BookTextView.gotoPosition(book.info.position);
            }
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
        widget.setEnabled(false);
        setEnabled(false);
    }

    public ZLTextFixedPosition getPosition() {
        if (pluginview != null)
            return pluginview.getPosition();
        else
            return new ZLTextFixedPosition(app.BookTextView.getStartCursor());
    }

    public void setWindow(Window w) {
        this.w = w;
        app.MiscOptions.AllowScreenBrightnessAdjustment.setValue(true);
    }

}
