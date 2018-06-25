package com.github.axet.bookreader.widgets;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
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

import com.github.axet.androidlibrary.widgets.AboutPreferenceCompat;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.app.ComicsPlugin;
import com.github.axet.bookreader.app.DjvuPlugin;
import com.github.axet.bookreader.app.MainApplication;
import com.github.axet.bookreader.app.PDFPlugin;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.k2pdfopt.K2PdfOpt;
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
import org.geometerplus.android.fbreader.image.ImageViewActivity;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.android.util.UIMessageUtil;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.book.IBookCollection;
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
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.util.AutoTextSnippet;
import org.geometerplus.fbreader.util.TextSnippet;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.core.options.StringPair;
import org.geometerplus.zlibrary.core.options.ZLOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.model.ZLTextMark;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.model.ZLTextParagraph;
import org.geometerplus.zlibrary.text.view.ZLTextElementAreaVector;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlinkRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextImageRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextRegion;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.ZLTextWordRegionSoul;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import java.io.IOException;
import java.util.ArrayList;
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
    GestureDetectorCompat gestures;
    PinchView pinch;

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

        public Rect toRect(int w, int h) {
            return new Rect(x, h - this.h - y, x + this.w, h - y);
        }
    }

    public static abstract class PluginPage {
        public int pageNumber;
        public int pageOffset; // pageBox sizes
        public PluginRect pageBox; // pageBox sizes
        public int w; // display w
        public int h; // display h
        public double hh; // pageBox sizes, visible height
        public double ratio;
        public int pageStep; // pageBox sizes, page step size (fullscreen height == pageStep + pageOverlap)
        public int pageOverlap; // pageBox sizes, page overlap size (fullscreen height == pageStep + pageOverlap)
        public int dpi; // pageBox dpi, set manually

        public PluginPage() {
        }

        public PluginPage(PluginPage r) {
            w = r.w;
            h = r.h;
            hh = r.hh;
            ratio = r.ratio;
            pageNumber = r.pageNumber;
            pageOffset = r.pageOffset;
            if (r.pageBox != null)
                pageBox = new PluginRect(r.pageBox);
            pageStep = r.pageStep;
            pageOverlap = r.pageOverlap;
        }

        public PluginPage(PluginPage r, ZLViewEnums.PageIndex index) {
            this(r);
            load(index);
        }

        public void renderPage() {
            ratio = pageBox.w / (double) w;
            hh = h * ratio;

            pageOverlap = (int) (hh * PAGE_OVERLAP_PERCENTS / 100);
            pageStep = (int) (hh - pageOverlap); // -5% or lowest base line
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
                renderPage();
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
                this.pageNumber = pageNumber;
                load(); // load pageBox
                renderPage(); // calculate pageStep
                int tail = pageBox.h % pageStep;
                pageOffset = pageBox.h - tail;
                if (tail <= pageOverlap)
                    pageOffset = pageOffset - pageStep; // skip tail
                this.pageOffset = pageOffset;
                return true;
            }
            this.pageOffset = pageOffset;
            return true;
        }

        public void scale(int w, int h) {
            double ratio = w / (double) pageBox.w;
            this.hh *= ratio;
            this.ratio *= ratio;
            pageBox.w = w;
            pageBox.h = (int) (pageBox.h * ratio);
            pageOffset = (int) (pageOffset * ratio);
            dpi = (int) (dpi * ratio);
        }

        public RenderRect renderRect() {
            RenderRect render = new RenderRect(); // render region

            render.x = 0;
            render.w = pageBox.w;

            if (pageOffset < 0) { // show empty space at beginig
                int tail = (int) (pageBox.h - pageOffset - hh); // tail to cut from the bottom
                if (tail < 0) {
                    render.h = pageBox.h;
                    render.y = 0;
                } else {
                    render.h = pageBox.h - tail;
                    render.y = tail;
                }
                render.dst = new Rect(0, (int) (-pageOffset / ratio), w, h);
            } else if (pageOffset == 0 && hh > pageBox.h) {  // show middle vertically
                int t = (int) ((hh - pageBox.h) / ratio / 2);
                render.h = pageBox.h;
                render.dst = new Rect(0, t, w, h - t);
            } else {
                render.h = (int) hh;
                render.y = pageBox.h - render.h - pageOffset - 1;
                if (render.y < 0) {
                    render.h += render.y;
                    h += render.y / ratio; // convert to display sizes
                    render.y = 0;
                }
                render.dst = new Rect(0, 0, w, h);
            }

            render.src = new Rect(0, 0, render.w, render.h);

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
            w = r.w;
            h = r.h;
            ratio = r.ratio;
            hh = r.hh;
            pageStep = r.pageStep;
            pageOverlap = r.pageOverlap;
        }
    }

    public static class RenderRect extends FBReaderView.PluginRect {
        public Rect src;
        public Rect dst;
    }

    public static class Reflow {
        public K2PdfOpt k2;
        public int current = 0; // current view position
        public int page = 0; // document page
        int w;
        int h;
        Context context;

        public Reflow(Context context, int w, int h, int page) {
            this.context = context;
            this.page = page;
            reset(w, h);
        }

        public void reset() {
            w = 0;
            h = 0;
            if (k2 != null) {
                k2.close();
                k2 = null;
            }
        }

        public void reset(int w, int h) {
            if (this.w != w || this.h != h) {
                SharedPreferences shared = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
                Float old = shared.getFloat(MainApplication.PREFERENCE_FONTSIZE_REFLOW, MainApplication.PREFERENCE_FONTSIZE_REFLOW_DEFAULT);
                if (k2 != null) {
                    old = k2.getFontSize();
                    k2.close();
                }
                k2 = new K2PdfOpt();
                DisplayMetrics d = context.getResources().getDisplayMetrics();
                k2.create(w, h, d.densityDpi);
                k2.setFontSize(old);
                this.w = w;
                this.h = h;
            }
        }

        public void load(Bitmap bm) {
            current = 0;
            k2.load(bm);
        }

        public void load(Bitmap bm, int page, int current) {
            this.page = page;
            this.current = current;
            k2.load(bm);
        }

        public int count() {
            if (k2 == null)
                return 0;
            return k2.getCount();
        }

        public Bitmap render(int page) {
            return k2.renderPage(page);
        }

        public boolean canScroll(ZLViewEnums.PageIndex index) {
            switch (index) {
                case previous:
                    return current > 0;
                case next:
                    return current + 1 < count();
                default:
                    return true; // current???
            }
        }

        public void onScrollingFinished(ZLViewEnums.PageIndex index) {
            switch (index) {
                case next:
                    current++;
                    break;
                case previous:
                    current--;
                    break;
            }
        }

        public void close() {
            if (k2 != null) {
                k2.close();
                k2 = null;
            }
        }
    }

    public interface PageTurningListener {
        void onScrollingFinished(ZLViewEnums.PageIndex index);
    }

    public static class PluginView {
        public Bitmap wallpaper;
        public int wallpaperColor;
        public Paint paint = new Paint();
        public PluginPage current;
        public boolean reflow = false;
        public boolean reflowDebug;
        public Reflow reflower;

        public PluginView() {
            try {
                org.geometerplus.fbreader.fbreader.FBReaderApp app = new org.geometerplus.fbreader.fbreader.FBReaderApp(Storage.systeminfo, new BookCollectionShadow());
                ZLFile wallpaper = app.BookTextView.getWallpaperFile();
                if (wallpaper != null)
                    this.wallpaper = BitmapFactory.decodeStream(wallpaper.getInputStream());
                wallpaperColor = (0xff << 24) | app.BookTextView.getBackgroundColor().intValue();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void drawWallpaper(Canvas canvas) {
            if (wallpaper != null) {
                float dx = wallpaper.getWidth();
                float dy = wallpaper.getHeight();
                for (int cw = 0; cw < canvas.getWidth() + dx; cw += dx) {
                    for (int ch = 0; ch < canvas.getHeight() + dy; ch += dy) {
                        canvas.drawBitmap(wallpaper, cw - dx, ch - dy, paint);
                    }
                }
            } else {
                canvas.drawColor(wallpaperColor);
            }
        }

        public void gotoPosition(ZLTextPosition p) {
            if (p == null)
                return;
            if (current.pageNumber != p.getParagraphIndex() || current.pageOffset != p.getElementIndex())
                current.load(p);
            if (reflower != null) {
                if (reflower.page != p.getParagraphIndex()) {
                    reflower.reset();
                    reflower.page = current.pageNumber;
                }
                reflower.current = p.getElementIndex();
            }
        }

        public boolean onScrollingFinished(ZLViewEnums.PageIndex index) {
            if (reflow && reflowDebug) {
                switch (index) {
                    case previous:
                        current.pageNumber--;
                        current.pageOffset = 0;
                        current.load();
                        break;
                    case next:
                        current.pageNumber++;
                        current.pageOffset = 0;
                        current.load();
                        break;
                }
                return false;
            }
            if (reflower != null) {
                reflower.onScrollingFinished(index);
                if (reflower.page != current.pageNumber) {
                    current.pageNumber = reflower.page;
                    current.pageOffset = 0;
                    current.load();
                }
                if (reflower.current == -1) {
                    current.pageNumber = reflower.page - 1;
                    current.pageOffset = 0;
                    current.load();
                }
                if (reflower.current >= reflower.count()) { // current points to next page +1
                    current.pageNumber = reflower.page + 1;
                    current.pageOffset = 0;
                    current.load();
                }
                return false;
            }
            PluginPage old = new PluginPage(current) {
                @Override
                public void load() {
                }

                @Override
                public int getPagesCount() {
                    return current.getPagesCount();
                }
            };
            current.load(index);
            PluginPage r;
            switch (index) {
                case previous:
                    r = new PluginPage(current, ZLViewEnums.PageIndex.next) {
                        @Override
                        public void load() {
                        }

                        @Override
                        public int getPagesCount() {
                            return current.getPagesCount();
                        }
                    };
                    break;
                case next:
                    r = new PluginPage(current, ZLViewEnums.PageIndex.previous) {
                        @Override
                        public void load() {
                        }

                        @Override
                        public int getPagesCount() {
                            return current.getPagesCount();
                        }
                    };
                    break;
                default:
                    return false;
            }
            return !old.equals(r.pageNumber, r.pageOffset); // need reset cache true/false?
        }

        public ZLTextFixedPosition getPosition() {
            return new ZLTextFixedPosition(current.pageNumber, current.pageOffset, 0);
        }

        public ZLTextFixedPosition getNextPosition() {
            if (current.w == 0 || current.h == 0)
                return null; // after reset() we do not know display size
            PluginPage next = new PluginPage(current, ZLViewEnums.PageIndex.next) {
                @Override
                public void load() {
                }

                @Override
                public int getPagesCount() {
                    return current.getPagesCount();
                }
            };
            if (current.equals(next.pageNumber, next.pageOffset))
                return null; // !canScroll()
            ZLTextFixedPosition e = new ZLTextFixedPosition(next.pageNumber, next.pageOffset, 0);
            if (e.ParagraphIndex >= next.getPagesCount())
                return null;
            return e;
        }

        public boolean canScroll(ZLView.PageIndex index) {
            if (reflower != null) {
                if (reflower.canScroll(index))
                    return true;
                switch (index) {
                    case previous:
                        if (current.pageNumber > 0)
                            return true;
                        if (current.pageNumber != reflower.page) { // only happens to 0 page of document, we need to know it reflow count
                            int render = reflower.current;
                            Bitmap bm = render(reflower.w, reflower.h, current.pageNumber); // 0 page
                            reflower.load(bm, current.pageNumber, 0);
                            bm.recycle();
                            int count = reflower.count();
                            count += render;
                            reflower.current = count;
                            return count > 0;
                        }
                        return false;
                    case next:
                        if (current.pageNumber + 1 < current.getPagesCount())
                            return true;
                        if (current.pageNumber != reflower.page) { // only happens to last page of document, we need to know it reflow count
                            int render = reflower.current - reflower.count();
                            Bitmap bm = render(reflower.w, reflower.h, current.pageNumber); // last page
                            reflower.load(bm, current.pageNumber, 0);
                            bm.recycle();
                            reflower.current = render;
                            return render + 1 < reflower.count();
                        }
                        return false;
                    default:
                        return true; // current???
                }
            }
            PluginPage r = new PluginPage(current, index) {
                @Override
                public void load() {
                }

                @Override
                public int getPagesCount() {
                    return current.getPagesCount();
                }
            };
            return !r.equals(current.pageNumber, current.pageOffset);
        }

        public ZLTextView.PagePosition pagePosition() {
            return new ZLTextView.PagePosition(current.pageNumber + 1, current.getPagesCount());
        }

        public Bitmap render(int w, int h, int page, Bitmap.Config c) {
            return null;
        }

        public Bitmap render(int w, int h, int page) {
            return render(w, h, page, Bitmap.Config.RGB_565); // reflower active, always 565
        }

        public void drawOnBitmap(Context context, Bitmap bitmap, int w, int h, ZLView.PageIndex index) {
            Canvas canvas = new Canvas(bitmap);
            drawOnCanvas(context, canvas, w, h, index);
        }

        public double getPageHeight(int w, ScrollView.ScrollAdapter.PageCursor c) {
            return -1;
        }

        public void drawOnCanvas(Context context, Canvas canvas, int w, int h, ZLView.PageIndex index) {
            drawWallpaper(canvas);
            if (reflow) {
                if (reflower == null) {
                    int page = current.pageNumber;
                    reflower = new Reflow(context, w, h, page);
                }
                Bitmap bm = null;
                reflower.reset(w, h);
                int render = reflower.current; // render reflow page index
                int page = reflower.page; // render pageNumber
                if (reflowDebug) {
                    switch (index) {
                        case previous:
                            page = current.pageNumber - 1;
                            break;
                        case next:
                            page = current.pageNumber + 1;
                            break;
                        case current:
                            break;
                    }
                    index = ZLViewEnums.PageIndex.current;
                    render = 0;
                }
                switch (index) {
                    case previous: // prev can point to many (no more then 2) pages behind, we need to walk every page manually
                        render -= 1;
                        while (render < 0) {
                            page--;
                            bm = render(w, h, page);
                            reflower.load(bm);
                            bm.recycle();
                            int count = reflower.count();
                            render = render + count;
                            reflower.page = page;
                            reflower.current = render + 1;
                        }
                        bm = reflower.render(render);
                        break;
                    case current:
                        bm = render(w, h, page);
                        if (reflowDebug) {
                            reflower.k2.setVerbose(true);
                            reflower.k2.setShowMarkedSource(true);
                        }
                        reflower.load(bm, page, render);
                        if (reflowDebug) {
                            reflower.close();
                            reflower = null;
                        } else {
                            bm.recycle();
                            bm = reflower.render(render);
                        }
                        break;
                    case next: // next can point to many (no more then 2) pages ahead, we need to walk every page manually
                        render += 1;
                        while (reflower.count() - render <= 0) {
                            page++;
                            render -= reflower.count();
                            bm = render(w, h, page);
                            reflower.load(bm, page, render - 1);
                            bm.recycle();
                        }
                        bm = reflower.render(render);
                        break;
                }
                if (bm != null) {
                    Rect src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
                    float wr = w / (float) bm.getWidth();
                    float hr = h / (float) bm.getHeight();
                    int dh = (int) (bm.getHeight() * wr);
                    int dw = (int) (bm.getWidth() * hr);
                    Rect dst;
                    if (dh > h) { // scaling width max makes it too high
                        int mid = (w - dw) / 2;
                        dst = new Rect(mid, 0, dw + mid, h); // scale it by height max and take calulated width
                    } else { // take width
                        int mid = (h - dh) / 2;
                        dst = new Rect(0, mid, w, dh + mid); // scale it by width max and take calulated height
                    }
                    canvas.drawBitmap(bm, src, dst, paint);
                    bm.recycle();
                    return;
                }
            }
            if (reflower != null) {
                reflower.close();
                reflower = null;
            }
            draw(canvas, w, h, index);
        }

        public void draw(Canvas bitmap, int w, int h, ZLView.PageIndex index, Bitmap.Config c) {
        }

        public void draw(Canvas bitmap, int w, int h, ZLView.PageIndex index) {
            try {
                draw(bitmap, w, h, index, Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError e) {
                draw(bitmap, w, h, index, Bitmap.Config.RGB_565);
            }
        }

        public void close() {
        }

        public TOCTree getCurrentTOCElement(TOCTree TOCTree) {
            TOCTree treeToSelect = null;
            for (TOCTree tree : TOCTree) {
                final TOCTree.Reference reference = tree.getReference();
                if (reference == null) {
                    continue;
                }
                if (reference.ParagraphIndex > current.pageNumber) {
                    break;
                }
                treeToSelect = tree;
            }
            return treeToSelect;
        }
    }

    public class CustomView extends FBView {
        public CustomView(FBReaderApp reader) {
            super(reader);
        }

        public void setContext() {
            final ZLAndroidPaintContext context = new ZLAndroidPaintContext(
                    app.SystemInfo,
                    new Canvas(),
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
            setContext(context);
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
                    onScaleBegin(pluginview.current.pageNumber, dst);
                }
            };
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
                pluginview.drawOnBitmap(getContext(), bitmap, getWidth(), getMainAreaHeight(), index);
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

    public static class SingleParagraphModel implements ZLTextModel {
        int index;
        ZLTextModel model;

        public SingleParagraphModel(int index, ZLTextModel m) {
            this.index = index;
            this.model = m;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getLanguage() {
            return null;
        }

        @Override
        public int getParagraphsNumber() {
            return 1;
        }

        @Override
        public ZLTextParagraph getParagraph(int index) {
            return model.getParagraph(this.index);
        }

        @Override
        public void removeAllMarks() {
        }

        @Override
        public ZLTextMark getFirstMark() {
            return null;
        }

        @Override
        public ZLTextMark getLastMark() {
            return null;
        }

        @Override
        public ZLTextMark getNextMark(ZLTextMark position) {
            return null;
        }

        @Override
        public ZLTextMark getPreviousMark(ZLTextMark position) {
            return null;
        }

        @Override
        public List<ZLTextMark> getMarks() {
            return new ArrayList<>();
        }

        @Override
        public int getTextLength(int index) {
            return model.getTextLength(this.index);
        }

        @Override
        public int findParagraphByTextLength(int length) {
            return 0;
        }

        @Override
        public int search(String text, int startIndex, int endIndex, boolean ignoreCase) {
            return 0;
        }
    }

    public class FBReaderApp extends org.geometerplus.fbreader.fbreader.FBReaderApp {
        public FBReaderApp(org.geometerplus.zlibrary.core.util.SystemInfo systemInfo, IBookCollection<Book> collection) {
            super(systemInfo, collection);
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
                FrameLayout f;
                ProgressBar progressBar;
                TextView text;
                Bitmap bm;
                PageCursor cache;
                Paint paint = new Paint();
                ZLTextElementAreaVector p;

                public PageView(Context context) {
                    super(context);
                    f = new FrameLayout(context);

                    progressBar = new ProgressBar(context) {
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
                    f.addView(progressBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                    text = new TextView(context);
                    text.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    f.addView(text, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
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
                                if (thread == null) {
                                    if (pluginview.reflower != null) {
                                        if (pluginview.reflower.page != page || pluginview.reflower.count() == 0 || pluginview.reflower.w != getWidth() || pluginview.reflower.h != getHeight()) {
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
                                                Reflow reflower = new Reflow(getContext(), getWidth(), getHeight(), page);
                                                Bitmap bm = pluginview.render(getWidth(), getHeight(), page);
                                                reflower.load(bm);
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
                                if (page == pluginview.reflower.page) {
                                    Canvas canvas = getCanvas(draw, c);
                                    pluginview.current.pageNumber = page;
                                    pluginview.reflower.current = c.start.getElementIndex();
                                    Bitmap bm = pluginview.reflower.render(c.start.getElementIndex());
                                    Rect src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
                                    Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
                                    canvas.drawBitmap(bm, src, dst, pluginview.paint);
                                    update();
                                    drawCache(draw);
                                } else {
                                    invalidate();
                                }
                            }
                            return;
                        }
                        open(c);
                        pluginview.drawOnCanvas(getContext(), draw, getWidth(), getHeight(), ZLViewEnums.PageIndex.current);
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
                        p = app.BookTextView.myCurrentPage.TextElementMap;
                        app.BookTextView.myCurrentPage.TextElementMap = new ZLTextElementAreaVector();
                        update();
                    }
                }

                void drawProgress(Canvas canvas, int page, int index) {
                    canvas.drawColor(Color.GRAY);
                    canvas.save();
                    canvas.translate(getWidth() / 2 - progressBar.getMeasuredWidth() / 2, getHeight() / 2 - progressBar.getMeasuredHeight() / 2);

                    String t = page + "." + index;
                    text.setText(t);

                    int dp60 = ThemeUtils.dp2px(getContext(), 60);
                    f.measure(MeasureSpec.makeMeasureSpec(dp60, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(dp60, MeasureSpec.EXACTLY));
                    f.layout(0, 0, dp60, dp60);
                    f.draw(canvas);

                    canvas.restore();
                }

                void recycle() {
                    if (bm != null) {
                        bm.recycle();
                        bm = null;
                    }
                    p = null;
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
                        if (widget instanceof ScrollView) {
                            pluginview.current.pageOffset = 0;
                        }
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
                        if (!cc.equals(c))
                            app.BookTextView.gotoPosition(c.start);
                    }
                }
            }

            @Override
            public PageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new PageHolder(new PageView(getContext()));
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
            PinchGesture pinch;

            Gestures(Context context) {
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
                        onScaleBegin(page, new Rect(v.getLeft(), v.getTop(), v.getWidth(), v.getHeight()));
                    }
                };
            }

            boolean open(MotionEvent e) {
                this.e = e;
                v = findView(e);
                if (v == null)
                    return false;
                int pos = v.holder.getAdapterPosition();
                if (pos == -1)
                    return false;
                ScrollAdapter.PageCursor c = adapter.pages.get(pos);
                if (v.p == null)
                    return false;
                if (!app.BookTextView.getStartCursor().samePositionAs(c.start))
                    app.BookTextView.gotoPosition(c.start);
                app.BookTextView.myCurrentPage.TextElementMap = v.p;
                x = (int) (e.getX() - v.getLeft());
                y = (int) (e.getY() - v.getTop());
                return true;
            }

            void close() {
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
                close();
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
                close();
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
                close();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (!open(e))
                    return;
                app.BookTextView.onFingerLongPress(x, y);
                app.BookTextView.onFingerReleaseAfterLongPress(x, y);
                v.invalidate();
                app.BookTextView.myCurrentPage.TextElementMap = new ZLTextElementAreaVector();
                adapter.invalidates.add(v.holder);
                close();
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
                    close();
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
                public void smoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
                    RecyclerView.SmoothScroller smoothScroller = new TopAlwaysSmoothScroller(recyclerView.getContext());
                    smoothScroller.setTargetPosition(position);
                    startSmoothScroll(smoothScroller);
                }
            };
            gestures = new GestureDetectorCompat(context, gesturesListener);

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
                if (view.getLeft() < x && view.getTop() < y && view.getRight() > x && view.getBottom() > y)
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
                ScrollView.ScrollAdapter.PageCursor cc = ((ScrollView) widget).adapter.pages.get(first);
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
                ScrollView.ScrollAdapter.PageCursor c = ((ScrollView) widget).adapter.pages.get(first);
                ((ScrollView) widget).adapter.open(c);
            }
        }

        void drawFooter(Canvas c) {
            if (app.Model != null) {
                FBView.Footer footer = app.BookTextView.getFooterArea();
                if (footer == null)
                    return;
                final ZLAndroidPaintContext context = new ZLAndroidPaintContext(
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
                final int voffset = getHeight() - footer.getHeight();
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

        public void onScaleBegin(int page, Rect v) {
            pinch = new PinchView(getContext(), page, v);
            FBReaderView.this.addView(pinch);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            scaleTouch = true;
            pinch.onScaleEnd();
            if (pinch.end < 0)
                pinchClose();
        }
    }

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

        public PinchView(Context context, int page, Rect v) {
            super(context);
            this.v = v;
            this.dst = new Rect(v);

            close = ContextCompat.getDrawable(context, R.drawable.ic_close_black_24dp);
            int closeSize = ThemeUtils.dp2px(context, 50);
            int closePadding = ThemeUtils.dp2px(context, 5);
            closeRect = new Rect(v.width() - closeSize + closePadding, closePadding, v.width() - closePadding, closeSize - closePadding);
            close.setBounds(new Rect(0, 0, closeRect.width(), closeRect.height()));
            DrawableCompat.setTint(DrawableCompat.wrap(close), Color.WHITE);
            closePaint = new Paint();
            closePaint.setStyle(Paint.Style.FILL);
            closePaint.setColor(0x33333333);

            bm = pluginview.render(v.width(), v.height(), page);
            src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
            gestures = new GestureDetectorCompat(context, this);
            gestures.setIsLongpressEnabled(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawBitmap(bm, src, dst, pluginview.paint);
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
        app = new FBReaderApp(new Storage.Info(getContext()), new BookCollectionShadow());

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
        config.setValue(app.ImageOptions.FitToScreen, FBView.ImageFitting.all);

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
            final PluginCollection pluginCollection = PluginCollection.Instance(app.SystemInfo);
            FormatPlugin plugin = Storage.getPlugin(pluginCollection, fbook);
            if (plugin instanceof PDFPlugin) {
                pluginview = new PDFPlugin.PDFiumView(BookUtil.fileByBook(fbook.book));
                BookModel Model = BookModel.createModel(fbook.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info != null)
                    gotoPluginPosition(book.info.position);
            } else if (plugin instanceof DjvuPlugin) {
                pluginview = new DjvuPlugin.DjvuView(BookUtil.fileByBook(fbook.book));
                BookModel Model = BookModel.createModel(fbook.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info != null)
                    gotoPluginPosition(book.info.position);
            } else if (plugin instanceof ComicsPlugin) {
                pluginview = new ComicsPlugin.ComicsView(BookUtil.fileByBook(fbook.book));
                BookModel Model = BookModel.createModel(fbook.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info != null)
                    gotoPluginPosition(book.info.position);
            } else {
                BookModel Model = BookModel.createModel(fbook.book, plugin);
                ZLTextHyphenator.Instance().load(fbook.book.getLanguage());
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info != null)
                    app.BookTextView.gotoPosition(book.info.position);
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
                    Reader.getTextView().hideOutline();
                    Reader.getViewWidget().repaint();
                    final String url = ((ZLTextImageRegionSoul) soul).ImageElement.URL;
                    if (url != null) {
                        try {
                            final Intent intent = new Intent();
                            intent.setClass(a, ImageViewActivity.class);
                            intent.putExtra(ImageViewActivity.URL_KEY, url);
                            intent.putExtra(
                                    ImageViewActivity.BACKGROUND_COLOR_KEY,
                                    Reader.ImageOptions.ImageViewBackground.getValue().intValue()
                            );
                            OrientationUtil.startActivity(a, intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
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
        } else {
            widget.reset();
            widget.repaint();
        }
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

    public void pinchClose() {
        if (pinch != null) {
            FBReaderView.this.removeView(pinch);
            pinch.close();
            pinch = null;
        }
    }

}
