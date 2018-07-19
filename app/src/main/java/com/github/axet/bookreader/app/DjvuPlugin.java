package com.github.axet.bookreader.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.SparseArray;

import com.github.axet.androidlibrary.app.Natives;
import com.github.axet.bookreader.widgets.FBReaderView;
import com.github.axet.bookreader.widgets.PluginRect;
import com.github.axet.bookreader.widgets.PluginView;
import com.github.axet.bookreader.widgets.RenderRect;
import com.github.axet.djvulibre.Config;
import com.github.axet.djvulibre.DjvuLibre;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DjvuPlugin extends BuiltinFormatPlugin {

    public static String TAG = DjvuPlugin.class.getSimpleName();

    public static final String EXT = "djvu";

    public static DjvuPlugin create(Storage.Info info) {
        if (Config.natives) {
            Natives.loadLibraries(info.context, "djvu", "djvulibrejni");
            Config.natives = false;
        }
        Storage.K2PdfOptInit(info.context);
        return new DjvuPlugin(info);
    }

    public static class SelectionPage {
        public int page;
        public int w;
        public int h;
        public DjvuLibre.Bounds index;
        public DjvuLibre.Text text;

        String getText(DjvuLibre.Bounds b) {
            return text.text.substring(b.start, b.end);
        }

        String getText(DjvuLibre.Bounds b1, DjvuLibre.Bounds b2) {
            int s;
            int e;
            if (b1.start > b2.start) {
                s = b2.start;
                e = b1.end;
            } else {
                s = b1.start;
                e = b2.end;
            }
            return text.text.substring(s, e);
        }

        DjvuLibre.Bounds prev(DjvuLibre.Bounds b) {
            List<DjvuLibre.Bounds> bb = Arrays.asList(text.bounds);
            int i = bb.indexOf(b);
            i--;
            if (i < 0)
                return null;
            return bb.get(i);
        }

        DjvuLibre.Bounds next(DjvuLibre.Bounds b) {
            List<DjvuLibre.Bounds> bb = Arrays.asList(text.bounds);
            int i = bb.indexOf(b);
            i++;
            if (i >= bb.size())
                return null;
            return bb.get(i);
        }

        DjvuLibre.Bounds find(PluginView.Selection.Point point) {
            for (DjvuLibre.Bounds b : text.bounds) {
                if (b.rect.contains(point.x, point.y)) {
                    return b;
                }
            }
            return null;
        }

        DjvuLibre.Bounds first() {
            return text.bounds[0];
        }

        DjvuLibre.Bounds last() {
            return text.bounds[text.bounds.length - 1];
        }

        int index(DjvuLibre.Bounds b) {
            List<DjvuLibre.Bounds> bb = Arrays.asList(text.bounds);
            return bb.indexOf(b);
        }

        int index() {
            return index(index);
        }
    }

    public static class DjvuSelection extends PluginView.Selection {
        DjvuLibre doc;

        SelectionPage start;
        SelectionPage end;

        SparseArray<SelectionPage> map = new SparseArray<>();

        public class SelectionBounds {
            SelectionPage page; // current

            SelectionPage s;
            SelectionPage e;

            DjvuLibre.Bounds ss; // start index
            DjvuLibre.Bounds ee; // end index
            DjvuLibre.Bounds ll; // last index

            boolean first;
            boolean last;

            boolean reverse;

            public SelectionBounds(PluginView.Selection.Page p) {
                this(p.page);
                start.w = p.w;
                start.h = p.h;
                end.w = p.w;
                end.h = p.h;
                page.w = p.w; // page can be opened by open
                page.h = p.h;
            }

            public SelectionBounds(int p) {
                this();
                if (s.page == e.page) {
                    page = s;
                    ss = s.index;
                    ee = e.index;
                    first = true;
                    last = true;
                    if (reverse)
                        ss = s.next(ss);
                } else if (s.page == p) {
                    page = s;
                    ss = s.index;
                    ee = s.last();
                    first = true;
                    last = false;
                    if (reverse)
                        ss = s.next(ss);
                } else if (e.page == p) {
                    page = e;
                    ss = e.first();
                    ee = e.index;
                    first = false;
                    last = true;
                } else {
                    page = open(p);
                    ss = page.first();
                    ee = page.last();
                    first = false;
                    last = false;
                }
                ll = ee;
                ee = page.next(ee);
            }

            public SelectionBounds() {
                if (start.page > end.page) {
                    reverse = true;
                    s = end;
                    e = start;
                } else {
                    if (start.page == end.page) {
                        if (start.index() > end.index()) {
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
            }

            String getText() {
                StringBuilder bb = new StringBuilder();
                for (DjvuLibre.Bounds b = ss; b != ee; b = page.next(b)) {
                    bb.append(page.getText(b));
                }
                return bb.toString();
            }
        }

        public DjvuSelection(DjvuLibre doc, Page page, Point point) {
            this.doc = doc;
            point = toPage(page.page, page.w, page.h, point);
            selectWord(open(page), point);
        }

        SelectionPage open(Page page) {
            SelectionPage pp = open(page.page);
            pp.w = page.w;
            pp.h = page.h;
            return pp;
        }

        SelectionPage open(int page) {
            SelectionPage pp = map.get(page);
            if (pp == null) {
                pp = new SelectionPage();
                map.put(page, pp);

                pp.page = page;

                int[] ii = new int[]{DjvuLibre.ZONE_CHARACTER, DjvuLibre.ZONE_WORD, DjvuLibre.ZONE_LINE,
                        DjvuLibre.ZONE_PARAGRAPH, DjvuLibre.ZONE_REGION, DjvuLibre.ZONE_COLUMN,
                        DjvuLibre.ZONE_PAGE};
                for (int i : ii) {
                    pp.text = doc.getText(1, i);
                    if (pp.text != null && pp.text.bounds.length != 0)
                        break;
                }
            }
            return pp;
        }

        public Point toPage(int page, int w, int h, Point point) {
            DjvuLibre.Page p = doc.getPageInfo(page);
            return new Point(point.x * p.width / w, p.height - point.y * p.height / h);
        }

        public Point toDevice(int page, int w, int h, Point point) {
            DjvuLibre.Page p = doc.getPageInfo(page);
            return new Point(point.x * w / p.width, p.height - point.y * h / p.height);
        }

        public Rect toDevice(int page, int w, int h, Rect rect) {
            Point p1 = toDevice(page, w, h, new Point(rect.left, rect.top));
            Point p2 = toDevice(page, w, h, new Point(rect.left, rect.top));
            return new Rect(p1.x, p1.y, p2.x, p2.y);
        }

        public boolean isEmpty() {
            return start == null || end == null;
        }

        boolean isWord(SelectionPage pp, DjvuLibre.Bounds start, DjvuLibre.Bounds b) {
            if (start == null) {
                String s = pp.getText(b);
                s = Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.US); // й composed as two chars sometimes.
                s = Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.US);
                for (char c : s.toCharArray()) {
                    if (isWord(c))
                        return true;
                }
            } else {
                String s = pp.getText(b);
                s = Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.US); // й composed as two chars sometimes.
                s = Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.US);
                for (char c : s.toCharArray()) {
                    if (!isWord(c))
                        return false;
                }
            }
            return false;
        }

        void selectWord(SelectionPage pp, Point point) {
            DjvuLibre.Bounds b = pp.find(point);
            if (b == null)
                return;
            SelectionPage start = pp;
            DjvuLibre.Bounds s = b;
            while (s != null && isWord(start, start.index, s)) {
                start.index = s;
                s = pp.prev(s);
            }
            SelectionPage end = start;
            DjvuLibre.Bounds e = b;
            while (e != null && isWord(end, end.index, e)) {
                end.index = e;
                e = pp.next(e);
            }
            if (start.index == null || end.index == null)
                return;
            this.start = start;
            this.end = end;
        }

        @Override
        public void setStart(Page page, Point point) {
            SelectionPage pp = open(page);
            point = toPage(page.page, page.w, page.h, point);
            DjvuLibre.Bounds b = pp.find(point);
            if (b == null)
                return;
            start.index = b;
        }

        @Override
        public void setEnd(Page page, Point point) {
            SelectionPage pp = open(page);
            point = toPage(page.page, page.w, page.h, point);
            DjvuLibre.Bounds b = pp.find(point);
            if (b == null)
                return;
            end.index = b;
        }

        @Override
        public String getText() {
            SelectionBounds b = new SelectionBounds();
            StringBuilder text = new StringBuilder();
            for (int i = b.s.page; i <= b.e.page; i++) {
                text.append(getText(i));
            }
            return text.toString();
        }

        String getText(int i) {
            SelectionBounds b = new SelectionBounds(i);
            return b.getText();
        }

        @Override
        public Rect[] getBoundsAll(Page page) {
            SelectionPage pp = open(page);
            Rect[] rr = new Rect[pp.text.bounds.length];
            for (int i = 0; i < rr.length; i++) {
                rr[i] = toDevice(page.page, page.w, page.h, pp.text.bounds[i].rect);
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
            ArrayList<Rect> rr = new ArrayList<>();
            for (DjvuLibre.Bounds i = b.ss; i != b.ee; i = b.page.next(i)) {
                rr.add(toDevice(b.page.page, b.page.w, b.page.h, i.rect));
            }
            bounds.rr = rr.toArray(new Rect[0]);
            return bounds;
        }

        @Override
        public Boolean inBetween(Page page, Point start, Point end) {
            SelectionBounds b = new SelectionBounds(page);
            if (b.s.page < page.page && page.page < b.e.page)
                return true;
            Point p1 = toPage(page.page, page.w, page.h, start);
            DjvuLibre.Bounds i1 = b.page.find(p1);
            if (i1 == null)
                return null;
            Point p2 = toPage(page.page, page.w, page.h, end);
            DjvuLibre.Bounds i2 = b.page.find(p2);
            if (i2 == null)
                return null;
            if (b.page.index(i2) < b.page.index(i1))
                return null; // document incorrectly marked (last symbol appears at the end of page)
            return b.page.index(i1) <= b.page.index(b.ss) && b.page.index(b.ss) <= b.page.index(i2) || b.page.index(i1) <= b.page.index(b.ll) && b.page.index(b.ll) <= b.page.index(i2);
        }

        @Override
        public boolean isValid(Page page, Point point) {
            SelectionPage pp = open(page);
            point = toPage(page.page, page.w, page.h, point);
            return pp.find(point) != null;
        }

        @Override
        public boolean isSelected(int page) {
            SelectionBounds b = new SelectionBounds(page);
            return b.s.page <= page && page <= b.e.page;
        }

        @Override
        public Boolean isAbove(Page page, Point point) {
            SelectionBounds b = new SelectionBounds(page);
            if (b.s.page < page.page)
                return true;
            point = toPage(page.page, page.w, page.h, point);
            DjvuLibre.Bounds index = b.page.find(point);
            if (index == null)
                return null;
            return b.page.index(b.ss) < b.page.index(index) || b.page.index(b.ll) < b.page.index(index);
        }

        @Override
        public Boolean isBelow(Page page, Point point) {
            SelectionBounds b = new SelectionBounds(page);
            if (b.e.page > page.page)
                return true;
            point = toPage(page.page, page.w, page.h, point);
            DjvuLibre.Bounds index = b.page.find(point);
            if (index == null)
                return null;
            return b.page.index(index) < b.page.index(b.ss) || b.page.index(index) < b.page.index(b.ll);
        }

        @Override
        public void close() {
        }
    }

    public static class DjvuPage extends com.github.axet.bookreader.widgets.PluginPage {
        public DjvuLibre doc;

        public DjvuPage(DjvuPage r) {
            super(r);
            doc = r.doc;
        }

        public DjvuPage(DjvuPage r, ZLViewEnums.PageIndex index, int w, int h) {
            this(r);
            this.w = w;
            this.h = h;
            load(index);
            if (index == ZLViewEnums.PageIndex.current) {
                load();
                renderPage();
            }
        }

        public DjvuPage(DjvuLibre d, int page, int w, int h) {
            this.doc = d;
            this.w = w;
            this.h = h;
            pageNumber = page;
            pageOffset = 0;
            load();
            renderPage();
        }

        public DjvuPage(DjvuLibre d) {
            doc = d;
            load();
        }

        public void load() {
            DjvuLibre.Page p = doc.getPageInfo(pageNumber);
            pageBox = new PluginRect(0, 0, p.width, p.height);
            dpi = p.dpi;
        }

        @Override
        public int getPagesCount() {
            return doc.getPagesCount();
        }
    }

    public static class DjvuView extends PluginView {
        public DjvuLibre doc;
        Paint paint = new Paint();
        FileInputStream is;

        public DjvuView(ZLFile f) {
            try {
                is = new FileInputStream(new File(f.getPath()));
                doc = new DjvuLibre(is.getFD());
                current = new DjvuPage(doc);
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
            DjvuPage r = new DjvuPage(doc, page, w, 0);
            return r.pageBox.h / r.ratio;
        }

        @Override
        public Bitmap render(int w, int h, int page, Bitmap.Config c) {
            DjvuPage r = new DjvuPage(doc, page, w, h);
            r.scale(w * 2, h * 2);
            Bitmap bm = Bitmap.createBitmap(r.pageBox.w, r.pageBox.h, c);
            doc.renderPage(bm, r.pageNumber, 0, 0, r.pageBox.w, r.pageBox.h, 0, 0, r.pageBox.w, r.pageBox.h);
            bm.setDensity(r.dpi);
            return bm;
        }

        @Override
        public void draw(Canvas canvas, int w, int h, ZLView.PageIndex index, Bitmap.Config c) {
            DjvuPage r = new DjvuPage((DjvuPage) current, index, w, h);
            if (index == ZLViewEnums.PageIndex.current)
                current.updatePage(r);

            r.scale(w, h);
            RenderRect render = r.renderRect();

            Bitmap bm = Bitmap.createBitmap(r.pageBox.w, r.pageBox.h, c);
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR);
            doc.renderPage(bm, r.pageNumber, 0, 0, r.pageBox.w, r.pageBox.h, render.x, render.y, render.w, render.h);
            canvas.drawBitmap(bm, render.src, render.dst, paint);
            bm.recycle();
        }

        @Override
        public Selection select(Selection.Page page, Selection.Point point) {
            DjvuSelection s = new DjvuSelection(doc, page, point);
            if (s.isEmpty())
                return null;
            return s;
        }
    }

    public static class DjvuTextModel extends DjvuView implements ZLTextModel {
        public DjvuTextModel(ZLFile f) {
            super(f);
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            doc.close();
            is.close();
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
            return index;
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

    public DjvuPlugin(Storage.Info info) {
        super(info, EXT);
    }

    @Override
    public void readMetainfo(AbstractBook book) throws BookReadingException {
        ZLFile f = BookUtil.fileByBook(book);
        try {
            FileInputStream is = new FileInputStream(f.getPath());
            DjvuLibre doc = new DjvuLibre(is.getFD());
            book.setTitle(doc.getMeta(DjvuLibre.META_TITLE));
            book.addAuthor(doc.getMeta(DjvuLibre.META_AUTHOR));
            doc.close();
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
        DjvuView view = new DjvuView(file);
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
        DjvuTextModel m = new DjvuTextModel(BookUtil.fileByBook(model.Book));
        model.setBookTextModel(m);
        DjvuLibre.Bookmark[] bookmarks = m.doc.getBookmarks();
        if (bookmarks == null)
            return;
        loadTOC(0, 0, bookmarks, model.TOCTree);
    }

    int loadTOC(int pos, int level, DjvuLibre.Bookmark[] bb, TOCTree tree) {
        int count = 0;
        TOCTree last = null;
        for (int i = pos; i < bb.length; ) {
            DjvuLibre.Bookmark b = bb[i];
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
