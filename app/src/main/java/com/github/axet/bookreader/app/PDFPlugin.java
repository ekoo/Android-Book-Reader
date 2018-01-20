package com.github.axet.bookreader.app;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import com.github.axet.bookreader.widgets.FBReaderView;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.shockwave.pdfium.util.Size;

import org.geometerplus.fbreader.book.AbstractBook;
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
import org.geometerplus.zlibrary.text.model.ZLTextMark;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.model.ZLTextParagraph;
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry;
import org.geometerplus.zlibrary.text.model.ZLVideoEntry;
import org.geometerplus.zlibrary.text.view.ZLTextControlElement;
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class PDFPlugin extends BuiltinFormatPlugin {

    public static String TAG = PDFPlugin.class.getSimpleName();

    public static final String EXT = "pdf";

    public static PdfiumCore core; // context can be avoided

    @TargetApi(21)
    public static class PluginNativePage extends FBReaderView.PluginPage {
        public PdfRenderer doc;

        public PluginNativePage(PluginNativePage r) {
            super(r);
            doc = r.doc;
        }

        public PluginNativePage(PluginNativePage r, ZLViewEnums.PageIndex index) {
            this(r);
            load(index);
        }

        public PluginNativePage(PdfRenderer d) {
            doc = d;
        }

        @Override
        public int getPagesCount() {
            return doc.getPageCount();
        }

        public void load() {
            PdfRenderer.Page page = doc.openPage(pageNumber);
            load(page);
            page.close();
        }

        void load(PdfRenderer.Page page) {
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
        public void drawOnBitmap(Bitmap bitmap, int w, int h, ZLView.PageIndex index) {
            Canvas canvas = new Canvas(bitmap);
            drawWallpaper(canvas);

            PluginNativePage r = new PluginNativePage((PluginNativePage) current, index);
            PdfRenderer.Page page = doc.openPage(r.pageNumber);
            r.load(page);

            r.renderRect(w, h);
            current.updatePage(r);

            r.scale(w, h);
            FBReaderView.RenderRect render = r.renderRect(w, h);

            Bitmap bm = Bitmap.createBitmap(r.pageBox.w, r.pageBox.h, Bitmap.Config.ARGB_8888);
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR);
            page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            canvas.drawBitmap(bm, render.toRect(bm.getWidth(), bm.getHeight()), render.dst, paint);
            bm.recycle();
            page.close();
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

        public PluginPdfiumPage(PluginPdfiumPage r, ZLViewEnums.PageIndex index) {
            this(r);
            load(index);
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
        }
    }

    public static class PDFiumView extends FBReaderView.PluginView {
        ParcelFileDescriptor fd;
        public PdfDocument doc;

        public PDFiumView(ZLFile f) {
            try {
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
        public void drawOnBitmap(Bitmap bitmap, int w, int h, ZLView.PageIndex index) {
            Canvas canvas = new Canvas(bitmap);
            drawWallpaper(canvas);

            PluginPdfiumPage r = new PluginPdfiumPage((PluginPdfiumPage) current, index);
            r.load(r.pageNumber);

            r.renderRect(w, h);
            current.updatePage(r);

            r.scale(w, h);
            FBReaderView.RenderRect render = r.renderRect(w, h);

            core.openPage(doc, r.pageNumber);
            Bitmap bm = Bitmap.createBitmap(r.pageBox.w, r.pageBox.h, Bitmap.Config.ARGB_8888);
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

    public PDFPlugin() {
        super(FBReaderApp.Instance().SystemInfo, EXT);
    }

    @Override
    public void readMetainfo(AbstractBook book) throws BookReadingException {
        ZLFile f = BookUtil.fileByBook(book);
        try {
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
        Bitmap bm = Bitmap.createBitmap(view.current.pageBox.w, view.current.pageBox.h, Bitmap.Config.ARGB_8888);
        view.drawOnBitmap(bm, bm.getWidth(), bm.getHeight(), ZLViewEnums.PageIndex.current);
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
        model.setBookTextModel(new PDFTextModel(BookUtil.fileByBook(model.Book)));
    }
}
