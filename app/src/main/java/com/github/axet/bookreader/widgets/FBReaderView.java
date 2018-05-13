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
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.axet.androidlibrary.widgets.AboutPreferenceCompat;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.bookreader.R;
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
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.util.AutoTextSnippet;
import org.geometerplus.fbreader.util.TextSnippet;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.options.ZLBooleanOption;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.model.ZLTextMark;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.model.ZLTextParagraph;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlinkRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextImageRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextRegion;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.ZLTextWordRegionSoul;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FBReaderView extends RelativeLayout {

    public static final String ACTION_MENU = FBReaderView.class.getCanonicalName() + ".ACTION_MENU";

    public static final int PAGE_OVERLAP_PERCENTS = 5; // percents
    public static final int PAGE_PAPER_COLOR = 0x80ffffff;

    public FBReaderApp app;
    public ZLAndroidWidget widget;
    public int battery;
    public String title;
    public Window w;
    public Storage.Book book;
    public PluginView pluginview;
    public PageTurningListener pageTurningListener;

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
            k2.close();
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
            current.load(p);
            if (reflower != null) {
                reflower.reset();
                reflower.page = current.pageNumber;
                reflower.current = 0;
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
            return new ZLTextView.PagePosition(current.pageNumber, current.getPagesCount());
        }

        public Bitmap render(int w, int h, int page, Bitmap.Config c) {
            return null;
        }

        public Bitmap render(int w, int h, int page) {
            return render(w, h, page, Bitmap.Config.RGB_565); // reflower active, always 565
        }

        public void drawOnBitmap(Context context, Bitmap bitmap, int w, int h, ZLView.PageIndex index) {
            Canvas canvas = new Canvas(bitmap);
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
        public synchronized PagePosition pagePosition() {
            if (pluginview != null)
                return pluginview.pagePosition();
            else
                return super.pagePosition();
        }

        @Override
        public synchronized void gotoPage(int page) {
            if (pluginview != null)
                pluginview.gotoPosition(new ZLTextFixedPosition(page, 0, 0));
            else
                super.gotoPage(page);
        }

        @Override
        public synchronized void paint(ZLPaintContext context, PageIndex pageIndex) {
            super.paint(context, pageIndex);
        }
    }

    public class FBAndroidWidget extends ZLAndroidWidget {
        public FBAndroidWidget() {
            super(FBReaderView.this.getContext());
            ZLApplication = new ZLAndroidWidget.ZLApplicationInstance() {
                public ZLApplication Instance() {
                    return app;
                }
            };
            setFocusable(true);
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

    public static class ParagraphModel implements ZLTextModel {
        int index;
        ZLTextModel model;

        public ParagraphModel(int index, ZLTextModel m) {
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

    public FBReaderView(Context context) { // create child view
        super(context);
        create();
        app.ViewOptions.ScrollbarType = new ZLIntegerRangeOption("", "", 0, 0, 0) {
            @Override
            public int getValue() {
                return 0;
            }
        };
        app.MiscOptions.AllowScreenBrightnessAdjustment = new ZLBooleanOption("", "", false) {
            @Override
            public boolean getValue() {
                return false;
            }
        };
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
        app = new FBReaderApp(new Storage.Info(getContext()), new BookCollectionShadow());
        widget = new FBAndroidWidget();
        addView(widget, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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

        app.BookTextView = new CustomView(app);
        app.setView(app.BookTextView);

        SharedPreferences shared = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());

        int d = shared.getInt(MainApplication.PREFERENCE_FONTSIZE_FBREADER, app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption.getValue());
        app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption.setValue(d);

        String f = shared.getString(MainApplication.PREFERENCE_FONTFAMILY_FBREADER, app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption.getValue());
        app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption.setValue(f);

        app.ViewOptions.ScrollbarType = new ZLIntegerRangeOption("", "", 0, 0, 0) {
            @Override
            public int getValue() {
                return FBView.SCROLLBAR_SHOW_AS_FOOTER;
            }
        };
        app.ViewOptions.getFooterOptions().ShowProgress.setValue(FooterOptions.ProgressDisplayType.asPages);
    }

    public void loadBook(Storage.Book book) {
        try {
            this.book = book;
            final PluginCollection pluginCollection = PluginCollection.Instance(app.SystemInfo);
            FormatPlugin plugin = Storage.getPlugin(pluginCollection, book);
            if (plugin instanceof PDFPlugin) {
                pluginview = new PDFPlugin.PDFiumView(BookUtil.fileByBook(book.book));
                BookModel Model = BookModel.createModel(book.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info != null)
                    pluginview.gotoPosition(book.info.position);
            } else if (plugin instanceof DjvuPlugin) {
                pluginview = new DjvuPlugin.DjvuView(BookUtil.fileByBook(book.book));
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
    }

    public ZLTextFixedPosition getPosition() {
        if (pluginview != null)
            return pluginview.getPosition();
        else
            return new ZLTextFixedPosition(app.BookTextView.getStartCursor());
    }

    public void setWindow(Window w) {
        this.w = w;
        app.MiscOptions.AllowScreenBrightnessAdjustment = new ZLBooleanOption("", "", true) {
            @Override
            public boolean getValue() {
                return true;
            }
        };
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
                        app.MiscOptions.TextSearchPattern.setValue(pattern);
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
                    Reader.getTextView().hideOutline();
                    Reader.getViewWidget().repaint();
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
                                        Reader.getViewWidget().repaint();
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
        LinearLayout ll = new LinearLayout(getContext());
        ll.setOrientation(LinearLayout.VERTICAL);

        WallpaperLayout f = new WallpaperLayout(getContext());
        ImageButton c = new ImageButton(getContext());
        c.setImageResource(R.drawable.ic_close_black_24dp);
        c.setColorFilter(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent));
        f.addView(c, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP));

        final FBReaderView r = new FBReaderView(getContext());
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        ll.addView(f, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ll.addView(r, rlp);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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
                        r.app.BookTextView.setModel(new ParagraphModel(label.ParagraphIndex, r.app.Model.getTextModel()));
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

    public void gotoPosition(TOCTree.Reference p) {
        if (p.Model != null)
            app.BookTextView.setModel(p.Model);
        gotoPosition(new ZLTextFixedPosition(p.ParagraphIndex, 0, 0));
    }

    public void gotoPosition(ZLTextPosition p) {
        if (pluginview != null)
            pluginview.gotoPosition(p);
        else
            app.BookTextView.gotoPosition(p);
        reset();
    }


    public void setColorProfile() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (shared.getString(MainApplication.PREFERENCE_THEME, "").equals(getContext().getString(R.string.Theme_Dark))) {
            setColorProfile(ColorProfile.NIGHT);
        } else {
            setColorProfile(ColorProfile.DAY);
        }
    }

    public void setColorProfile(String p) {
        app.ViewOptions.ColorProfileName.setValue(p);
        reset();
    }

    public void reset() {
        widget.reset();
        widget.repaint();
    }
}
