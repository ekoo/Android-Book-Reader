package com.github.axet.bookreader.app;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.SparseArray;

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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    public static class UL implements Comparator<Rect> {
        @Override
        public int compare(Rect o1, Rect o2) {
            int r = Integer.valueOf(o2.top).compareTo(Integer.valueOf(o1.top));
            if (r != 0)
                return r;
            return Integer.valueOf(o1.left).compareTo(Integer.valueOf(o2.left));
        }
    }

    public static class SelectionPage {
        int page;
        Pdfium.Page ppage;
        Pdfium.Text text;
        int index; // char index
        int count; // total symbols
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

        public SelectionPage(Pdfium pdfium, int page) {
            this(page, pdfium.openPage(page), 0, 0);
        }

        public SelectionPage(int p, Pdfium.Page page, int w, int h) {
            this.page = p;
            this.ppage = page;
            this.text = page.open();
            this.count = (int) text.getCount();
            this.w = w;
            this.h = h;
            this.index = -1;
        }

        public int findFirstSymbol() {
            Rect[] rr = text.getBounds(0, count);
            Arrays.sort(rr, new UL());
            for (Rect r : rr) {
                int index = text.getIndex(r.left, r.top);
                if (index != -1)
                    return index;
                index = text.getIndex(r.left + 1, r.top + 1);
                if (index != -1)
                    return index;
                index = text.getIndex(r.left, r.centerY());
                if (index != -1)
                    return index;
                index = text.getIndex(r.left + 1, r.centerY());
                if (index != -1)
                    return index;
                index = text.getIndex(r.centerX(), r.centerY());
                if (index != -1)
                    return index;
            }
            return 0;
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
        SparseArray<SelectionPage> map = new SparseArray<>();

        public class SelectionBounds {
            SelectionPage page;
            int ss;
            int ee;
            int cc;
            boolean first;
            boolean last;
            boolean reverse;

            public SelectionBounds(Page p) {
                start.w = p.w;
                start.h = p.h;
                end.w = p.w;
                end.h = p.h;

                SelectionPage s;
                SelectionPage e;

                if (start.page > end.page) {
                    reverse = true;
                    s = end;
                    e = start;
                } else {
                    if (start.page == end.page) {
                        if (start.index > end.index) {
                            reverse = true;
                            s = end;
                            e = start;
                        } else {
                            s = start;
                            e = end;
                        }
                    } else {
                        s = start;
                        e = end;
                    }
                }
                if (s.page == e.page) {
                    ss = s.index;
                    ee = e.index;
                    cc = ee - ss + 1;
                    first = true;
                    last = true;
                } else if (s.page == p.page) {
                    ss = s.index;
                    ee = s.count;
                    cc = ee - ss + 1;
                    first = true;
                    last = false;
                } else if (e.page == p.page) {
                    s = e;
                    ss = s.findFirstSymbol();
                    ee = s.index;
                    cc = ee - ss + 1;
                    first = false;
                    last = true;
                } else {
                    s = open(p);
                    ss = s.findFirstSymbol();
                    ee = s.count;
                    cc = ee - ss + 1;
                    first = false;
                    last = false;
                }
                page = s;
            }
        }


        public Selection(Pdfium pdfium, SelectionPage page, Point point) {
            this.pdfium = pdfium;
            map.put(page.page, page);
            point = new Point(page.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y));
            selectWord(page, point);
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
            s = Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.US); // й composed as two chars sometimes.
            s = Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.US);
            return isWord(s.toCharArray()[0]);
        }

        SelectionPage open(Page page) {
            SelectionPage p = map.get(page.page);
            if (p != null) {
                p.w = page.w;
                p.h = page.h;
            }
            if (p == null) {
                p = new SelectionPage(pdfium, page);
                map.put(p.page, p);
            }
            return new SelectionPage(p);
        }

        SelectionPage open(int page) {
            SelectionPage p = map.get(page);
            if (p == null) {
                p = new SelectionPage(pdfium, page);
                map.put(p.page, p);
            }
            return new SelectionPage(p);
        }

        void selectWord(SelectionPage page, Point point) {
            start = page;
            int index = start.text.getIndex(point.x, point.y);
            if (index < 0 || index >= start.count)
                return;
            int start = index;
            while (start >= 0 && isWord(this.start, start)) {
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
            SelectionPage start = open(page);
            if (start.count > 0) {
                point = new Point(start.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y));
                int index = start.text.getIndex(point.x, point.y);
                if (index == -1) {
                    return;
                }
                start.index = index;
                this.start = start;
                return;
            }
        }

        @Override
        public int getStart() {
            return start.page;
        }

        @Override
        public void setEnd(Page page, Point point) {
            SelectionPage end = open(page);
            if (end.count > 0) {
                point = new Point(end.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y));
                int index = end.text.getIndex(point.x, point.y);
                if (index == -1) {
                    return;
                }
                end.index = index;
                this.end = end;
                return;
            }
        }

        @Override
        public int getEnd() {
            return end.page;
        }

        @Override
        public String getText() {
            int s;
            int e;
            if (start.page > end.page) {
                s = end.page;
                e = start.page;
            } else {
                s = start.page;
                e = end.page;
            }
            String text = "";
            for (int i = s; i <= e; i++) {
                text += getText(i);
            }
            return text;
        }

        String getText(int i) {
            if (start.page == end.page) {
                int s;
                int e;
                if (start.index > end.index) {
                    s = end.index;
                    e = start.index;
                } else {
                    s = start.index;
                    e = end.index;
                }
                int c = e - s + 1;
                return start.text.getText(s, c);
            }
            if (i == start.page) {
                int s;
                int e;
                if (start.page > end.page) {
                    s = 0;
                    e = start.index;
                } else {
                    s = start.index;
                    e = start.count;
                }
                int c = e - s + 1;
                return start.text.getText(s, c);
            }
            if (i == end.page) {
                int s;
                int e;
                if (start.page > end.page) {
                    s = end.index;
                    e = end.count;
                } else {
                    s = 0;
                    e = end.index;
                }
                int c = e - s + 1;
                return end.text.getText(s, c);
            }
            SelectionPage p = open(i);
            return p.text.getText(0, p.count);
        }

        @Override
        public Rect[] getBoundsAll(Page page) {
            SelectionPage p = open(page);
            Rect[] rr = p.text.getBounds(0, p.count);
            for (int i = 0; i < rr.length; i++) {
                Rect r = rr[i];
                r = p.ppage.toDevice(0, 0, p.w, p.h, 0, r);
                rr[i] = r;
            }
            return rr;
        }

        @Override
        public Bounds getBounds(Page p) {
            Bounds bounds = new Bounds();
            SelectionBounds b = new SelectionBounds(p);
            bounds.reverse = b.reverse;
            bounds.start = b.first;
            bounds.end = b.last;
            bounds.rr = b.page.text.getBounds(b.ss, b.cc);
            for (int i = 0; i < bounds.rr.length; i++) {
                Rect r = bounds.rr[i];
                r = b.page.ppage.toDevice(0, 0, b.page.w, b.page.h, 0, r);
                bounds.rr[i] = r;
            }
            return bounds;
        }

        @Override
        public Boolean isAbove(Page page, Point point) {
            if (start.page < page.page)
                return true;
            SelectionBounds b = new SelectionBounds(page);
            if (b.page.count > 0) {
                point = new Point(b.page.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y));
                int index = b.page.text.getIndex(point.x, point.y);
                if (index == -1)
                    return null;
                return b.ss < index || b.ee < index;
            }
            return null;
        }

        @Override
        public Boolean isBelow(Page page, Point point) {
            if (end.page > page.page)
                return true;
            SelectionBounds b = new SelectionBounds(page);
            if (b.page.count > 0) {
                point = new Point(b.page.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y));
                int index = b.page.text.getIndex(point.x, point.y);
                if (index == -1)
                    return null;
                return index < b.ss || index < b.ee;
            }
            return null;
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
            for (int i = 0; i < map.size(); i++) {
                SelectionPage page = map.valueAt(i);
                page.close();
            }
            map.clear();
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
