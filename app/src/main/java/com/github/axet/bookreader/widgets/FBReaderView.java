package com.github.axet.bookreader.widgets;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.axet.bookreader.app.DjvuPlugin;
import com.github.axet.bookreader.app.PDFPlugin;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.djvulibre.DjvuLibre;
import com.github.johnpersano.supertoasts.SuperActivityToast;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.OnClickWrapper;

import org.geometerplus.android.fbreader.NavigationPopup;
import org.geometerplus.android.fbreader.PopupPanel;
import org.geometerplus.android.fbreader.SelectionPopup;
import org.geometerplus.android.fbreader.TextSearchPopup;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.bookmark.EditBookmarkActivity;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.android.util.UIMessageUtil;
import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.DictionaryHighlighting;
import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.fbreader.options.FooterOptions;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.util.TextSnippet;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import java.io.IOException;

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

        public PluginPage(PluginPage r, ZLViewEnums.PageIndex index) {
            this(r);
            load(index);
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
            RenderRect render = new RenderRect(); // render region
            render.ratio = pageBox.w / (float) w;
            float hh = h * render.ratio; // pageBox sizes, visible height

            pageOverlap = (int) (hh * PAGE_OVERLAP_PERCENTS / 100);
            pageStep = (int) (hh - pageOverlap); // -5% or lowest base line

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
                render.dst = new Rect(0, (int) (-pageOffset / render.ratio), w, h);
            } else if (pageOffset == 0 && hh > pageBox.h) {  // show middle vertically
                int t = (int) ((hh - pageBox.h) / render.ratio / 2);
                render.h = pageBox.h;
                render.dst = new Rect(0, t, w, h - t);
            } else {
                render.h = (int) hh;
                render.y = pageBox.h - render.h - pageOffset - 1;
                if (render.y < 0) {
                    render.h += render.y;
                    h += render.y / render.ratio; // convert to display sizes
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
            pageStep = r.pageStep;
            pageOverlap = r.pageOverlap;
        }
    }

    public static class RenderRect extends FBReaderView.PluginRect {
        public float ratio;
        public Rect src;
        public Rect dst;
    }

    public static abstract class PluginView {
        public Bitmap wallpaper;
        public int wallpaperColor;
        public Paint paint = new Paint();
        public PluginPage current;

        public PluginView() {
            try {
                FBReaderApp app = ((FBReaderApp) FBReaderApp.Instance());
                ZLFile wallpaper = app.BookTextView.getWallpaperFile();
                if (wallpaper != null)
                    this.wallpaper = BitmapFactory.decodeStream(wallpaper.getInputStream());
                wallpaperColor = app.BookTextView.getBackgroundColor().intValue();
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
        }

        public void onScrollingFinished(ZLViewEnums.PageIndex index) {
            current.load(index);
        }

        public ZLTextFixedPosition getPosition() {
            return new ZLTextFixedPosition(current.pageNumber, current.pageOffset, 0);
        }

        public boolean canScroll(ZLView.PageIndex index) {
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

        public void drawOnBitmap(Bitmap bitmap, int w, int h, ZLView.PageIndex index) {
        }

        void close() {
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

        @Override
        public synchronized void gotoPage(int page) {
            if (pluginview != null)
                pluginview.gotoPosition(new ZLTextFixedPosition(page, 0, 0));
            else
                super.gotoPage(page);
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

    public void setActivity(final Activity a) {
        PopupPanel.removeAllWindows(app, a);

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
                Toast.makeText(a, toast.getText(), toast.getDuration()).show();
            }
        });

        ((PopupPanel) app.getPopupById(TextSearchPopup.ID)).setPanelInfo(a, this);
        ((NavigationPopup) app.getPopupById(NavigationPopup.ID)).setPanelInfo(a, this);
        ((PopupPanel) app.getPopupById(SelectionPopup.ID)).setPanelInfo(a, this);
    }

}
