package com.github.axet.bookreader.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
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

import com.github.axet.androidlibrary.net.HttpClient;
import com.github.axet.androidlibrary.widgets.AboutPreferenceCompat;
import com.github.axet.androidlibrary.widgets.PinchView;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.androidlibrary.widgets.TopAlwaysSmoothScroller;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.app.BookApplication;
import com.github.axet.bookreader.app.ComicsPlugin;
import com.github.axet.bookreader.app.DjvuPlugin;
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
import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.UIMessageUtil;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.FBHyperlinkType;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.ActionCode;
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
import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.ZLTextControlElement;
import org.geometerplus.zlibrary.text.view.ZLTextElement;
import org.geometerplus.zlibrary.text.view.ZLTextElementArea;
import org.geometerplus.zlibrary.text.view.ZLTextElementAreaVector;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextHighlighting;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlinkRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextImageElement;
import org.geometerplus.zlibrary.text.view.ZLTextImageRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextParagraphCursor;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextRegion;
import org.geometerplus.zlibrary.text.view.ZLTextSimpleHighlighting;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor;
import org.geometerplus.zlibrary.text.view.ZLTextWordRegionSoul;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
    SelectionView selection;
    ZLTextPosition scrollDelayed;
    DrawerLayout drawer;
    PluginView.Search search;
    int searchPagePending;

    public static void showControls(final ViewGroup p, final View areas) {
        p.removeCallbacks((Runnable) areas.getTag());
        p.addView(areas);
        Runnable hide = new Runnable() {
            @Override
            public void run() {
                hideControls(p, areas);
            }
        };
        areas.setTag(hide);
        p.postDelayed(hide, 3000);
    }

    public static void hideControls(final ViewGroup p, final View areas) {
        p.removeCallbacks((Runnable) areas.getTag());
        areas.setTag(null);
        if (Build.VERSION.SDK_INT >= 11) {
            ValueAnimator v = ValueAnimator.ofFloat(1f, 0f);
            v.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                @TargetApi(11)
                public void onAnimationUpdate(ValueAnimator animation) {
                    ViewCompat.setAlpha(areas, (float) animation.getAnimatedValue());
                }
            });
            v.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    p.removeView(areas);
                }
            });
            v.setDuration(500);
            v.start();
        } else {
            p.removeView(areas);
        }
    }

    public static Intent translateIntent(String text) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PROCESS_TEXT);
        intent.setType(HttpClient.CONTENTTYPE_TEXT);
        intent.setPackage("com.google.android.apps.translate"); // only known translator
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text);
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
        return intent;
    }

    public static Rect findUnion(List<ZLTextElementArea> areas, Storage.Bookmark bm) {
        Rect union = null;
        for (ZLTextElementArea a : areas) {
            if (bm.start.compareTo(a) <= 0 && bm.end.compareTo(a) >= 0) {
                Rect r = new Rect(a.XStart, a.YStart, a.XEnd, a.YEnd);
                if (union == null)
                    union = r;
                else
                    union.union(r);
            }
        }
        return union;
    }

    public static class ZLTextIndexPosition extends ZLTextFixedPosition implements Parcelable {
        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            public ZLTextIndexPosition createFromParcel(Parcel in) {
                return new ZLTextIndexPosition(in);
            }

            public ZLTextIndexPosition[] newArray(int size) {
                return new ZLTextIndexPosition[size];
            }
        };

        public ZLTextPosition end;

        static ZLTextPosition read(Parcel in) {
            int p = in.readInt();
            int e = in.readInt();
            int c = in.readInt();
            return new ZLTextFixedPosition(p, e, c);
        }

        public ZLTextIndexPosition(ZLTextPosition p, ZLTextPosition e) {
            super(p);
            end = new ZLTextFixedPosition(e);
        }

        public ZLTextIndexPosition(Parcel in) {
            super(read(in));
            end = read(in);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(getParagraphIndex());
            dest.writeInt(getElementIndex());
            dest.writeInt(getCharIndex());
            dest.writeInt(end.getParagraphIndex());
            dest.writeInt(end.getElementIndex());
            dest.writeInt(end.getCharIndex());
        }
    }

    public static class ZLBookmark extends ZLTextSimpleHighlighting {
        public FBView view;
        public Storage.Bookmark b;

        public ZLBookmark(FBView view, Storage.Bookmark b) {
            super(view, b.start, b.end);
            this.view = view;
            this.b = b;
        }

        @Override
        public ZLColor getForegroundColor() {
            return null;
        }

        @Override
        public ZLColor getBackgroundColor() {
            if (b.color != 0)
                return new ZLColor(b.color);
            return view.getHighlightingBackgroundColor();
        }

        @Override
        public ZLColor getOutlineColor() {
            return null;
        }
    }

    public interface PageTurningListener {
        void onScrollingFinished(ZLViewEnums.PageIndex index);

        void onSearchClose();

        void onBookmarksUpdate();
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

        @Override
        public Animation getAnimationType() {
            PowerManager pm = (PowerManager) FBReaderView.this.getContext().getSystemService(Context.POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= 21 && pm.isPowerSaveMode())
                return Animation.none;
            else
                return super.getAnimationType();
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
        public void onFingerSingleTap(int x, int y) {
            final ZLTextHighlighting highlighting = findHighlighting(x, y, maxSelectionDistance());
            if (highlighting instanceof ZLBookmark) {
                app.runAction(ActionCode.SELECTION_BOOKMARK, ((ZLBookmark) highlighting).b);
                return;
            }
            super.onFingerSingleTap(x, y);
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
            if (widget instanceof ZLAndroidWidget)
                ((FBAndroidWidget) widget).updateOverlays();
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

        @Override
        public int getSelectionStartY() {
            if (selection != null)
                return selection.getSelectionStartY();
            return super.getSelectionStartY();
        }

        @Override
        public int getSelectionEndY() {
            if (selection != null)
                return selection.getSelectionEndY();
            return super.getSelectionEndY();
        }
    }

    public class FBAndroidWidget extends ZLAndroidWidget {
        PinchGesture pinch;
        BrightnessGesture brightness;

        int x;
        int y;

        ZLTextPosition selectionPage;

        ReflowMap<Reflow.Info> infos = new ReflowMap<>();
        ReflowMap<LinksView> links = new ReflowMap<>();
        ReflowMap<BookmarksView> bookmarks = new ReflowMap<>();
        ReflowMap<SearchView> searchs = new ReflowMap<>();

        public class ReflowMap<V> extends HashMap<ZLTextPosition, V> {
            ArrayList<ZLTextPosition> last = new ArrayList<>();

            @Override
            public V put(ZLTextPosition key, V value) {
                V v = super.put(key, value);
                if (pluginview.reflower != null) {
                    int l = pluginview.reflower.emptyCount() - 1;
                    if (key.getElementIndex() == l) {
                        ZLTextFixedPosition n = new ZLTextFixedPosition(key.getParagraphIndex() + 1, -1, 0);
                        super.put(n, value); // ignore result, duplicate key for same value
                        last.add(n); // (3,-1,0) == (2,2,0) when (2,1,0) is last

                        ZLTextPosition k = new ZLTextFixedPosition(key.getParagraphIndex(), l + 1, 0);
                        V kv = get(new ZLTextFixedPosition(key.getParagraphIndex() + 1, 0, 0));
                        super.put(k, kv); // ignore result, duplicate key for same value
                        last.add(k); // (2,2,0) == (3,0,0) when (2,1,0) is last
                    }
                    if (key.getElementIndex() == 0) {
                        int p = key.getParagraphIndex() - 1;
                        for (ZLTextPosition k : keySet()) {
                            if (k.getParagraphIndex() == p && get(k) == null) {
                                super.put(k, value); // update (2,3,0) == (3,0,0)
                            }
                        }
                    }
                }
                if (v != null)
                    return v;
                last.add(key);
                if (last.size() > 9) { // number of possible old values + dups
                    ZLTextPosition k = last.remove(0);
                    return remove(k);
                }
                return null;
            }

            @Override
            public void clear() {
                super.clear();
                last.clear();
            }
        }

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
                pinch = new PinchGesture(getContext()) {
                    @Override
                    public void onScaleBegin(float x, float y) {
                        pinchOpen(pluginview.current.pageNumber, getPageRect());
                    }
                };
            }

            brightness = new BrightnessGesture(getContext());
        }

        public Rect getPageRect() {
            Rect dst;
            if (pluginview.reflow) {
                dst = new Rect(0, 0, getWidth(), getHeight());
            } else {
                PluginPage p = pluginview.current; // not using current.renderRect() show partial page
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
            }
            return dst;
        }

        @Override
        public void setScreenBrightness(int percent) {
            myColorLevel = brightness.setScreenBrightness(percent);
            postInvalidate();
            updateColorLevel();
        }

        @Override
        public int getScreenBrightness() {
            return brightness.getScreenBrightness();
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
            if (pluginview != null) {
                pluginview.drawOnBitmap(getContext(), bitmap, getWidth(), getMainAreaHeight(), index, (CustomView) app.BookTextView, book.info);
                Reflow.Info info = null;
                ZLTextPosition position;
                if (pluginview.reflow) {
                    position = new ZLTextFixedPosition(pluginview.reflower.page, pluginview.reflower.index + pluginview.reflower.pending, 0);
                    info = new Reflow.Info(pluginview.reflower, position.getElementIndex());
                    infos.put(position, info);
                } else {
                    PluginPage old = new PluginPage(pluginview.current) {
                        @Override
                        public void load() {
                        }

                        @Override
                        public int getPagesCount() {
                            return pluginview.current.getPagesCount();
                        }
                    };
                    old.load(index);
                    position = new ZLTextFixedPosition(old.pageNumber, 0, 0);
                }
                Rect dst = getPageRect();
                PluginView.Selection.Page page = pluginview.selectPage(position, info, dst.width(), dst.height());
                LinksView l = new LinksView(pluginview.getLinks(page), info);
                LinksView old = links.put(position, l);
                if (old != null)
                    old.close();
                BookmarksView b = new BookmarksView(page, book.info.bookmarks, info);
                BookmarksView bold = bookmarks.put(position, b);
                if (bold != null)
                    bold.close();
                if (search != null) {
                    SearchView s = new SearchView(search.getBounds(page), info);
                    SearchView sold = searchs.put(position, s);
                    if (sold != null)
                        sold.close();
                }
            } else {
                super.drawOnBitmap(bitmap, index);
            }
        }

        public void updateOverlaysReset() {
            updateOverlays();
            resetCache(); // need to drawonbitmap
        }

        ZLTextFixedPosition getPosition() {
            if (pluginview.reflow)
                return new ZLTextFixedPosition(pluginview.reflower.page, pluginview.reflower.index, 0);
            else
                return new ZLTextFixedPosition(pluginview.current.pageNumber, 0, 0);
        }

        public void updateOverlays() {
            if (pluginview != null) {
                final Rect dst = getPageRect();
                int x = dst.left;
                int y = dst.top;
                if (pluginview.reflow)
                    x += getInfo().margin.left;

                ZLTextPosition position = getPosition();

                for (LinksView l : links.values()) {
                    if (l != null)
                        l.hide();
                }
                LinksView l = links.get(position);
                if (l != null) {
                    l.show();
                    l.update(x, y);
                }

                for (BookmarksView b : bookmarks.values()) {
                    if (b != null)
                        b.hide();
                }
                BookmarksView b = bookmarks.get(position);
                if (b != null) {
                    b.show();
                    b.update(x, y);
                }

                for (SearchView s : searchs.values()) {
                    if (s != null)
                        s.hide();
                }
                SearchView s = searchs.get(position);
                if (s != null) {
                    s.show();
                    s.update(x, y);
                }

                if (selectionPage != null && !selectionPage.samePositionAs(position)) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            selectionClose();
                        }
                    });
                    selectionPage = null;
                }
            }
        }

        public void linksClose() {
            for (LinksView l : links.values()) {
                if (l != null)
                    l.close();
            }
            links.clear();
        }

        public void bookmarksClose() {
            for (BookmarksView l : bookmarks.values()) {
                if (l != null)
                    l.close();
            }
            bookmarks.clear();
        }

        public void searchClose() {
            for (SearchView l : searchs.values()) {
                if (l != null)
                    l.close();
            }
            searchs.clear();
        }

        @SuppressWarnings("unchecked")
        public void searchPage(int page) {
            int w = getWidth();
            int h = getMainAreaHeight();

            pluginview.current.w = w;
            pluginview.current.h = h;
            pluginview.current.load(page, 0);
            pluginview.current.renderPage();

            Rect dst = getPageRect();

            if (pluginview.reflow) {
                if (pluginview.reflower != null && (pluginview.reflower.page != page || pluginview.reflower.w != w || pluginview.reflower.h != h)) {
                    pluginview.reflower.close();
                    pluginview.reflower = null;
                }
                if (pluginview.reflower == null) {
                    pluginview.reflower = new Reflow(getContext(), w, h, page, (CustomView) app.BookTextView, book.info);
                    Bitmap bm = pluginview.render(pluginview.reflower.rw, pluginview.reflower.h, page);
                    pluginview.reflower.load(bm, page, 0);
                }
                for (int i = 0; i < pluginview.reflower.count(); i++) {
                    Reflow.Info info = new Reflow.Info(pluginview.reflower, i);
                    ZLTextPosition pos = new ZLTextFixedPosition(page, i, 0);
                    PluginView.Selection.Page p = pluginview.selectPage(pos, info, pluginview.reflower.w, pluginview.reflower.h);
                    PluginView.Search.Bounds bb = search.getBounds(p);
                    if (bb.rr != null) {
                        bb.rr = pluginview.boundsUpdate(bb.rr, info);
                        if (bb.highlight != null) {
                            HashSet hh = new HashSet(Arrays.asList(pluginview.boundsUpdate(bb.highlight, info)));
                            for (Rect r : bb.rr) {
                                if (hh.contains(r)) {
                                    pluginview.gotoPosition(new ZLTextFixedPosition(page, i, 0));
                                }
                            }
                        }
                    }
                }
                resetCache();
            } else {
                ZLTextPosition pos = new ZLTextFixedPosition(page, 0, 0);
                PluginView.Selection.Page p = pluginview.selectPage(pos, getInfo(), dst.width(), dst.height());
                PluginView.Search.Bounds bb = search.getBounds(p);
                if (bb.rr != null) {
                    if (bb.highlight != null) {
                        HashSet hh = new HashSet(Arrays.asList(bb.highlight));
                        for (Rect r : bb.rr) {
                            if (hh.contains(r)) {
                                int offset = 0;
                                int t = r.top + dst.top;
                                int b = r.bottom + dst.top;
                                while (t - offset / pluginview.current.ratio > getBottom() || b - offset / pluginview.current.ratio > getBottom() && r.height() < getMainAreaHeight()) {
                                    offset += pluginview.current.pageStep;
                                }
                                pluginview.gotoPosition(new ZLTextFixedPosition(page, offset, 0));
                                resetCache();
                                return;
                            }
                        }
                    }
                }
            }
        }

        public void resetCache() { // do not reset reflower
            super.reset();
            repaint();
        }

        @Override
        public void reset() {
            super.reset();
            if (pluginview != null) {
                if (pluginview.reflower != null) {
                    pluginview.reflower.reset();
                }
            }
            infos.clear();
            linksClose();
            bookmarksClose();
            searchClose();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            x = (int) event.getX();
            y = (int) event.getY();
            if (pluginview != null && !pluginview.reflow) {
                if (pinch.onTouchEvent(event))
                    return true;
            }
            return super.onTouchEvent(event);
        }

        Reflow.Info getInfo() {
            if (pluginview.reflower == null)
                return null;
            return infos.get(new ZLTextFixedPosition(pluginview.reflower.page, pluginview.reflower.index, 0));
        }

        @Override
        public boolean onLongClick(View v) {
            if (pluginview != null) {
                final Rect dst = getPageRect();
                ZLTextPosition pos = getPosition();
                final PluginView.Selection s = pluginview.select(pos, getInfo(), dst.width(), dst.height(), x - dst.left, y - dst.top);
                if (s != null) {
                    selectionPage = pos;
                    selectionOpen(s);
                    final PluginView.Selection.Page page = pluginview.selectPage(pos, getInfo(), dst.width(), dst.height());
                    final Runnable run = new Runnable() {
                        @Override
                        public void run() {
                            int x = dst.left;
                            int y = dst.top;
                            if (pluginview.reflow)
                                x += getInfo().margin.left;
                            selection.update((SelectionView.PageView) selection.getChildAt(0), x, y);
                        }
                    };
                    PluginView.Selection.Setter setter = new PluginView.Selection.Setter() {
                        @Override
                        public void setStart(int x, int y) {
                            PluginView.Selection.Point point = pluginview.selectPoint(getInfo(), x - dst.left, y - dst.top);
                            if (point != null)
                                s.setStart(page, point);
                            run.run();
                        }

                        @Override
                        public void setEnd(int x, int y) {
                            PluginView.Selection.Point point = pluginview.selectPoint(getInfo(), x - dst.left, y - dst.top);
                            if (point != null)
                                s.setEnd(page, point);
                            run.run();
                        }

                        @Override
                        public PluginView.Selection.Bounds getBounds() {
                            PluginView.Selection.Bounds bounds = s.getBounds(page);
                            if (pluginview.reflow) {
                                bounds.rr = pluginview.boundsUpdate(bounds.rr, getInfo());
                                bounds.start = true;
                                bounds.end = true;
                            }
                            return bounds;
                        }
                    };
                    SelectionView.PageView view = new SelectionView.PageView(getContext(), (CustomView) app.BookTextView, setter);
                    selection.add(view);
                    run.run();
                    return true;
                }
                selectionClose();
            }
            return super.onLongClick(v);
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
            if (widget instanceof FBAndroidWidget)
                ((FBAndroidWidget) widget).updateOverlays();
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
            ArrayList<PageHolder> holders = new ArrayList<>(); // keep all active holders, including Recycler.mCachedViews
            ZLTextPosition oldTurn; // last page shown

            public class PageView extends View {
                public PageHolder holder;
                TimeAnimatorCompat time;
                FrameLayout progress;
                ProgressBar progressBar;
                TextView progressText;
                Bitmap bm; // cache bitmap
                PageCursor cache; // cache cursor

                ZLTextElementAreaVector text;
                Reflow.Info info;
                SelectionView.PageView selection;
                LinksView links;
                BookmarksView bookmarks;
                SearchView search;

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
                                                    i = reflower.emptyCount() + i;
                                                    c.start = new ZLTextFixedPosition(page, i, 0);
                                                }
                                                reflower.index = i;
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
                                Canvas canvas = getCanvas(c);
                                pluginview.current.pageNumber = page;
                                pluginview.reflower.index = c.start.getElementIndex();
                                if (pluginview.reflower.count() > 0) {
                                    Bitmap bm = pluginview.reflower.render(c.start.getElementIndex());
                                    Rect src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
                                    Rect dst = new Rect(app.BookTextView.getLeftMargin(), 0, app.BookTextView.getLeftMargin() + pluginview.reflower.rw, pluginview.reflower.h);
                                    canvas.drawColor(Color.WHITE); // cache color always white
                                    canvas.drawBitmap(bm, src, dst, null); // cache paint always clean
                                    info = new Reflow.Info(pluginview.reflower, c.start.getElementIndex());
                                } else { // empty source page?
                                    pluginview.drawWallpaper(canvas);
                                    pluginview.drawPage(canvas, w, h, pluginview.reflower.bm);
                                }
                                update();
                                drawCache(draw);
                                onReflowerDone();
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
                    if (links != null) {
                        links.close();
                        links = null;
                    }
                    if (bookmarks != null) {
                        bookmarks.close();
                        bookmarks = null;
                    }
                    if (search != null) {
                        search.close();
                        search = null;
                    }
                    selection = null;
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
                    draw.drawBitmap(bm, src, dst, pluginview.paint);
                }

                Canvas getCanvas(PageCursor c) {
                    if (bm != null)
                        recycle();
                    bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
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

                @Override
                public String toString() {
                    String str = "";
                    String format = "[%d,%d,%d]";
                    if (start == null)
                        str += "- ";
                    else
                        str += String.format(format, start.getParagraphIndex(), start.getElementIndex(), start.getCharIndex());
                    if (end == null)
                        str += " -";
                    else {
                        if (start != null)
                            str += " - ";
                        str += String.format(format, end.getParagraphIndex(), end.getElementIndex(), end.getCharIndex());
                    }
                    return str;
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

            public int findPos(ZLTextPosition p) {
                for (int i = 0; i < pages.size(); i++) {
                    PageCursor c = pages.get(i);
                    if (c.start != null && c.start.samePositionAs(p))
                        return i;
                }
                return -1;
            }

            public void loadPages(Reflow reflow) {
                if (pages.size() == 0) {
                    int last = reflow.count() - 1;
                    for (int i = 0; i <= last; i++) {
                        ZLTextPosition pos = new ZLTextFixedPosition(reflow.page, i, 0);
                        ZLTextPosition end;
                        if (i == last)
                            end = null;
                        else
                            end = new ZLTextFixedPosition(reflow.page, i + 1, 0);
                        pages.add(new PageCursor(pos, end));
                        notifyItemInserted(i);
                    }
                }
                ZLTextPosition prev = new ZLTextFixedPosition(reflow.page - 1, 0, 0);
                ZLTextPosition start = new ZLTextFixedPosition(reflow.page, 0, 0);
                ZLTextPosition next = new ZLTextFixedPosition(reflow.page + 1, 0, 0);
                for (int i = 0; i < pages.size(); i++) {
                    PageCursor c = pages.get(i);
                    boolean startTest = c.start != null && c.start.samePositionAs(start);
                    boolean prevTest = c.start != null && c.end != null && c.start.getParagraphIndex() == prev.getParagraphIndex() && i == (pages.size() - 1);
                    if (startTest || prevTest) { // update/add next reflow.count pages
                        int last = reflow.count() - 1;
                        for (int k = 0; k <= last; k++, i++) {
                            if (i >= pages.size()) {
                                c = new PageCursor(null, null);
                                pages.add(c);
                                notifyItemInserted(i);
                            } else {
                                c = pages.get(i);
                            }
                            ZLTextPosition pos = new ZLTextFixedPosition(reflow.page, k, 0);
                            ZLTextPosition pos2;
                            if (k == last)
                                pos2 = null;
                            else
                                pos2 = new ZLTextFixedPosition(reflow.page, k + 1, 0);
                            c.update(new PageCursor(pos, pos2));
                        }
                        return;
                    }
                    if (c.start != null && c.start.samePositionAs(next)) { // update/add prev reflow.count pages
                        i--;
                        int last = reflow.count() - 1;
                        for (int k = last; k >= 0; k--, i--) {
                            if (i < 0) {
                                c = new PageCursor(null, null);
                                pages.add(0, c);
                                notifyItemInserted(i);
                            } else {
                                c = pages.get(i);
                            }
                            ZLTextPosition pos = new ZLTextFixedPosition(reflow.page, k, 0);
                            ZLTextPosition pos2;
                            if (k == last)
                                pos2 = new ZLTextFixedPosition(start);
                            else
                                pos2 = new ZLTextFixedPosition(reflow.page, k + 1, 0);
                            c.update(new PageCursor(pos, pos2));
                        }
                        return;
                    }
                }
                throw new RuntimeException("unable to load reflower");
            }

            @Override
            public PageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new PageHolder(new PageView(parent));
            }

            @Override
            public void onBindViewHolder(PageHolder holder, int position) {
                holder.page.holder = holder;
                holders.add(holder);
            }

            @Override
            public void onViewRecycled(PageHolder holder) {
                super.onViewRecycled(holder);
                holder.page.recycle();
                holder.page.holder = null;
                holders.remove(holder);
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
                    oldTurn = c.start;
                }
                postInvalidate();
                notifyDataSetChanged();
            }

            PageCursor getCurrent() {
                if (pluginview != null) {
                    if (pluginview.reflow) {
                        if (pluginview.reflower != null) {
                            ZLTextFixedPosition s = new ZLTextFixedPosition(pluginview.reflower.page, pluginview.reflower.index, 0);
                            ZLTextFixedPosition e;
                            int index = s.ElementIndex + 1;
                            if (pluginview.reflower.count() == -1) {
                                e = null;
                            } else if (index >= pluginview.reflower.count()) { // current points to next page +1
                                e = new ZLTextFixedPosition(pluginview.reflower.page + 1, 0, 0);
                            } else {
                                e = new ZLTextFixedPosition(s.ParagraphIndex, index, 0);
                            }
                            return new PageCursor(s, e);
                        } else {
                            return new PageCursor(new ZLTextFixedPosition(pluginview.current.pageNumber, 0, 0), null);
                        }
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
            BrightnessGesture brightness;

            Gestures(Context context) {
                gestures = new GestureDetectorCompat(context, this);
                brightness = new BrightnessGesture(context);

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

            public boolean onReleaseCheck(MotionEvent e) {
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

            public boolean onCancelCheck(MotionEvent e) {
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
                onReleaseCheck(e);
                onCancelCheck(e);
                if (brightness.onTouchEvent(e))
                    return true;
                if (gestures.onTouchEvent(e))
                    return true;
                if (onFilter(e))
                    return true;
                return false;
            }
        }

        public ScrollView(final Context context) {
            super(context);

            lm = new LinearLayoutManager(context) {
                @Override
                public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
                    int off = super.scrollVerticallyBy(dy, recycler, state);
                    if (pluginview != null)
                        updateOverlays();
                    return off;
                }

                @Override
                public void smoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (Build.VERSION.SDK_INT >= 21 && pm.isPowerSaveMode()) {
                        scrollToPositionWithOffset(position, 0);
                    } else {
                        RecyclerView.SmoothScroller smoothScroller = new TopAlwaysSmoothScroller(recyclerView.getContext());
                        smoothScroller.setTargetPosition(position);
                        startSmoothScroll(smoothScroller);
                    }
                }

                @Override
                public void onLayoutCompleted(State state) {
                    super.onLayoutCompleted(state);
                    if (pluginview != null)
                        updateOverlays();
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
            for (ScrollAdapter.PageHolder h : adapter.holders) {
                ScrollAdapter.PageView view = h.page;
                if (view.text != null && view.text.getRegion(soul) != null)
                    return view;
            }
            return null;
        }

        public Rect findUnion(Storage.Bookmark bm) {
            Rect union = null;
            for (int i = 0; i < lm.getChildCount(); i++) {
                ScrollAdapter.PageView view = (ScrollAdapter.PageView) lm.getChildAt(i);
                if (view.text != null) {
                    Rect r = FBReaderView.findUnion(view.text.areas(), bm);
                    if (r != null) {
                        r.offset(view.getLeft(), view.getTop());
                        if (union == null)
                            union = r;
                        else
                            union.union(r);
                    }
                }
            }
            return union;
        }

        @Override
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
            gesturesListener.pinch.pinchClose();
        }

        @Override
        public void startAnimatedScrolling(int x, int y, int speed) {
        }

        @Override
        public void setScreenBrightness(int percent) {
            gesturesListener.brightness.setScreenBrightness(percent);
            postInvalidate();
        }

        @Override
        public int getScreenBrightness() {
            return gesturesListener.brightness.getScreenBrightness();
        }

        @Override
        public void onDraw(Canvas c) {
            super.onDraw(c);
        }

        @Override
        public void draw(Canvas c) {
            if (adapter.size.w != getWidth() || adapter.size.h != getHeight()) { // reset for textbook and reflow mode only
                adapter.reset();
                gesturesListener.pinch.pinchClose();
            }
            super.draw(c);
            updatePosition();
            drawFooter(c);
        }

        void updatePosition() { // position can vary depend on which page drawn, restore it after every draw
            int first = findFirstPage();
            if (first == -1)
                return;

            ScrollView.ScrollAdapter.PageCursor c = adapter.pages.get(first);

            ZLTextPosition pos = c.start;
            if (pos == null)
                pos = c.end;

            if (pluginview != null && pluginview.reflow) {
                if (c.start == null) {
                    int p = c.end.getParagraphIndex();
                    int i = c.end.getElementIndex() - 1;
                    if (i < 0)
                        p = p - 1;
                    pluginview.current.pageNumber = p;
                } else {
                    pluginview.current.pageNumber = c.start.getParagraphIndex();
                }
                clearReflowPage(); // reset reflow page, since we treat pageOffset differently for reflower/full page view
            } else {
                adapter.open(c);
                if (scrollDelayed != null) {
                    if (pluginview != null) {
                        PluginPage info = pluginview.getPageInfo(getWidth(), getHeight(), c);
                        for (ScrollAdapter.PageCursor p : adapter.pages) {
                            if (p.start != null && p.start.getParagraphIndex() == scrollDelayed.getParagraphIndex()) {
                                if (scrollDelayed instanceof ZLTextIndexPosition) {
                                    PluginView.Selection s = pluginview.select(scrollDelayed, ((ZLTextIndexPosition) scrollDelayed).end);
                                    PluginView.Selection.Page page = pluginview.selectPage(scrollDelayed, null, info.w, info.h);
                                    PluginView.Selection.Bounds bb = s.getBounds(page);
                                    s.close();
                                    Rect union = SelectionView.union(Arrays.asList(bb.rr));
                                    int offset = union.top;
                                    scrollBy(0, offset);
                                    adapter.oldTurn = pos;
                                } else {
                                    int offset = (int) (scrollDelayed.getElementIndex() / info.ratio);
                                    scrollBy(0, offset);
                                    adapter.oldTurn = pos;
                                }
                                scrollDelayed = null;
                                break;
                            }
                        }
                    } else {
                        gotoPosition(scrollDelayed);
                        adapter.oldTurn = pos;
                        scrollDelayed = null;
                    }
                }
            }
            if (!pos.equals(adapter.oldTurn)) {
                if (pageTurningListener != null)
                    pageTurningListener.onScrollingFinished(ZLViewEnums.PageIndex.current);
                adapter.oldTurn = pos;
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
            final ZLView.FooterArea footer = app.BookTextView.getFooterArea();
            return footer != null ? getHeight() - footer.getHeight() : getHeight();
        }

        public void onReflowerDone() {
            if (search != null) {
                if (searchPagePending != -1) {
                    final int p = searchPagePending;
                    post(new Runnable() {
                        @Override
                        public void run() {
                            searchPage(p);
                        }
                    });
                    searchPagePending = -1;
                }
            }
            if (selection != null) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateOverlays();
                    }
                });
            }
            if (scrollDelayed != null) {
                adapter.loadPages(pluginview.reflower);
                for (int i = 0; i < adapter.pages.size(); i++) {
                    ScrollAdapter.PageCursor c = adapter.pages.get(i);
                    PluginPage pinfo = pluginview.getPageInfo(getWidth(), getHeight(), c);
                    if (c.start != null && c.start.getParagraphIndex() == scrollDelayed.getParagraphIndex()) {
                        Reflow.Info info = new Reflow.Info(pluginview.reflower, c.start.getElementIndex());
                        double ratio = info.bm.width() / (double) getWidth();
                        ArrayList<Rect> ss = new ArrayList<>(info.src.keySet());
                        Collections.sort(ss, new SelectionView.UL());
                        int offset;
                        if (scrollDelayed instanceof ZLTextIndexPosition) {
                            PluginView.Selection s = pluginview.select(scrollDelayed, ((ZLTextIndexPosition) scrollDelayed).end);
                            PluginView.Selection.Page page = pluginview.selectPage(scrollDelayed, info, pinfo.w, pinfo.h);
                            PluginView.Selection.Bounds bb = s.getBounds(page);
                            s.close();
                            Rect union = SelectionView.union(Arrays.asList(bb.rr));
                            offset = union.top;
                        } else {
                            offset = (int) (scrollDelayed.getElementIndex() / pinfo.ratio * ratio);
                        }
                        for (Rect s : ss) {
                            if (s.top <= offset && s.bottom >= offset || s.top > offset) {
                                scrollToPosition(i);
                                int screen = (int) ((s.top - offset) / ratio);
                                int off = info.src.get(s).top - screen;
                                scrollBy(0, off);
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateOverlays();
                                    }
                                });
                                adapter.oldTurn = new ZLTextFixedPosition(c.start);
                                scrollDelayed = null;
                                return;
                            }
                        }
                    }
                }
            }
        }

        public void overlayRemove(ScrollAdapter.PageView view) {
            selectionRemove(view);
            linksRemove(view);
            searchRemove(view);
        }

        public void overlaysClose() {
            for (ScrollAdapter.PageHolder h : adapter.holders) {
                overlayRemove(h.page);
            }
        }

        public void updateOverlays() {
            for (ScrollAdapter.PageHolder h : adapter.holders) {
                overlayUpdate(h.page);
            }
        }

        public void overlayUpdate(ScrollAdapter.PageView view) {
            if (selection != null)
                selectionUpdate(view);
            linksUpdate(view);
            bookmarksUpdate(view);
            if (search != null)
                searchUpdate(view);
        }

        public void linksClose() {
            for (ScrollAdapter.PageHolder h : adapter.holders) {
                linksRemove(h.page);
            }
        }

        public void linksRemove(ScrollAdapter.PageView view) {
            if (view.links == null)
                return;
            view.links.close();
            view.links = null;
        }

        public void linksUpdate(ScrollAdapter.PageView view) {
            int pos = view.holder.getAdapterPosition();
            if (pos == -1) {
                linksRemove(view);
            } else {
                ScrollAdapter.PageCursor c = adapter.pages.get(pos);

                final PluginView.Selection.Page page;

                if (c.start == null || c.end == null) {
                    page = null;
                } else {
                    page = pluginview.selectPage(c.start, view.info, view.getWidth(), view.getHeight());
                }

                if (page != null && (!pluginview.reflow || view.info != null) && view.getParent() != null) { // cached views has no parrent
                    if (view.links == null)
                        view.links = new LinksView(pluginview.getLinks(page), view.info);
                    int x = view.getLeft();
                    int y = view.getTop();
                    if (view.info != null)
                        x += view.info.margin.left;
                    view.links.update(x, y);
                } else {
                    linksRemove(view);
                }
            }
        }

        public void bookmarksClose() {
            for (ScrollAdapter.PageHolder h : adapter.holders) {
                bookmarksRemove(h.page);
            }
        }

        public void bookmarksRemove(ScrollAdapter.PageView view) {
            if (view.bookmarks == null)
                return;
            view.bookmarks.close();
            view.bookmarks = null;
        }

        public void bookmarksUpdate(ScrollAdapter.PageView view) {
            int pos = view.holder.getAdapterPosition();
            if (pos == -1) {
                bookmarksRemove(view);
            } else {
                ScrollAdapter.PageCursor c = adapter.pages.get(pos);

                final PluginView.Selection.Page page;

                if (c.start == null || c.end == null) {
                    page = null;
                } else {
                    page = pluginview.selectPage(c.start, view.info, view.getWidth(), view.getHeight());
                }

                if (page != null && (!pluginview.reflow || view.info != null) && view.getParent() != null) { // cached views has no parrent
                    if (view.bookmarks == null)
                        view.bookmarks = new BookmarksView(page, book.info.bookmarks, view.info);
                    int x = view.getLeft();
                    int y = view.getTop();
                    if (view.info != null)
                        x += view.info.margin.left;
                    view.bookmarks.update(x, y);
                } else {
                    bookmarksRemove(view);
                }
            }
        }

        public void bookmarksUpdate() {
            for (ScrollAdapter.PageHolder h : adapter.holders) {
                bookmarksRemove(h.page);
                bookmarksUpdate(h.page);
            }
        }

        @SuppressWarnings("unchecked")
        public void searchPage(int page) {
            if (pluginview.reflow) {
                if (pluginview.reflower != null && pluginview.reflower.page == page) {
                    for (int i = 0; i < pluginview.reflower.count(); i++) {
                        Reflow.Info info = new Reflow.Info(pluginview.reflower, i);
                        ZLTextPosition pos = new ZLTextFixedPosition(page, i, 0);
                        PluginView.Selection.Page p = pluginview.selectPage(pos, info, pluginview.reflower.w, pluginview.reflower.h);
                        PluginView.Search.Bounds bb = search.getBounds(p);
                        if (bb.rr != null) {
                            bb.rr = pluginview.boundsUpdate(bb.rr, info);
                            if (bb.highlight != null) {
                                HashSet hh = new HashSet(Arrays.asList(pluginview.boundsUpdate(bb.highlight, info)));
                                for (Rect r : bb.rr) {
                                    if (hh.contains(r)) {
                                        adapter.loadPages(pluginview.reflower);
                                        final int pp = adapter.findPos(pos);
                                        if (pp != -1) {
                                            smoothScrollToPosition(pp);
                                            searchClose(); // remove all SearchView
                                            updateOverlays();
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    searchClose(); // remove all SearchView
                    updateOverlays();
                    return; // reflow missing for symbol (treated as image)
                }
                searchPagePending = page;
                pluginview.gotoPosition(new ZLTextFixedPosition(page, 0, 0));
                resetNewPosition();
            } else {
                for (ScrollAdapter.PageHolder holder : adapter.holders) {
                    int pos = holder.getAdapterPosition();
                    if (pos != -1) {
                        ScrollAdapter.PageCursor c = adapter.pages.get(pos);
                        if (c.start != null && c.start.getParagraphIndex() == page) {
                            PluginView.Selection.Page p = pluginview.selectPage(c.start, holder.page.info, holder.page.getWidth(), holder.page.getHeight());
                            PluginView.Search.Bounds bb = search.getBounds(p);
                            if (bb.rr != null) {
                                if (bb.highlight != null) {
                                    HashSet hh = new HashSet(Arrays.asList(bb.highlight));
                                    for (Rect r : bb.rr) {
                                        if (hh.contains(r)) {
                                            int h = getMainAreaHeight();
                                            int bottom = getTop() + h;
                                            int y = r.top + holder.page.getTop();
                                            if (y > bottom) {
                                                int dy = y - bottom;
                                                int pages = dy / getHeight() + 1;
                                                smoothScrollBy(0, pages * h);
                                            } else {
                                                y = r.bottom + holder.page.getTop();
                                                if (y > bottom) {
                                                    int dy = y - bottom;
                                                    smoothScrollBy(0, dy);
                                                }
                                            }
                                            y = r.bottom + holder.page.getTop();
                                            if (y < getTop()) {
                                                int dy = y - getTop();
                                                int pages = dy / getHeight() - 1;
                                                smoothScrollBy(0, pages * h);
                                            } else {
                                                y = r.top + holder.page.getTop();
                                                if (y < getTop()) {
                                                    int dy = y - getTop();
                                                    smoothScrollBy(0, dy);
                                                }
                                            }
                                            searchClose();
                                            updateOverlays();
                                            return;
                                        }
                                    }
                                }
                                return;
                            }
                        }
                    }
                }
                ZLTextPosition pp = new ZLTextFixedPosition(page, 0, 0);
                gotoPluginPosition(pp);
                resetNewPosition();
            }
        }

        public void searchClose() {
            for (ScrollAdapter.PageHolder h : adapter.holders) {
                searchRemove(h.page);
            }
        }

        public void searchRemove(ScrollAdapter.PageView view) {
            if (view.search == null)
                return;
            view.search.close();
            view.search = null;
        }

        public void searchUpdate(ScrollAdapter.PageView view) {
            int pos = view.holder.getAdapterPosition();
            if (pos == -1) {
                searchRemove(view);
            } else {
                ScrollAdapter.PageCursor c = adapter.pages.get(pos);

                final PluginView.Selection.Page page;

                if (c.start == null || c.end == null) {
                    page = null;
                } else {
                    page = pluginview.selectPage(c.start, view.info, view.getWidth(), view.getHeight());
                }

                if (page != null && (!pluginview.reflow || view.info != null) && view.getParent() != null) { // cached views has no parrent
                    if (view.search == null)
                        view.search = new SearchView(search.getBounds(page), view.info);
                    int x = view.getLeft();
                    int y = view.getTop();
                    if (view.info != null)
                        x += view.info.margin.left;
                    view.search.update(x, y);
                } else {
                    searchRemove(view);
                }
            }
        }

        public void selectionClose() {
            for (ScrollAdapter.PageHolder h : adapter.holders) {
                selectionRemove(h.page);
            }
        }

        public void selectionRemove(ScrollAdapter.PageView view) {
            if (view.selection != null) {
                selection.remove(view.selection);
                view.selection = null;
            }
        }

        void selectionUpdate(final ScrollAdapter.PageView view) {
            int pos = view.holder.getAdapterPosition();
            if (pos == -1) {
                selectionRemove(view);
            } else {
                ScrollAdapter.PageCursor c = adapter.pages.get(pos);

                boolean selected = true;
                final PluginView.Selection.Page page;

                if (c.start == null || c.end == null) {
                    selected = false;
                    page = null;
                } else {
                    page = pluginview.selectPage(c.start, view.info, view.getWidth(), view.getHeight());
                }

                if (selected)
                    selected = selection.selection.isSelected(page.page);

                final Rect first;
                final Rect last;

                if (pluginview.reflow && selected && view.info != null) {
                    Rect[] bounds = selection.selection.getBoundsAll(page);
                    ArrayList<Rect> ii = new ArrayList<>();
                    for (Rect b : bounds) {
                        for (Rect s : view.info.src.keySet()) {
                            Rect i = new Rect(b);
                            if (i.intersect(s) && (i.height() * 100 / s.height() > SelectionView.ARTIFACT_PERCENTS || b.height() > 0 && i.height() * 100 / b.height() > SelectionView.ARTIFACT_PERCENTS)) {
                                ii.add(i);
                            }
                        }
                    }
                    Collections.sort(ii, new SelectionView.LinesUL(ii));

                    boolean a = false;
                    Rect f = null;
                    for (int i = 0; !a && i < ii.size(); i++) {
                        f = ii.get(i);
                        do {
                            a = selection.selection.isValid(page, new PluginView.Selection.Point(f.left, f.centerY()));
                        } while (!a && ++f.left < f.right);
                    }
                    first = f;

                    boolean b = false;
                    Rect l = null;
                    for (int i = ii.size() - 1; !b && i >= 0; i--) {
                        l = ii.get(i);
                        do {
                            b = selection.selection.isValid(page, new PluginView.Selection.Point(l.right, l.centerY()));
                        } while (!b && --l.right > l.left);
                    }
                    last = l;

                    Boolean r = selection.selection.inBetween(page, new PluginView.Selection.Point(f.left, f.centerY()), new PluginView.Selection.Point(l.right, l.centerY()));

                    selected = r != null && r;
                } else {
                    if (pluginview.reflow)
                        selected = false;
                    first = null;
                    last = null;
                }

                if (selected) {
                    if (view.selection == null) {
                        PluginView.Selection.Setter setter = new PDFPlugin.Selection.Setter() {
                            @Override
                            public void setStart(int x, int y) {
                                int pos = NO_POSITION;
                                ScrollAdapter.PageView v = findView(x, y);
                                if (v != null) {
                                    pos = v.holder.getAdapterPosition();
                                    if (pos != -1) {
                                        ScrollAdapter.PageCursor c = adapter.pages.get(pos);
                                        x = x - v.getLeft();
                                        y = y - v.getTop();
                                        PluginView.Selection.Page page = pluginview.selectPage(c.start, v.info, v.getWidth(), v.getHeight());
                                        PluginView.Selection.Point point = pluginview.selectPoint(v.info, x, y);
                                        if (point != null)
                                            selection.selection.setStart(page, point);
                                    }
                                }
                                selectionUpdate(view);
                                if (pos != -1 && pos != view.holder.getAdapterPosition())
                                    selectionUpdate(v);
                            }

                            @Override
                            public void setEnd(int x, int y) {
                                int pos = NO_POSITION;
                                ScrollAdapter.PageView v = findView(x, y);
                                if (v != null) {
                                    pos = v.holder.getAdapterPosition();
                                    if (pos != -1) {
                                        ScrollAdapter.PageCursor c = adapter.pages.get(pos);
                                        x = x - v.getLeft();
                                        y = y - v.getTop();
                                        PluginView.Selection.Page page = pluginview.selectPage(c.start, v.info, v.getWidth(), v.getHeight());
                                        PluginView.Selection.Point point = pluginview.selectPoint(v.info, x, y);
                                        if (point != null)
                                            selection.selection.setEnd(page, point);
                                    }
                                }
                                selectionUpdate(view);
                                if (pos != -1 && pos != view.holder.getAdapterPosition())
                                    selectionUpdate(v);
                            }

                            @Override
                            public PluginView.Selection.Bounds getBounds() {
                                PluginView.Selection.Bounds bounds = selection.selection.getBounds(page);
                                if (pluginview.reflow) {
                                    bounds.rr = pluginview.boundsUpdate(bounds.rr, view.info);

                                    Boolean a = selection.selection.isAbove(page, new PluginView.Selection.Point(first.left, first.centerY()));
                                    Boolean b = selection.selection.isBelow(page, new PluginView.Selection.Point(last.right, last.centerY()));

                                    bounds.start = a != null && !a;
                                    bounds.end = b != null && !b;
                                }
                                return bounds;
                            }
                        };
                        view.selection = new SelectionView.PageView(getContext(), (CustomView) app.BookTextView, setter);
                        selection.add(view.selection);
                    }
                    int x = view.getLeft();
                    int y = view.getTop();
                    if (view.info != null)
                        x += view.info.margin.left;
                    selection.update(view.selection, x, y);
                } else {
                    selectionRemove(view);
                }
            }
        }
    }

    public class BrightnessGesture {
        int myStartY;
        boolean myIsBrightnessAdjustmentInProgress;
        int myStartBrightness;
        int areaWidth;
        Integer myColorLevel;

        public BrightnessGesture(Context context) {
            areaWidth = ThemeUtils.dp2px(context, 36); // 24dp - icon; 48dp - button
        }

        public boolean onTouchEvent(MotionEvent e) {
            int x = (int) e.getX();
            int y = (int) e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (app.MiscOptions.AllowScreenBrightnessAdjustment.getValue() && x < areaWidth) {
                        myIsBrightnessAdjustmentInProgress = true;
                        myStartY = y;
                        myStartBrightness = widget.getScreenBrightness();
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (myIsBrightnessAdjustmentInProgress) {
                        if (x >= areaWidth * 2) {
                            myIsBrightnessAdjustmentInProgress = false;
                            return false;
                        } else {
                            final int delta = (myStartBrightness + 30) * (myStartY - y) / getHeight();
                            widget.setScreenBrightness(myStartBrightness + delta);
                            return true;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (myIsBrightnessAdjustmentInProgress) {
                        myIsBrightnessAdjustmentInProgress = false;
                        return true;
                    }
                    break;
            }
            return false;
        }

        public Integer setScreenBrightness(int percent) {
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

            return myColorLevel;
        }

        public int getScreenBrightness() {
            if (myColorLevel != null) {
                return (myColorLevel - 0x60) * 25 / (0xFF - 0x60);
            }

            float level = w.getAttributes().screenBrightness;
            level = level >= 0 ? level : .5f;

            // level = .01f + (percent - 25) * .99f / 75;
            return 25 + (int) ((level - .01f) * 75 / .99f);
        }
    }

    public class PinchGesture extends com.github.axet.androidlibrary.widgets.PinchGesture {
        public PinchGesture(Context context) {
            super(context);
        }

        public boolean isScaleTouch(MotionEvent e) {
            if (pluginview == null || pluginview.reflow)
                return false;
            return super.isScaleTouch(e);
        }

        public void pinchOpen(int page, Rect v) {
            Bitmap bm = pluginview.render(v.width(), v.height(), page);
            pinch = new PinchView(context, v, bm) {
                public int clip;

                {
                    if (widget instanceof ScrollView)
                        clip = ((ScrollView) widget).getMainAreaHeight();
                    else
                        clip = ((ZLAndroidWidget) widget).getMainAreaHeight();
                }

                @Override
                public void pinchClose() {
                    PinchGesture.this.pinchClose();
                }

                @Override
                protected void dispatchDraw(Canvas canvas) {
                    Rect c = canvas.getClipBounds();
                    c.bottom = clip - getTop();
                    canvas.clipRect(c);
                    super.dispatchDraw(canvas);
                }
            };
            FBReaderView.this.addView(pinch, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        public void pinchClose() {
            if (pinch != null)
                FBReaderView.this.removeView(pinch);
            super.pinchClose();
        }
    }

    public class LinksView {
        public ArrayList<View> links = new ArrayList<>();

        public LinksView(PluginView.Link[] ll, Reflow.Info info) {
            if (ll == null)
                return;
            for (int i = 0; i < ll.length; i++) {
                final PluginView.Link l = ll[i];
                Rect[] rr;
                if (pluginview.reflow) {
                    rr = pluginview.boundsUpdate(new Rect[]{l.rect}, info);
                    if (rr.length == 0)
                        continue;
                } else {
                    rr = new Rect[]{l.rect};
                }
                for (Rect r : rr) {
                    MarginLayoutParams lp = new MarginLayoutParams(r.width(), r.height());
                    View v = new View(getContext());
                    v.setLayoutParams(lp);
                    v.setTag(r);
                    v.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (l.index != -1) {
                                app.runAction(ActionCode.PROCESS_HYPERLINK, new BookModel.Label(null, l.index));
                            } else {
                                AboutPreferenceCompat.openUrlDialog(getContext(), l.url);
                            }
                        }
                    });
                    links.add(v);
                    FBReaderView.this.addView(v);
                }
            }
        }

        public void update(int x, int y) {
            for (View v : links) {
                Rect l = (Rect) v.getTag();
                MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
                lp.leftMargin = x + l.left;
                lp.topMargin = y + l.top;
                lp.width = l.width();
                lp.height = l.height();
                v.requestLayout();
            }
        }

        public void hide() {
            for (View v : links)
                v.setVisibility(GONE);
        }

        public void show() {
            for (View v : links)
                v.setVisibility(VISIBLE);
        }

        public void close() {
            final ArrayList<View> old = new ArrayList<>(links); // can be called during RelativeLayout onLayout
            post(new Runnable() {
                @Override
                public void run() {
                    for (View v : old) {
                        FBReaderView.this.removeView(v);
                    }
                }
            });
            links.clear();
        }
    }

    public class BookmarksView {
        public ArrayList<View> bookmarks = new ArrayList<>();
        int clip;

        public class WordView extends View {
            public WordView(Context context) {
                super(context);
            }

            @Override
            public void draw(Canvas canvas) {
                android.graphics.Rect c = canvas.getClipBounds();
                c.bottom = clip - getTop();
                canvas.clipRect(c);
                super.draw(canvas);
            }
        }

        public BookmarksView(PluginView.Selection.Page page, Storage.Bookmarks bms, Reflow.Info info) {
            if (widget instanceof ScrollView)
                clip = ((ScrollView) widget).getMainAreaHeight();
            else
                clip = ((ZLAndroidWidget) widget).getMainAreaHeight();
            if (bms == null)
                return;
            ArrayList<Storage.Bookmark> ll = bms.getBookmarks(page);
            if (ll == null)
                return;
            for (int i = 0; i < ll.size(); i++) {
                final ArrayList<View> bmv = new ArrayList<>();
                final Storage.Bookmark l = ll.get(i);
                PluginView.Selection s = pluginview.select(l.start, l.end);
                PluginView.Selection.Bounds bb = s.getBounds(page);
                s.close();
                Rect union = null;
                Rect[] rr;
                if (pluginview.reflow) {
                    rr = pluginview.boundsUpdate(bb.rr, info);
                } else {
                    rr = bb.rr;
                }
                List<Rect> kk = SelectionView.lines(rr);
                for (Rect r : kk) {
                    if (union == null)
                        union = new Rect(r);
                    else
                        union.union(r);
                    MarginLayoutParams lp = new MarginLayoutParams(r.width(), r.height());
                    WordView v = new WordView(getContext());
                    v.setLayoutParams(lp);
                    v.setTag(r);
                    v.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            BookmarkPopup b = new BookmarkPopup(v, l, bmv) {
                                @Override
                                public void onDelete(Storage.Bookmark l) {
                                    book.info.bookmarks.remove(l);
                                    bookmarksUpdate();
                                    if (pageTurningListener != null)
                                        pageTurningListener.onBookmarksUpdate();
                                }
                            };
                            b.show();
                        }
                    });
                    int color = l.color == 0 ? app.BookTextView.getHighlightingBackgroundColor().intValue() : l.color;
                    v.setBackgroundColor(SelectionView.SELECTION_ALPHA << 24 | (color & 0xffffff));
                    bmv.add(v);
                    bookmarks.add(v);
                    FBReaderView.this.addView(v);
                }
            }
        }

        public void update(int x, int y) {
            for (View v : bookmarks) {
                Rect l = (Rect) v.getTag();
                MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
                lp.leftMargin = x + l.left;
                lp.topMargin = y + l.top;
                lp.width = l.width();
                lp.height = l.height();
                v.requestLayout();
            }
        }

        public void hide() {
            for (View v : bookmarks)
                v.setVisibility(GONE);
        }

        public void show() {
            for (View v : bookmarks)
                v.setVisibility(VISIBLE);
        }

        public void close() {
            final ArrayList<View> old = new ArrayList<>(bookmarks); // can be called during RelativeLayout onLayout
            post(new Runnable() {
                @Override
                public void run() {
                    for (View v : old) {
                        FBReaderView.this.removeView(v);
                    }
                }
            });
            bookmarks.clear();
        }
    }

    public class SearchView {
        public ArrayList<View> words = new ArrayList<>();
        int padding;
        int clip;

        public class WordView extends View {
            public WordView(Context context) {
                super(context);
            }

            @Override
            public void draw(Canvas canvas) {
                android.graphics.Rect c = canvas.getClipBounds();
                c.bottom = clip - getTop();
                canvas.clipRect(c);
                super.draw(canvas);
            }
        }

        @SuppressWarnings("unchecked")
        public SearchView(PluginView.Search.Bounds bb, Reflow.Info info) {
            if (widget instanceof ScrollView)
                clip = ((ScrollView) widget).getMainAreaHeight();
            else
                clip = ((ZLAndroidWidget) widget).getMainAreaHeight();
            padding = ThemeUtils.dp2px(getContext(), SelectionView.SELECTION_PADDING);
            if (bb == null || bb.rr == null)
                return;
            if (pluginview.reflow)
                bb.rr = pluginview.boundsUpdate(bb.rr, info);
            HashSet hh = null;
            if (bb.highlight != null) {
                if (pluginview.reflow)
                    hh = new HashSet(Arrays.asList(pluginview.boundsUpdate(bb.highlight, info)));
                else
                    hh = new HashSet(Arrays.asList(bb.highlight));
            }
            for (int i = 0; i < bb.rr.length; i++) {
                final Rect l = bb.rr[i];
                MarginLayoutParams lp = new MarginLayoutParams(l.width(), l.height());
                WordView v = new WordView(getContext());
                v.setLayoutParams(lp);
                v.setTag(l);
                if (hh != null && hh.contains(l))
                    v.setBackgroundColor(SelectionView.SELECTION_ALPHA << 24 | 0x990000);
                else
                    v.setBackgroundColor(SelectionView.SELECTION_ALPHA << 24 | app.BookTextView.getHighlightingBackgroundColor().intValue());
                words.add(v);
                FBReaderView.this.addView(v);
            }
        }

        public void update(int x, int y) {
            for (View v : words) {
                Rect l = (Rect) v.getTag();
                MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
                lp.leftMargin = x + l.left - padding;
                lp.topMargin = y + l.top - padding;
                lp.width = l.width() + 2 * padding;
                lp.height = l.height() + 2 * padding;
                v.requestLayout();
            }
        }

        public void hide() {
            for (View v : words)
                v.setVisibility(GONE);
        }

        public void show() {
            for (View v : words)
                v.setVisibility(VISIBLE);
        }

        public void close() {
            final ArrayList<View> old = new ArrayList<>(words); // can be called during RelativeLayout onLayout
            post(new Runnable() {
                @Override
                public void run() {
                    for (View v : old) {
                        FBReaderView.this.removeView(v);
                    }
                }
            });
            words.clear();
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
                    PackageManager packageManager = getContext().getPackageManager();
                    List<ResolveInfo> rr = packageManager.queryIntentActivities(translateIntent(null), 0);
                    if (rr.isEmpty())
                        t.setVisibility(View.GONE);
                    else
                        t.setVisibility(View.VISIBLE);
                }
            };
        }

        config();

        app.BookTextView = new CustomView(app);
        app.setView(app.BookTextView);

        setWidget(Widgets.PAGING);
    }

    public void configColorProfile(SharedPreferences shared) {
        if (shared.getString(BookApplication.PREFERENCE_THEME, "").equals(getContext().getString(R.string.Theme_Dark))) {
            config.setValue(app.ViewOptions.ColorProfileName, ColorProfile.NIGHT);
        } else {
            config.setValue(app.ViewOptions.ColorProfileName, ColorProfile.DAY);
            ColorProfile p = ColorProfile.get(ColorProfile.DAY);
            config.setValue(p.BackgroundOption, 0xF5E5CC);
            config.setValue(p.WallpaperOption, "");
        }
    }

    public void configWidget(SharedPreferences shared) {
        String mode = shared.getString(BookApplication.PREFERENCE_VIEW_MODE, "");
        setWidget(mode.equals(FBReaderView.Widgets.CONTINUOUS.toString()) ? FBReaderView.Widgets.CONTINUOUS : FBReaderView.Widgets.PAGING);
    }

    public void config() {
        SharedPreferences shared = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
        configColorProfile(shared);

        int d = shared.getInt(BookApplication.PREFERENCE_FONTSIZE_FBREADER, app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption.getValue());
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption, d);

        String f = shared.getString(BookApplication.PREFERENCE_FONTFAMILY_FBREADER, app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption.getValue());
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
        overlaysClose();
        ZLTextPosition pos = null;
        if (widget != null) {
            pos = getPosition();
            removeView((View) widget);
        }
        widget = v;
        addView((View) v, 0, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (pos != null)
            gotoPosition(pos);
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
                bookmarksUpdate();
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

    public ZLTextPosition getPosition() {
        if (pluginview != null) {
            if (widget instanceof ScrollView) {
                int first = ((ScrollView) widget).findFirstPage();
                if (first != -1) {
                    RecyclerView.ViewHolder h = ((ScrollView) widget).findViewHolderForAdapterPosition(first);
                    ScrollView.ScrollAdapter.PageView p = (ScrollView.ScrollAdapter.PageView) h.itemView;
                    ScrollView.ScrollAdapter.PageCursor c = ((ScrollView) widget).adapter.pages.get(first);
                    PluginPage info = pluginview.getPageInfo(p.getWidth(), p.getHeight(), c);
                    if (p.info != null) { // reflow can be true but reflower == null
                        ArrayList<Rect> rr = new ArrayList<>(p.info.dst.keySet());
                        Collections.sort(rr, new SelectionView.UL());
                        int top = -p.getTop();
                        for (Rect r : rr) {
                            if (r.top > top || (r.top < top && r.bottom > top)) {
                                int screen = r.top - top; // offset from top screen to top element
                                double ratio = p.info.bm.width() / p.getWidth();
                                int offset = (int) (p.info.dst.get(r).top / ratio - screen); // recommended page offset (element - current screen offset)
                                offset *= info.ratio;
                                return new ZLTextFixedPosition(pluginview.current.pageNumber, offset, 0);
                            }
                        }
                    } else {
                        int top = -p.getTop();
                        if (top < 0)
                            top = 0;
                        int offset = (int) (top * info.ratio);
                        return new ZLTextFixedPosition(pluginview.current.pageNumber, offset, 0);
                    }
                }
            }
            return pluginview.getPosition();
        } else {
            if (widget instanceof ScrollView) {
                int first = ((ScrollView) widget).findFirstPage();
                if (first != -1) {
                    ScrollView.ScrollAdapter.PageCursor c = ((ScrollView) widget).adapter.pages.get(first);
                    RecyclerView.ViewHolder h = ((ScrollView) widget).findViewHolderForAdapterPosition(first);
                    ScrollView.ScrollAdapter.PageView p = (ScrollView.ScrollAdapter.PageView) h.itemView;
                    if (p.text != null) { // happens when view invalidate / recycled before calling getPosition()
                        int top = -p.getTop();
                        for (ZLTextElementArea a : p.text.areas()) {
                            if (a.YStart > top || (a.YStart < top && a.YEnd > top)) {
                                ZLTextParagraphCursor paragraphCursor = new ZLTextParagraphCursor(app.Model.getTextModel(), a.getParagraphIndex());
                                ZLTextWordCursor wordCursor = new ZLTextWordCursor(paragraphCursor);
                                wordCursor.moveTo(a);
                                ZLTextFixedPosition last;
                                ZLTextElement e;
                                do {
                                    last = new ZLTextFixedPosition(wordCursor);
                                    wordCursor.previousWord();
                                    e = wordCursor.getElement();
                                }
                                while (e instanceof ZLTextControlElement && wordCursor.compareTo(c.start) >= 0);
                                return last;
                            }
                        }
                    }
                }
            }
            return new ZLTextFixedPosition(app.BookTextView.getStartCursor());
        }
    }

    public void setWindow(Window w) {
        this.w = w;
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
                        if (pluginview != null) {
                            searchClose();
                            search = pluginview.search(pattern);
                            search.setPage(getPosition().getParagraphIndex());
                            a.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (search.getCount() == 0) {
                                        UIMessageUtil.showErrorMessage(a, "textNotFound");
                                        popup.StartPosition = null;
                                    } else {
                                        if (widget instanceof ScrollView) {
                                            ((ScrollView) widget).updateOverlays();
                                        }
                                        if (widget instanceof FBAndroidWidget) {
                                            ((FBAndroidWidget) widget).updateOverlaysReset();
                                        }
                                        app.showPopup(popup.getId());
                                    }
                                }
                            });
                            return;
                        }
                        if (app.getTextView().search(pattern, true, false, false, false) != 0) {
                            a.runOnUiThread(new Runnable() {
                                public void run() {
                                    app.showPopup(popup.getId());
                                    if (widget instanceof ScrollView)
                                        reset();
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
                if (pluginview != null) {
                    BookModel.Label l = (BookModel.Label) params[0];
                    showHyperlink(l);
                    return;
                }
                final ZLTextRegion region = app.BookTextView.getOutlinedRegion();
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
                            final AutoTextSnippet snippet = app.getFootnoteData(hyperlink.Id);
                            if (snippet == null) {
                                break;
                            }

                            app.Collection.markHyperlinkAsVisited(app.getCurrentBook(), hyperlink.Id);
                            final boolean showToast;
                            switch (app.MiscOptions.ShowFootnoteToast.getValue()) {
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
                                            showHyperlink(hyperlink);
                                        }
                                    }));
                                }
                                toast.setText(snippet.getText());
                                toast.setDuration(app.MiscOptions.FootnoteToastDuration.getValue().Value);
                                toast.setOnDismissWrapper(new OnDismissWrapper("ftnt", new SuperToast.OnDismissListener() {
                                    @Override
                                    public void onDismiss(View view) {
                                        app.BookTextView.hideOutline();
                                    }
                                }));
                                app.BookTextView.outlineRegion(region);
                                showToast(toast);
                            } else {
                                book.info.position = getPosition();
                                showHyperlink(hyperlink);
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
                                    String name = image.ImageElement.Id;
                                    Uri uri = Uri.parse(image.ImageElement.URL);
                                    Intent intent = ImagesProvider.getProvider().openIntent(uri, name);
                                    getContext().startActivity(intent);
                                    break;
                                }
                                case R.id.action_share: {
                                    String name = image.ImageElement.Id;
                                    String type = Storage.getTypeByExt(ImagesProvider.EXT);
                                    Uri uri = Uri.parse(image.ImageElement.URL);
                                    Intent intent = ImagesProvider.getProvider().shareIntent(uri, name, type, Storage.getTitle(book.info) + " (" + name + ")");
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
                            app.BookTextView.hideOutline();
                            widget.repaint();
                            FBReaderView.this.removeView(anchor);
                        }
                    });
                    post(new Runnable() { // allow anchor view to be placed
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
                final ZLTextModel textModel = app.BookTextView.getModel();
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
                ((SelectionPopup) app.getPopupById(SelectionPopup.ID)).move(view.getSelectionStartY(), view.getSelectionEndY());
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
                String text;

                if (selection != null) {
                    text = selection.selection.getText();
                } else {
                    TextSnippet snippet = app.BookTextView.getSelectedSnippet();
                    if (snippet == null)
                        return;
                    text = snippet.getText();
                }

                app.BookTextView.clearSelection();
                selectionClose();

                final ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Application.CLIPBOARD_SERVICE);
                clipboard.setText(text);
                UIMessageUtil.showMessageText(a, clipboard.getText().toString());

                if (widget instanceof ScrollView) {
                    ((ScrollView) widget).adapter.processInvalidate();
                    ((ScrollView) widget).adapter.processClear();
                }
            }
        });
        app.addAction(ActionCode.SELECTION_SHARE, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                String text;

                if (selection != null) {
                    text = selection.selection.getText();
                } else {
                    TextSnippet snippet = app.BookTextView.getSelectedSnippet();
                    if (snippet == null)
                        return;
                    text = snippet.getText();
                }

                app.BookTextView.clearSelection();
                selectionClose();

                final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType(HttpClient.CONTENTTYPE_TEXT);
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, Storage.getTitle(book.info));
                intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
                a.startActivity(Intent.createChooser(intent, null));

                if (widget instanceof ScrollView) {
                    getHandler().post(new Runnable() { // do not clear immidiallty. let onPause to be called before p.text = null
                        @Override
                        public void run() {
                            ((ScrollView) widget).adapter.processInvalidate();
                            ((ScrollView) widget).adapter.processClear();
                        }
                    });
                }
            }
        });
        app.addAction(ActionCode.SELECTION_TRANSLATE, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final String text;

                if (selection != null) {
                    text = selection.selection.getText();
                } else {
                    TextSnippet snippet = app.BookTextView.getSelectedSnippet();
                    if (snippet == null)
                        return;
                    text = snippet.getText();
                }

                Intent intent = translateIntent(text);
                getContext().startActivity(intent);

                app.BookTextView.clearSelection();
                selectionClose();
            }
        });
        app.addAction(ActionCode.SELECTION_BOOKMARK, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                if (params.length != 0) {
                    Storage.Bookmark bm = (Storage.Bookmark) params[0];
                    Rect union;
                    final View anchor = new View(getContext());
                    if (widget instanceof ScrollView)
                        union = ((ScrollView) widget).findUnion(bm);
                    else
                        union = findUnion(app.BookTextView.myCurrentPage.TextElementMap.areas(), bm);
                    MarginLayoutParams lp = new MarginLayoutParams(union.width(), union.height());
                    lp.leftMargin = union.left;
                    lp.topMargin = union.top;
                    FBReaderView.this.addView(anchor, lp);
                    final BookmarkPopup b = new BookmarkPopup(anchor, bm, new ArrayList<View>()) {
                        @Override
                        public void onDelete(Storage.Bookmark l) {
                            book.info.bookmarks.remove(l);
                            bookmarksUpdate();
                            if (pageTurningListener != null)
                                pageTurningListener.onBookmarksUpdate();
                        }

                        @Override
                        public void onSelect(int color) {
                            super.onSelect(color);
                            bookmarksUpdate();
                        }

                        @Override
                        public void onDismiss() {
                            super.onDismiss();
                            FBReaderView.this.removeView(anchor);
                            bookmarksUpdate();
                        }
                    };
                    post(new Runnable() {
                        @Override
                        public void run() {
                            b.show();
                        }
                    });
                } else {
                    if (book.info.bookmarks == null)
                        book.info.bookmarks = new Storage.Bookmarks();
                    if (selection != null) {
                        book.info.bookmarks.add(new Storage.Bookmark(selection.selection.getText(), selection.selection.getStart(), selection.selection.getEnd()));
                    } else {
                        TextSnippet snippet = app.BookTextView.getSelectedSnippet();
                        book.info.bookmarks.add(new Storage.Bookmark(snippet.getText(), snippet.getStart(), snippet.getEnd()));
                    }
                    bookmarksUpdate();
                    if (pageTurningListener != null)
                        pageTurningListener.onBookmarksUpdate();
                    app.BookTextView.clearSelection();
                    selectionClose();
                }
            }
        });
        app.addAction(ActionCode.SELECTION_CLEAR, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                app.BookTextView.clearSelection();
                selectionClose();
                if (widget instanceof ScrollView) {
                    ((ScrollView) widget).adapter.processInvalidate();
                    ((ScrollView) widget).adapter.processClear();
                }
            }
        });

        app.addAction(ActionCode.FIND_PREVIOUS, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                if (search != null) {
                    Runnable run = new Runnable() {
                        @Override
                        public void run() {
                            final int page = search.prev();
                            if (page == -1)
                                return;
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    if (widget instanceof ScrollView) {
                                        ((ScrollView) widget).searchPage(page);
                                        return;
                                    }
                                    if (widget instanceof FBAndroidWidget) {
                                        ((FBAndroidWidget) widget).searchPage(page);
                                        return;
                                    }
                                }
                            });
                        }
                    };
                    UIUtil.wait("search", run, getContext());
                    return;
                } else {
                    app.BookTextView.findPrevious();
                }
                resetNewPosition();
            }
        });
        app.addAction(ActionCode.FIND_NEXT, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                if (search != null) {
                    Runnable run = new Runnable() {
                        @Override
                        public void run() {
                            final int page = search.next();
                            if (page == -1)
                                return;
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    if (widget instanceof ScrollView) {
                                        ((ScrollView) widget).searchPage(page);
                                        return;
                                    }
                                    if (widget instanceof FBAndroidWidget) {
                                        ((FBAndroidWidget) widget).searchPage(page);
                                        return;
                                    }
                                }
                            });
                        }
                    };
                    UIUtil.wait("search", run, getContext());
                    return;
                } else {
                    app.BookTextView.findNext();
                }
                resetNewPosition();
            }
        });
        app.addAction(ActionCode.CLEAR_FIND_RESULTS, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                if (pageTurningListener != null)
                    pageTurningListener.onSearchClose();
                searchClose();
                app.BookTextView.clearFindResults();
                resetNewPosition();
            }
        });

        app.addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final PageTurningOptions preferences = app.PageTurningOptions;
                widget.startAnimatedScrolling(
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
                final PageTurningOptions preferences = app.PageTurningOptions;
                widget.startAnimatedScrolling(
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

    void showHyperlink(final ZLTextHyperlink hyperlink) {
        final BookModel.Label label = app.Model.getLabel(hyperlink.Id);
        showHyperlink(label);
    }

    void showHyperlink(final BookModel.Label label) {
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

        r.app.addAction(ActionCode.PROCESS_HYPERLINK, new FBAction(r.app) {
            @Override
            protected void run(Object... params) {
                if (r.pluginview != null) {
                    BookModel.Label l = (BookModel.Label) params[0];
                    r.app.BookTextView.gotoPosition(l.ParagraphIndex, 0, 0);
                    r.resetNewPosition();
                    return;
                }
                final ZLTextRegion region = r.app.BookTextView.getOutlinedRegion();
                if (region == null) {
                    return;
                }
                final ZLTextRegion.Soul soul = region.getSoul();
                if (soul instanceof ZLTextHyperlinkRegionSoul) {
                    r.app.BookTextView.hideOutline();
                    r.widget.repaint();
                    final ZLTextHyperlink hyperlink = ((ZLTextHyperlinkRegionSoul) soul).Hyperlink;
                    switch (hyperlink.Type) {
                        case FBHyperlinkType.EXTERNAL:
                            AboutPreferenceCompat.openUrlDialog(getContext(), hyperlink.Id);
                            break;
                        case FBHyperlinkType.INTERNAL:
                        case FBHyperlinkType.FOOTNOTE: {
                            final BookModel.Label label = r.app.Model.getLabel(hyperlink.Id);
                            r.app.BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
                            r.resetNewPosition();
                        }
                    }
                }
            }
        });

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
        if (label.ModelId == null) {
            builder.setNeutralButton(R.string.keep_reading_position, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    gotoPosition(r.getPosition());
                }
            });
        }
        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                Window w = dialog.getWindow();
                w.setLayout(getWidth(), getHeight()); // fixed size after creation
                r.loadBook(book);
                if (label.ModelId == null) {
                    r.app.BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
                    r.app.setView(r.app.BookTextView);
                } else {
                    final ZLTextModel model = r.app.Model.getFootnoteModel(label.ModelId);
                    r.app.BookTextView.setModel(model);
                    r.app.setView(r.app.BookTextView);
                    r.app.BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
                }
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
            if (p.getElementIndex() != 0) {
                scrollDelayed = p;
                p = new ZLTextFixedPosition(p.getParagraphIndex(), 0, 0);
            }
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
            if (pluginview != null)
                ((ScrollView) widget).updateOverlays();
        } else {
            widget.reset();
            widget.repaint();
        }
    }

    public void updateTheme() {
        if (pluginview != null)
            pluginview.updateTheme();
        if (widget instanceof ScrollView) {
            ((ScrollView) widget).requestLayout(); // repaint views
            ((ScrollView) widget).reset();
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
            pluginview.reflower.index = 0;
    }

    public void selectionOpen(PluginView.Selection s) {
        selectionClose();
        selection = new SelectionView(getContext(), (CustomView) app.BookTextView, s) {
            @Override
            public void onTouchLock() {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                app.runAction(ActionCode.SELECTION_HIDE_PANEL);
            }

            @Override
            public void onTouchUnlock() {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                app.runAction(ActionCode.SELECTION_SHOW_PANEL);
            }
        };
        addView(selection);
        if (widget instanceof ScrollView) {
            ((ScrollView) widget).updateOverlays();
            selection.setClipHeight(((ScrollView) widget).getMainAreaHeight());
        } else {
            selection.setClipHeight(((ZLAndroidWidget) widget).getMainAreaHeight());
        }
        app.runAction(ActionCode.SELECTION_SHOW_PANEL);
    }

    public void selectionClose() {
        if (widget instanceof ScrollView)
            ((ScrollView) widget).selectionClose();
        if (selection != null) {
            selection.close();
            removeView(selection);
            selection = null;
        }
        app.runAction(ActionCode.SELECTION_HIDE_PANEL);
    }

    public void linksClose() {
        if (widget instanceof ScrollView)
            ((ScrollView) widget).linksClose();
        if (widget instanceof FBAndroidWidget)
            ((FBAndroidWidget) widget).linksClose();
    }

    public void bookmarksClose() {
        if (widget instanceof ScrollView)
            ((ScrollView) widget).bookmarksClose();
        if (widget instanceof FBAndroidWidget)
            ((FBAndroidWidget) widget).bookmarksClose();
    }

    public void bookmarksUpdate() {
        if (pluginview == null) {
            app.BookTextView.removeHighlightings(ZLBookmark.class);
            ArrayList<ZLTextHighlighting> hi = new ArrayList<>();
            if (book.info.bookmarks != null) {
                for (int i = 0; i < book.info.bookmarks.size(); i++) {
                    final Storage.Bookmark b = book.info.bookmarks.get(i);
                    ZLBookmark h = new ZLBookmark(app.BookTextView, b);
                    hi.add(h);
                }
            }
            app.BookTextView.addHighlightings(hi);
        }
        if (widget instanceof ScrollView) {
            if (pluginview == null) {
                for (ScrollView.ScrollAdapter.PageHolder h : ((ScrollView) widget).adapter.holders) {
                    h.page.recycle();
                    h.page.invalidate();
                }
            } else {
                ((ScrollView) widget).bookmarksUpdate();
            }
        }
        if (widget instanceof FBAndroidWidget)
            ((FBAndroidWidget) widget).updateOverlaysReset();
    }

    public void searchClose() {
        app.hideActivePopup();
        if (widget instanceof ScrollView)
            ((ScrollView) widget).searchClose();
        if (widget instanceof FBAndroidWidget)
            ((FBAndroidWidget) widget).searchClose();
        if (search != null) {
            search.close();
            search = null;
        }
        searchPagePending = -1;
    }

    public void overlaysClose() {
        pinchClose();
        selectionClose();
        linksClose();
        bookmarksClose();
        searchClose();
    }

    public boolean isPinch() {
        if (widget instanceof ScrollView)
            return ((ScrollView) widget).gesturesListener.pinch.isPinch();
        if (widget instanceof FBAndroidWidget)
            return ((FBAndroidWidget) widget).pinch.isPinch();
        return false;
    }

    public void pinchClose() {
        if (widget instanceof ScrollView)
            ((ScrollView) widget).gesturesListener.pinch.pinchClose();
        if (widget instanceof FBAndroidWidget)
            ((FBAndroidWidget) widget).pinch.pinchClose();
    }

    public void showControls() {
        ActiveAreasView areas = new ActiveAreasView(getContext());
        areas.create(app, getWidth());
        showControls(this, areas);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ZLTextPosition scrollDelayed = getPosition();
        reset();
        gotoPosition(scrollDelayed);
        pinchClose();
    }

    public boolean isReflow() {
        return pluginview.reflow;
    }

    public void setReflow(boolean b) {
        pluginview.reflow = b;
        ZLTextPosition scrollDelayed = getPosition();
        reset();
        gotoPosition(scrollDelayed);
    }

    public int getFontsizeFB() {
        if (book.info.fontsize != null)
            return book.info.fontsize;
        return app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption.getValue();
    }

    public void setFontsizeFB(int p) {
        book.info.fontsize = p;
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption, p);
        resetCaches();
    }

    public void setFontFB(String f) {
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption, f);
        resetCaches();
    }

    public Float getFontsizeReflow() {
        if (book.info.fontsize != null)
            return book.info.fontsize / 100f;
        return null;
    }

    public void setFontsizeReflow(float p) {
        book.info.fontsize = (int) (p * 100f);
        if (pluginview.reflower != null && pluginview.reflower.k2 != null)
            pluginview.reflower.k2.setFontSize(p);
        ZLTextPosition scrollDelayed = getPosition();
        reset();
        gotoPosition(scrollDelayed);
    }
}
