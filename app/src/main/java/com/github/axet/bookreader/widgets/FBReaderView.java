package com.github.axet.bookreader.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.github.axet.bookreader.activities.MainActivity;
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
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.util.SystemInfo;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

public class FBReaderView extends RelativeLayout {
    public SystemInfo info;
    public FBReaderApp app;
    public FBView view;
    public ZLAndroidWidget w;
    public int battery;

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
//        android:layout_width="fill_parent"
//        android:layout_height="fill_parent"
//        android:fadeScrollbars="false"
//        android:focusable="true"
//        android:scrollbarAlwaysDrawVerticalTrack="true"
//        android:scrollbars="vertical"

        w = new ZLAndroidWidget(getContext()) {
            @Override
            public void setScreenBrightness(int percent) {
                super.setScreenBrightness(percent);
            }

            @Override
            public int getScreenBrightness() {
                return super.getScreenBrightness();
            }
        };
        addView(w, new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        info = new SystemInfo() {
            @Override
            public String tempDirectory() {
                return getContext().getFilesDir().getPath();
            }

            @Override
            public String networkCacheDirectory() {
                return getContext().getFilesDir().getPath();
            }
        };

        app = (FBReaderApp) FBReaderApp.Instance();
        if (app == null) {
            app = new FBReaderApp(info, new BookCollectionShadow());
        }

        app.setWindow(new ZLApplicationWindow() {
            @Override
            public void setWindowTitle(String title) {
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
                return w;
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
            new SelectionPopup(app);
        }

        app.MiscOptions.AllowScreenBrightnessAdjustment.setValue(true);
        app.ViewOptions.ScrollbarType.setValue(FBView.SCROLLBAR_SHOW_AS_FOOTER);
        app.ViewOptions.getFooterOptions().ShowProgress.setValue(FooterOptions.ProgressDisplayType.asPages);

        view = (FBView) ZLApplication.Instance().getCurrentView();
    }

    public void addAction(String action, ZLApplication.ZLAction a) {
        app.addAction(action, a);
    }

    public void load(Storage.StoredBook book) {
        try {
            if (book != null) {
                final PluginCollection pluginCollection = PluginCollection.Instance(info);
                FormatPlugin plugin = BookUtil.getPlugin(pluginCollection, book.book);
                BookModel Model = BookModel.createModel(book.book, plugin);
                ZLTextHyphenator.Instance().load(book.book.getLanguage());
                view.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info != null)
                    view.gotoPosition(book.info.position);
            } else {
                view.setModel(null);
                app.Model = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ZLTextFixedPosition getPosition() {
        return new ZLTextFixedPosition(view.getStartCursor());
    }

}
