package com.github.axet.bookreader.widgets;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.github.axet.bookreader.app.PDFPlugin;
import com.github.axet.bookreader.app.Storage;

import org.geometerplus.android.fbreader.NavigationPopup;
import org.geometerplus.android.fbreader.SelectionPopup;
import org.geometerplus.android.fbreader.TextSearchPopup;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.fbreader.options.FooterOptions;
import org.geometerplus.fbreader.formats.BookReadingException;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.util.SystemInfo;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

public class FBReaderView extends RelativeLayout {
    public FBReaderApp app;
    public ZLAndroidWidget widget;
    public int battery;
    public String title;
    public Window w;
    public Storage.Book book;
    public PDFPlugin.PDFView pdfview;

    public class CustomView extends FBView {

        public CustomView(FBReaderApp reader) {
            super(reader);
        }

        @Override
        public boolean canScroll(PageIndex index) {
            if (pdfview != null)
                return pdfview.canScroll(index);
            else
                return super.canScroll(index);
        }

        @Override
        public synchronized void onScrollingFinished(PageIndex pageIndex) {
            if (pdfview != null)
                pdfview.onScrollingFinished(pageIndex);
            else
                super.onScrollingFinished(pageIndex);
        }

        @Override
        public synchronized PagePosition pagePosition() {
            if (pdfview != null)
                return pdfview.pagePosition();
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
                if (pdfview != null)
                    pdfview.drawOnBitmap(bitmap, getWidth(), getMainAreaHeight(), index);
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
                pdfview = new PDFPlugin.PDFView(book.book);
                BookModel Model = BookModel.createModel(book.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info != null)
                    pdfview.gotoPosition(book.info.position);
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
        if (pdfview != null) {
            pdfview.close();
            pdfview = null;
        }
        app.BookTextView.setModel(null);
        app.Model = null;
        book = null;
        widget.setEnabled(false);
        setEnabled(false);
    }

    public ZLTextFixedPosition getPosition() {
        return new ZLTextFixedPosition(app.BookTextView.getStartCursor());
    }

    public void setWindow(Window w) {
        this.w = w;
        app.MiscOptions.AllowScreenBrightnessAdjustment.setValue(true);
    }

}
