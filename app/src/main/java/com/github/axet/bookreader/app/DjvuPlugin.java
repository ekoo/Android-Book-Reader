package com.github.axet.bookreader.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.github.axet.bookreader.widgets.FBReaderView;
import com.github.axet.djvulibre.DjvuLibre;

import org.geometerplus.fbreader.book.AbstractBook;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.formats.BookReadingException;
import org.geometerplus.fbreader.formats.BuiltinFormatPlugin;
import org.geometerplus.zlibrary.core.encodings.EncodingCollection;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.text.model.ExtensionEntry;
import org.geometerplus.zlibrary.text.model.ZLImageEntry;
import org.geometerplus.zlibrary.text.model.ZLTextMark;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.model.ZLTextParagraph;
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry;
import org.geometerplus.zlibrary.text.model.ZLVideoEntry;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DjvuPlugin extends BuiltinFormatPlugin {

    public static String TAG = DjvuPlugin.class.getSimpleName();

    public static final String EXT = "djvu";
    public static final int PAGE_OVERLAP_PERCENTS = 5; // percents

    public static class RenderPage {
        public DjvuLibre doc;
        public int pageNumber;
        public int pageOffset; // pageBox sizes
        public DjvuLibre.Page pageBox; // pageBox sizes

        public RenderPage(RenderPage r) {
            doc = r.doc;
            pageNumber = r.pageNumber;
            pageOffset = r.pageOffset;
            pageBox = r.pageBox;
        }

        public RenderPage(DjvuLibre d, int n, int o) {
            doc = d;
            pageNumber = n;
            pageOffset = o;
            load();
        }

        public boolean next(float h) {
            pageOffset += h;
            if (pageOffset >= pageBox.height) {
                pageOffset = 0;
                pageNumber++;
                if (pageNumber >= doc.getPagesCount())
                    return false;
                load();
                return true;
            }
            return true;
        }

        public boolean prev(float h) {
            pageOffset -= h;
            if (pageOffset < 0) {
                pageNumber--;
                if (pageNumber < 0)
                    return false;
                pageOffset = (int) (pageBox.height - (pageBox.height % h));
                load();
                return true;
            }
            return true;
        }

        void load() {
            pageBox = doc.getPageInfo(pageNumber);
        }

        public Rect cropBox(float h) {
            int top = (int) (pageBox.height - pageOffset - 1);
            int right = pageBox.width;
            int bottom = (int) (top - h);
            if (bottom <= 0)
                bottom = 0;
            return new Rect(0, top, right, bottom);
        }

        public boolean equals(int n, int o) {
            return pageNumber == n && pageOffset == o;
        }
    }

    public static class DjvuView implements FBReaderView.PluginView {
        public DjvuLibre doc;
        public int pageNumber;
        public int pageOffset; // pageBox sizes
        public int pageStep; // pageBox sizes
        Paint paint = new Paint();
        Bitmap wallpaper;
        int wallpaperColor;

        public DjvuView(Book book) {
            ZLFile f = BookUtil.fileByBook(book);
            try {
                FileInputStream is = new FileInputStream(new File(f.getPath()));
                doc = new DjvuLibre(is.getFD());
                FBReaderApp app = ((FBReaderApp) FBReaderApp.Instance());
                ZLFile wallpaper = app.BookTextView.getWallpaperFile();
                if (wallpaper != null)
                    this.wallpaper = BitmapFactory.decodeStream(wallpaper.getInputStream());
                wallpaperColor = app.BookTextView.getBackgroundColor().intValue();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
        }

        public void gotoPosition(ZLTextPosition p) {
            if (p == null) {
                pageNumber = 0;
                pageOffset = 0;
            } else {
                pageNumber = p.getParagraphIndex();
                pageOffset = p.getElementIndex();
            }
        }

        public RenderPage getPageNumber(ZLViewEnums.PageIndex index) {
            RenderPage r = new RenderPage(doc, pageNumber, pageOffset);
            switch (index) {
                case current:
                    break;
                case next:
                    RenderPage rr = new RenderPage(r);
                    if (rr.next(pageStep))
                        return rr;
                    break;
                case previous:
                    rr = new RenderPage(r);
                    if (rr.prev(pageStep))
                        return rr;
                    break;
            }
            return r;
        }

        public void onScrollingFinished(ZLViewEnums.PageIndex index) {
            RenderPage r = getPageNumber(index);
            pageNumber = r.pageNumber;
            pageOffset = r.pageOffset;
        }

        public ZLTextFixedPosition getPosition() {
            return new ZLTextFixedPosition(pageNumber, pageOffset, 0);
        }

        public boolean canScroll(ZLView.PageIndex index) {
            return !getPageNumber(index).equals(pageNumber, pageOffset);
        }

        public void drawOnBitmap(Bitmap bitmap, int w, int h, ZLView.PageIndex index) {
            Canvas canvas = new Canvas(bitmap);

            if (wallpaper != null) {
                float dx = wallpaper.getWidth();
                float dy = wallpaper.getHeight();
                for (int cw = 0; cw < bitmap.getWidth() + dx; cw += dx) {
                    for (int ch = 0; ch < bitmap.getHeight() + dy; ch += dy) {
                        canvas.drawBitmap(wallpaper, cw - dx, ch - dy, paint);
                    }
                }
            } else {
                canvas.drawColor(wallpaperColor);
            }

            RenderPage r = getPageNumber(index);

            float rr = r.pageBox.width / (float) w;
            float hh = h * rr; // pageBox sizes

            pageStep = (int) (hh - hh * PAGE_OVERLAP_PERCENTS / 100); // -5% or lowest base line

            int width = r.pageBox.width;
            int height = (int) hh;
            int top = r.pageBox.height - height - r.pageOffset - 1;
            if (top <= 0) {
                height += top;
                h += top / rr;
                top = 0;
            }

            Bitmap bm = doc.renderPage(r.pageNumber, 0, 0, r.pageBox.width, r.pageBox.height, 0, top, width, height);
            Rect src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
            Rect dst;
            if (r.pageOffset == 0 && hh > r.pageBox.height) {
                int t = (int) ((hh - r.pageBox.height) / rr / 2);
                dst = new Rect(0, t, w, t + h); // show middle vertically
            } else {
                dst = new Rect(0, 0, w, h);
            }
            canvas.drawBitmap(bm, src, dst, paint);
        }

        public ZLTextView.PagePosition pagePosition() {
            return new ZLTextView.PagePosition(pageNumber, doc.getPagesCount());
        }
    }

    public static class DjvuTextModel extends DjvuView implements ZLTextModel {
        public ArrayList<ZLTextParagraph> pars = new ArrayList<>();

        public DjvuTextModel(Book book) {
            super(book);
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            doc.close();
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
            return pars.size();
        }

        @Override
        public ZLTextParagraph getParagraph(int index) {
            return pars.get(index);
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
            return 0;
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

    public static class EntryIterator implements ZLTextParagraph.EntryIterator {
        @Override
        public byte getType() {
            return 0;
        }

        @Override
        public char[] getTextData() {
            return new char[0];
        }

        @Override
        public int getTextOffset() {
            return 0;
        }

        @Override
        public int getTextLength() {
            return 0;
        }

        @Override
        public byte getControlKind() {
            return 0;
        }

        @Override
        public boolean getControlIsStart() {
            return false;
        }

        @Override
        public byte getHyperlinkType() {
            return 0;
        }

        @Override
        public String getHyperlinkId() {
            return null;
        }

        @Override
        public ZLImageEntry getImageEntry() {
            return null;
        }

        @Override
        public ZLVideoEntry getVideoEntry() {
            return null;
        }

        @Override
        public ExtensionEntry getExtensionEntry() {
            return null;
        }

        @Override
        public ZLTextStyleEntry getStyleEntry() {
            return null;
        }

        @Override
        public short getFixedHSpaceLength() {
            return 0;
        }

        @Override
        public boolean next() {
            return false;
        }
    }

    public DjvuPlugin() {
        super(FBReaderApp.Instance().SystemInfo, EXT);
    }

    @Override
    public void readMetainfo(AbstractBook book) throws BookReadingException {
        ZLFile f = BookUtil.fileByBook(book);
        try {
            FileInputStream is = new FileInputStream(f.getPath());
            DjvuLibre doc = new DjvuLibre(is.getFD());
            book.setTitle(doc.getTitle());
            book.addAuthor(doc.getAuthor());
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void readUids(AbstractBook book) throws BookReadingException {
    }

    @Override
    public void detectLanguageAndEncoding(AbstractBook book) throws BookReadingException {
    }

    @Override
    public ZLImage readCover(ZLFile file) {
        return null;
    }

    @Override
    public String readAnnotation(ZLFile file) {
        return null;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public EncodingCollection supportedEncodings() {
        return null;
    }

    @Override
    public void readModel(BookModel model) throws BookReadingException {
        model.setBookTextModel(new DjvuTextModel(model.Book));
    }
}
