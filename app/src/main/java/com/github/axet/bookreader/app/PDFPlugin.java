package com.github.axet.bookreader.app;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import com.github.axet.androidlibrary.app.Natives;
import com.github.axet.bookreader.widgets.FBReaderView;
import com.github.axet.bookreader.widgets.PluginPage;
import com.github.axet.bookreader.widgets.PluginRect;
import com.github.axet.bookreader.widgets.PluginView;
import com.github.axet.bookreader.widgets.RenderRect;
import com.github.axet.pdfium.Config;
import com.github.axet.pdfium.Pdfium;

import org.geometerplus.fbreader.book.AbstractBook;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.formats.BookReadingException;
import org.geometerplus.fbreader.formats.BuiltinFormatPlugin;
import org.geometerplus.zlibrary.core.encodings.EncodingCollection;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.text.model.ZLTextMark;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.model.ZLTextParagraph;
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFPlugin extends BuiltinFormatPlugin {

    public static String TAG = PDFPlugin.class.getSimpleName();

    public static final String EXT = "pdf";

    public static PDFPlugin create(Storage.Info info) {
        if (Config.natives) {
            Natives.loadLibraries(info.context, "modpdfium", "pdfiumjni");
            Config.natives = false;
        }
        Storage.K2PdfOptInit(info.context);
        return new PDFPlugin(info);
    }

    public static class SelectionPage {
        int page;
        Pdfium.Page ppage;
        Pdfium.Text text;
        int index; // char index
        long count; // total symbols
        int w;
        int h;

        public SelectionPage(SelectionPage s) {
            page = s.page;
            ppage = s.ppage;
            text = s.text;
            index = s.index;
            count = s.count;
            w = s.w;
            h = s.h;
        }

        public SelectionPage(Pdfium pdfium, PluginView.Selection.Page page) {
            this(page.page, pdfium.openPage(page.page), page.w, page.h);
        }

        public SelectionPage(int p, Pdfium.Page page, int w, int h) {
            this.page = p;
            this.ppage = page;
            this.text = page.open();
            this.count = text.getCount();
            this.w = w;
            this.h = h;
            this.index = -1;
        }

        public void close() {
            text.close();
            ppage.close();
        }

    }

    public static class Selection extends PluginView.Selection {
        Pdfium pdfium;
        SelectionPage start;
        SelectionPage end;

        public Selection(Pdfium pdfium, SelectionPage sp, Point point) {
            this.pdfium = pdfium;
            point = new Point(sp.ppage.toPage(0, 0, sp.w, sp.h, 0, point.x, point.y));
            selectWord(sp, point);
        }

        public boolean isEmpty() {
            if (start == null || end == null)
                return true;
            return start.index == -1 || end.index == -1;
        }

        boolean isWord(SelectionPage p, int i) {
            String s = p.text.getText(i, 1);
            if (s == null || s.length() != 1)
                return false;
            return isWord(s.toCharArray()[0]);
        }

        void selectWord(SelectionPage page, Point point) {
            start = page;
            int index = start.text.getIndex(point.x, point.y);
            if (index < 0 || index >= start.count)
                return;
            int start = index;
            while (start > 1 && isWord(this.start, start)) {
                this.start.index = start;
                start--;
            }
            end = new SelectionPage(this.start);
            int end = index;
            while (end < this.end.count && isWord(this.end, end)) {
                this.end.index = end;
                end++;
            }
        }

        @Override
        public void setStart(Page page, Point point) {
            SelectionPage start = new SelectionPage(pdfium, page);
            if (start.count > 0) {
                point = new Point(start.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y));
                int index = start.text.getIndex(point.x, point.y);
                if (index == -1) {
                    start.close();
                    return;
                }
                start.index = index;
                this.start = start;
                return;
            }
            start.close();
        }

        @Override
        public int getStart() {
            return start.page;
        }

        @Override
        public void setEnd(Page page, Point point) {
            SelectionPage end = new SelectionPage(pdfium, page);
            if (end.count > 0) {
                point = new Point(end.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y));
                int index = end.text.getIndex(point.x, point.y);
                if (index == -1) {
                    end.close();
                    return;
                }
                end.index = index;
                this.end = end;
                return;
            }
            end.close();
        }

        @Override
        public int getEnd() {
            return end.page;
        }

        @Override
        public String getText() {
            int s = Math.min(start.index, end.index);
            int e = Math.max(start.index, end.index);
            if (start.page == end.page)
                return start.text.getText(s, e - s + 1);
            else
                return null;
        }

        @Override
        public Rect[] getBounds(Page p) {
            int s = Math.min(start.index, end.index);
            int e = Math.max(start.index, end.index);
            if (start.page == end.page) {
                Rect[] rr = start.text.getBounds(s, e - s + 1);
                for (int i = 0; i < rr.length; i++) {
                    Rect r = rr[i];
                    r = start.ppage.toDevice(0, 0, start.w, start.h, 0, r);
                    rr[i] = r;
                }
                return rr;
            } else {
                return null;
            }
        }

        @Override
        public void close() {
            if (start != null) {
                start.close();
                start = null;
            }
            if (end != null) {
                end.close();
                end = null;
            }
        }
    }

    @TargetApi(21)
    public static class NativePage extends PluginPage {
        public PdfRenderer doc;
        public PdfRenderer.Page page;

        public NativePage(NativePage r) {
            super(r);
            doc = r.doc;
        }

        public NativePage(NativePage r, ZLViewEnums.PageIndex index, int w, int h) {
            this(r);
            this.w = w;
            this.h = h;
            load(index);
            if (index == ZLViewEnums.PageIndex.current) {
                load();
                renderPage();
            }
        }

        public NativePage(PdfRenderer d) {
            doc = d;
        }

        @Override
        public int getPagesCount() {
            return doc.getPageCount();
        }

        public void load() {
            if (page != null)
                page.close();
            page = doc.openPage(pageNumber);
            pageBox = new PluginRect(0, 0, page.getWidth(), page.getHeight());
        }
    }

    @TargetApi(21)
    public static class NativeView extends PluginView {
        public PdfRenderer doc;

        public NativeView(ZLFile f) {
            try {
                ParcelFileDescriptor fd = ParcelFileDescriptor.open(new File(f.getPath()), ParcelFileDescriptor.MODE_READ_ONLY);
                doc = new PdfRenderer(fd);
                current = new NativePage(doc);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
            doc.close();
        }

        @Override
        public void draw(Canvas canvas, int w, int h, ZLView.PageIndex index, Bitmap.Config c) {
            NativePage r = new NativePage((NativePage) current, index, w, h);
            if (index == ZLViewEnums.PageIndex.current)
                current.updatePage(r);

            r.scale(w, h);
            RenderRect render = r.renderRect();

            Bitmap bm = Bitmap.createBitmap(r.pageBox.w, r.pageBox.h, c);
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR);
            r.page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            canvas.drawBitmap(bm, render.toRect(bm.getWidth(), bm.getHeight()), render.dst, paint);
            bm.recycle();
            r.page.close();
            r.page = null;
        }

    }

    public static class PdfiumPage extends PluginPage {
        public Pdfium doc;

        public PdfiumPage(PdfiumPage r) {
            super(r);
            doc = r.doc;
        }

        public PdfiumPage(PdfiumPage r, ZLViewEnums.PageIndex index, int w, int h) {
            this(r);
            this.w = w;
            this.h = h;
            load(index);
            if (index == ZLViewEnums.PageIndex.current) {
                load();
                renderPage();
            }
        }

        public PdfiumPage(Pdfium d, int page, int w, int h) {
            this.doc = d;
            this.w = w;
            this.h = h;
            pageNumber = page;
            pageOffset = 0;
            load();
            renderPage();
        }

        public PdfiumPage(Pdfium d) {
            doc = d;
            load();
        }

        @Override
        public int getPagesCount() {
            return doc.getPagesCount();
        }

        public void load() {
            load(pageNumber);
        }

        void load(int index) {
            Pdfium.Size s = doc.getPageSize(index);
            pageBox = new PluginRect(0, 0, s.width, s.height);
            dpi = 72; // default Pdifium resolution
        }
    }

    public static class PdfiumView extends PluginView {
        ParcelFileDescriptor fd;
        public Pdfium doc;

        public PdfiumView(ZLFile f) {
            try {
                doc = new Pdfium();
                fd = ParcelFileDescriptor.open(new File(f.getPath()), ParcelFileDescriptor.MODE_READ_ONLY);
                doc.open(fd.getFileDescriptor());
                current = new PdfiumPage(doc);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
            doc.close();
            try {
                fd.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public double getPageHeight(int w, FBReaderView.ScrollView.ScrollAdapter.PageCursor c) {
            int page;
            if (c.start == null)
                page = c.end.getParagraphIndex() - 1;
            else
                page = c.start.getParagraphIndex();
            PdfiumPage r = new PdfiumPage(doc, page, w, 0);
            return r.pageBox.h / r.ratio;
        }

        @Override
        public Bitmap render(int w, int h, int page, Bitmap.Config c) {
            PdfiumPage r = new PdfiumPage(doc, page, w, h);
            r.scale(w * 2, h * 2);
            Bitmap bm = Bitmap.createBitmap(r.pageBox.w, r.pageBox.h, c);
            Pdfium.Page p = doc.openPage(r.pageNumber);
            p.render(bm, 0, 0, bm.getWidth(), bm.getHeight());
            p.close();
            bm.setDensity(r.dpi);
            return bm;
        }

        @Override
        public void draw(Canvas canvas, int w, int h, ZLView.PageIndex index, Bitmap.Config c) {
            PdfiumPage r = new PdfiumPage((PdfiumPage) current, index, w, h);
            if (index == ZLViewEnums.PageIndex.current)
                current.updatePage(r);

            r.scale(w, h);
            RenderRect render = r.renderRect();

            Pdfium.Page p = doc.openPage(r.pageNumber);
            Bitmap bm = Bitmap.createBitmap(r.pageBox.w, r.pageBox.h, c);
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR);
            p.render(bm, 0, 0, bm.getWidth(), bm.getHeight());
            p.close();
            canvas.drawBitmap(bm, render.toRect(bm.getWidth(), bm.getHeight()), render.dst, paint);
            bm.recycle();
        }

        @Override
        public Selection select(Selection.Page page, Selection.Point point) {
            SelectionPage start = new SelectionPage(doc, page);
            if (start.count > 0) {
                PDFPlugin.Selection s = new PDFPlugin.Selection(doc, start, point);
                if (s.isEmpty()) {
                    s.close();
                    return null;
                }
                return s;
            }
            start.close();
            return null;
        }

    }

    public static class PDFTextModel extends PdfiumView implements ZLTextModel {
        public PDFTextModel(ZLFile f) {
            super(f);
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            close();
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
            return doc.getPagesCount();
        }

        @Override
        public ZLTextParagraph getParagraph(int index) {
            return new ZLTextParagraph() {
                @Override
                public EntryIterator iterator() {
                    return null;
                }

                @Override
                public byte getKind() {
                    return Kind.END_OF_TEXT_PARAGRAPH;
                }
            };
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
            return index; // index - page
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

    public PDFPlugin(Storage.Info info) {
        super(info, EXT);
    }

    @Override
    public void readMetainfo(AbstractBook book) throws BookReadingException {
        ZLFile f = BookUtil.fileByBook(book);
        try {
            Pdfium doc = new Pdfium();
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(new File(f.getPath()), ParcelFileDescriptor.MODE_READ_ONLY);
            doc.open(fd.getFileDescriptor());
            book.addAuthor(doc.getMeta(Pdfium.META_AUTHOR));
            book.setTitle(doc.getMeta(Pdfium.META_TITLE));
            doc.close();
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
    public ZLImage readCover(ZLFile f) {
        PdfiumView view = new PdfiumView(f);
        view.current.scale(Storage.COVER_SIZE, Storage.COVER_SIZE); // reduce render memory footprint
        Bitmap bm = Bitmap.createBitmap(view.current.pageBox.w, view.current.pageBox.h, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bm);
        view.drawWallpaper(canvas);
        view.draw(canvas, bm.getWidth(), bm.getHeight(), ZLViewEnums.PageIndex.current);
        view.close();
        return new ZLBitmapImage(bm);
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
        PDFTextModel m = new PDFTextModel(BookUtil.fileByBook(model.Book));
        model.setBookTextModel(m);
        Pdfium.Bookmark[] bookmarks = m.doc.getTOC();
        loadTOC(0, 0, bookmarks, model.TOCTree);
    }

    int loadTOC(int pos, int level, Pdfium.Bookmark[] bb, TOCTree tree) {
        int count = 0;
        TOCTree last = null;
        for (int i = pos; i < bb.length; ) {
            Pdfium.Bookmark b = bb[i];
            String tt = b.title;
            if (tt == null || tt.isEmpty())
                continue;
            if (b.level > level) {
                int c = loadTOC(i, b.level, bb, last);
                i += c;
                count += c;
            } else if (b.level < level) {
                break;
            } else {
                TOCTree t = new TOCTree(tree);
                t.setText(tt);
                t.setReference(null, b.page);
                last = t;
                i++;
                count++;
            }
        }
        return count;
    }

}
