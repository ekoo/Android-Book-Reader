package com.github.axet.bookreader.app;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;

import com.github.axet.androidlibrary.app.Natives;
import com.github.axet.bookreader.widgets.FBReaderView;
import com.github.axet.k2pdfopt.K2PdfOpt;
import com.shockwave.pdfium.Config;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.shockwave.pdfium.util.Size;

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

    static {
        if (Config.natives) {
            Natives.loadLibraries(Storage.zlib, "modpdfium", "pdfiumjni");
            Config.natives = false;
        }
        Storage.K2PdfOptInit(Storage.zlib);
    }

    @TargetApi(21)
    public static class PluginNativePage extends FBReaderView.PluginPage {
        public PdfRenderer doc;
        public PdfRenderer.Page page;

        public PluginNativePage(PluginNativePage r) {
            super(r);
            doc = r.doc;
        }

        public PluginNativePage(PluginNativePage r, ZLViewEnums.PageIndex index, int w, int h) {
            this(r);
            this.w = w;
            this.h = h;
            load(index);
            if (index == ZLViewEnums.PageIndex.current) {
                load();
                renderPage();
            }
        }

        public PluginNativePage(PdfRenderer d) {
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
            pageBox = new FBReaderView.PluginRect(0, 0, page.getWidth(), page.getHeight());
        }
    }

    @TargetApi(21)
    public static class PDFNativeView extends FBReaderView.PluginView {
        ParcelFileDescriptor fd;
        public PdfRenderer doc;

        public PDFNativeView(ZLFile f) {
            try {
                fd = ParcelFileDescriptor.open(new File(f.getPath()), ParcelFileDescriptor.MODE_READ_ONLY);
                doc = new PdfRenderer(fd);
                current = new PluginNativePage(doc);
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
        public void draw(Canvas canvas, int w, int h, ZLView.PageIndex index, Bitmap.Config c) {
            PluginNativePage r = new PluginNativePage((PluginNativePage) current, index, w, h);

            current.updatePage(r);

            r.scale(w, h);
            FBReaderView.RenderRect render = r.renderRect();

            Bitmap bm = Bitmap.createBitmap(r.pageBox.w, r.pageBox.h, c);
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR);
            r.page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            canvas.drawBitmap(bm, render.toRect(bm.getWidth(), bm.getHeight()), render.dst, paint);
            bm.recycle();
            r.page.close();
            r.page = null;
        }

    }

    public static class PluginPdfiumPage extends FBReaderView.PluginPage {
        public PdfiumCore core;
        public PdfDocument doc;

        public PluginPdfiumPage(PluginPdfiumPage r) {
            super(r);
            core = r.core;
            doc = r.doc;
        }

        public PluginPdfiumPage(PluginPdfiumPage r, ZLViewEnums.PageIndex index, int w, int h) {
            this(r);
            this.w = w;
            this.h = h;
            load(index);
            if (index == ZLViewEnums.PageIndex.current) {
                load();
                renderPage();
            }
        }

        public PluginPdfiumPage(PluginPdfiumPage r, int page, int w, int h) {
            this(r);
            this.w = w;
            this.h = h;
            pageNumber = page;
            pageOffset = 0;
            load();
            renderPage();
        }

        public PluginPdfiumPage(PdfiumCore c, PdfDocument d) {
            core = c;
            doc = d;
            load();
        }

        @Override
        public int getPagesCount() {
            return core.getPageCount(doc);
        }

        public void load() {
            load(pageNumber);
        }

        void load(int index) {
            Size s = core.getPageSize(doc, index);
            pageBox = new FBReaderView.PluginRect(0, 0, s.getWidth(), s.getHeight());
            dpi = 72; // default Pdifium resolution
        }
    }

    public static class PDFiumView extends FBReaderView.PluginView {
        public PdfiumCore core;
        ParcelFileDescriptor fd;
        public PdfDocument doc;

        public PDFiumView(ZLFile f) {
            try {
                core = new PdfiumCore();
                fd = ParcelFileDescriptor.open(new File(f.getPath()), ParcelFileDescriptor.MODE_READ_ONLY);
                doc = core.newDocument(fd);
                current = new PluginPdfiumPage(core, doc);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
            core.closeDocument(doc);
            try {
                fd.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Bitmap render(int w, int h, int page, Bitmap.Config c) {
            PluginPdfiumPage r = new PluginPdfiumPage((PluginPdfiumPage) current, page, w, h);
            r.scale(w * 2, h * 2);
            Bitmap bm = Bitmap.createBitmap(r.pageBox.w, r.pageBox.h, c);
            core.openPage(doc, r.pageNumber);
            core.renderPageBitmap(doc, bm, r.pageNumber, 0, 0, bm.getWidth(), bm.getHeight());
            bm.setDensity(r.dpi);
            return bm;
        }

        @Override
        public void draw(Canvas canvas, int w, int h, ZLView.PageIndex index, Bitmap.Config c) {
            PluginPdfiumPage r = new PluginPdfiumPage((PluginPdfiumPage) current, index, w, h);

            current.updatePage(r);

            r.scale(w, h);
            FBReaderView.RenderRect render = r.renderRect();

            core.openPage(doc, r.pageNumber);
            Bitmap bm = Bitmap.createBitmap(r.pageBox.w, r.pageBox.h, c);
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR);
            core.renderPageBitmap(doc, bm, r.pageNumber, 0, 0, bm.getWidth(), bm.getHeight());
            canvas.drawBitmap(bm, render.toRect(bm.getWidth(), bm.getHeight()), render.dst, paint);
            bm.recycle();
        }
    }

    public static class PDFTextModel extends PDFiumView implements ZLTextModel {
        public ArrayList<ZLTextParagraph> pars = new ArrayList<>();

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
            return core.getPageCount(doc);
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

    public PDFPlugin() {
        super(Storage.systeminfo, EXT);
    }

    @Override
    public void readMetainfo(AbstractBook book) throws BookReadingException {
        ZLFile f = BookUtil.fileByBook(book);
        try {
            PdfiumCore core = new PdfiumCore();
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(new File(f.getPath()), ParcelFileDescriptor.MODE_READ_ONLY);
            PdfDocument doc = core.newDocument(fd);
            PdfDocument.Meta info = core.getDocumentMeta(doc);
            book.addAuthor(info.getAuthor());
            book.setTitle(info.getTitle());
            core.closeDocument(doc);
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
        PDFiumView view = new PDFiumView(f);
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
        List<PdfDocument.Bookmark> bookmarks = m.core.getTableOfContents(m.doc);
        loadTOC(bookmarks, model.TOCTree);
    }

    void loadTOC(List<PdfDocument.Bookmark> bb, TOCTree tree) {
        for (PdfDocument.Bookmark b : bb) {
            String tt = b.getTitle();
            if (tt.isEmpty() && !b.hasChildren())
                continue;
            TOCTree t = new TOCTree(tree);
            t.setText(tt);
            t.setReference(null, (int) b.getPageIdx());
            if (b.hasChildren())
                loadTOC(b.getChildren(), t);
        }
    }
}
