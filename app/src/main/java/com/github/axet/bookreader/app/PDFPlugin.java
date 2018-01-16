package com.github.axet.bookreader.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage;
import com.tom_roush.pdfbox.rendering.PageDrawer;
import com.tom_roush.pdfbox.text.PDFTextStripper;

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
import org.geometerplus.zlibrary.text.view.ZLTextControlElement;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PDFPlugin extends BuiltinFormatPlugin {

    public static String TAG = PDFPlugin.class.getSimpleName();

    public static final String EXT = "pdf";

    public static class PDFTextEntryIterator extends EntryIterator {
        TextParagraph par;
        int index = -1;

        public PDFTextEntryIterator(final TextParagraph p) {
            this.par = p;
        }

        @Override
        public byte getType() {
            Object o = par.entries.get(index);
            if (o instanceof String)
                return ZLTextParagraph.Entry.TEXT;
            if (o instanceof ZLImageEntry)
                return ZLTextParagraph.Entry.IMAGE;
            if (o instanceof ZLTextControlElement)
                return ZLTextParagraph.Entry.CONTROL;
            throw new RuntimeException("unknown");
        }

        @Override
        public char[] getTextData() {
            return ((String) par.entries.get(index)).toCharArray();
        }

        @Override
        public int getTextOffset() {
            return 0;
        }

        @Override
        public boolean getControlIsStart() {
            return ((ZLTextControlElement) par.entries.get(index)).IsStart;
        }

        @Override
        public byte getControlKind() {
            return ((ZLTextControlElement) par.entries.get(index)).Kind;
        }

        @Override
        public int getTextLength() {
            return ((String) par.entries.get(index)).length();
        }

        @Override
        public ZLImageEntry getImageEntry() {
            return (ZLImageEntry) par.entries.get(index);
        }

        @Override
        public boolean next() {
            index++;
            return index < par.entries.size();
        }
    }

    public static class TextParagraph implements ZLTextParagraph {
        PDFTextModel model;
        int index;
        int offset;
        int length;
        ArrayList<Object> entries;

        public TextParagraph(PDFTextModel model, int index) {
            this.model = model;
            this.index = index;
        }

        public void text() {
            entries = new ArrayList<>();
            try {
                PDPage p = model.doc.getPage(index);
                final StringWriter w = new StringWriter();
                PDFTextStripper t = new PDFTextStripper() {
                    {
                        output = w;
                        setStartPage(0);
                    }

                    @Override
                    protected void endArticle() throws IOException {
                        super.endArticle();
                        String s = w.toString();
                        length += s.length();
                        entries.add(s);
                    }
                };
                t.processPage(p);

                PDResources res = p.getResources();
                for (COSName n : res.getXObjectNames()) {
                    PDXObject o = res.getXObject(n);
                    if (o instanceof PDImage) { // PDImageXObject
                        PDImage i = (PDImage) o;
                        String id = "" + entries.size();
                        ZLBitmapImage image = new ZLBitmapImage(i.getImage());
                        Map<String, ZLImage> imageMap = new TreeMap<>();
                        imageMap.put(id, image);
                        ZLImageEntry e = new ZLImageEntry(imageMap, id, (short) 0, false);
                        entries.add(e);
                    }
                }
            } catch (IOException e) {
                Log.d(TAG, "unable to process image", e);
            }
        }

        @Override
        public EntryIterator iterator() {
            if (entries == null)
                text();
            return new PDFTextEntryIterator(this);
        }

        @Override
        public byte getKind() {
            return Kind.TEXT_PARAGRAPH;
        }
    }

    public static class RenderTextParagraph extends TextParagraph {

        public RenderTextParagraph(PDFTextModel model, int index) {
            super(model, index);
        }

        void render() {
            entries = new ArrayList<>();

            String id = "1";
            ZLBitmapImage image = new ZLBitmapImage(null) {
                @Override
                public Bitmap getBitmap() {
                    return createBitmap(model.doc.getPage(index)); // reduce memory impact
                }
            };
            Map<String, ZLImage> imageMap = new TreeMap<>();
            imageMap.put(id, image);
            ZLImageEntry e = new ZLImageEntry(imageMap, id, (short) 0, true);

            entries.add(e);
        }

        public static Bitmap createBitmap(PDPage p) {
            PDRectangle cropBox = p.getCropBox();

            Bitmap bm = Bitmap.createBitmap((int) cropBox.getWidth(), (int) cropBox.getHeight(), Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bm);

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            try {
                renderPage(p, paint, canvas, cropBox);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return bm;
        }

        public static void renderPage(PDPage page, Paint paint, Canvas canvas, PDRectangle cropBox) throws IOException {
            int rotationAngle = page.getRotation();

            if (rotationAngle != 0) {
                float translateX = 0;
                float translateY = 0;
                switch (rotationAngle) {
                    case 90:
                        translateX = cropBox.getHeight();
                        break;
                    case 270:
                        translateY = cropBox.getWidth();
                        break;
                    case 180:
                        translateX = cropBox.getWidth();
                        translateY = cropBox.getHeight();
                        break;
                }
                canvas.translate(translateX, translateY);
                canvas.rotate((float) Math.toRadians(rotationAngle));
            }

            PageDrawer drawer = new PageDrawer(page);
            drawer.drawPage(paint, canvas, cropBox);
        }

        @Override
        public EntryIterator iterator() {
            if (entries == null)
                render();
            return new PDFTextEntryIterator(this);
        }
    }

    public static class EndTextParagraph implements ZLTextParagraph {
        byte kind;

        public EndTextParagraph(byte kind) {
            this.kind = kind;
        }

        @Override
        public EntryIterator iterator() {
            return new PDFPlugin.EntryIterator();
        }

        @Override
        public byte getKind() {
            return kind;
        }
    }

    public static class RenderPage {
        public PDDocument doc;
        public int pageNumber;
        public int pageOffset;
        public PDPage page;
        public PDRectangle pageBox;

        public RenderPage(RenderPage r) {
            doc = r.doc;
            pageNumber = r.pageNumber;
            pageOffset = r.pageOffset;
            page = r.page;
            pageBox = r.pageBox;
        }

        public RenderPage(PDDocument d, int n, int o) {
            doc = d;
            pageNumber = n;
            pageOffset = o;
            load();
        }

        public boolean next(float h) {
            pageOffset += h;
            if (pageOffset >= pageBox.getHeight()) {
                pageOffset = 0;
                pageNumber++;
                if (pageNumber >= doc.getNumberOfPages())
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
                load();
                pageOffset = (int) (pageBox.getHeight() - h);
                return true;
            }
            return true;
        }

        void load() {
            page = doc.getPage(pageNumber);
            pageBox = page.getCropBox();
        }

        public PDRectangle cropBox(float h) {
            return new PDRectangle(0, pageBox.getHeight() - pageOffset - h, pageBox.getWidth(), h);
        }

        public boolean equals(int n, int o) {
            return pageNumber == n && pageOffset == o;
        }
    }

    public static class PDFView {
        public PDDocument doc;
        public int pageNumber;
        public int pageOffset;
        public int pageHeight;
        Paint paint = new Paint();
        Bitmap wallpaper;
        int wallpaperColor;

        public PDFView(Book book) {
            ZLFile f = BookUtil.fileByBook(book);
            try {
                doc = PDDocument.load(new File(f.getPath()));
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
                    if (rr.next(pageHeight))
                        return rr;
                    break;
                case previous:
                    rr = new RenderPage(r);
                    if (rr.prev(pageHeight))
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

            float rr = r.pageBox.getWidth() / w;
            float hh = Math.min(h * rr, r.pageBox.getHeight());

            pageHeight = (int) (hh - hh * 0.05); // -10%
            PDRectangle cropBox = r.cropBox(hh);
            Bitmap bm = Bitmap.createBitmap((int) cropBox.getWidth(), (int) cropBox.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            try {
                RenderTextParagraph.renderPage(r.page, paint, c, cropBox);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Rect src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
            Rect dst = new Rect(0, 0, w, h);
            canvas.drawBitmap(bm, src, dst, paint);
        }

        public ZLTextView.PagePosition pagePosition() {
            return new ZLTextView.PagePosition(pageNumber, doc.getNumberOfPages());
        }
    }

    public static class PDFTextModel extends PDFView implements ZLTextModel {
        public ArrayList<ZLTextParagraph> pars = new ArrayList<>();

        public PDFTextModel(Book book) {
            super(book);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                // pars.add(new RenderTextParagraph(this, i));
                // pars.add(new EndTextParagraph(ZLTextParagraph.Kind.END_OF_SECTION_PARAGRAPH));
                pars.add(new TextParagraph(this, i));
                pars.add(new EndTextParagraph(ZLTextParagraph.Kind.END_OF_TEXT_PARAGRAPH));
            }
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

    public PDFPlugin() {
        super(FBReaderApp.Instance().SystemInfo, EXT);
    }

    @Override
    public void readMetainfo(AbstractBook book) throws BookReadingException {
        ZLFile f = BookUtil.fileByBook(book);
        try {
            PDDocument doc = PDDocument.load(new File(f.getPath()));
            PDDocumentInformation info = doc.getDocumentInformation();
            book.addAuthor(info.getAuthor());
            book.setTitle(info.getTitle());
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
        model.setBookTextModel(new PDFTextModel(model.Book));
    }
}
