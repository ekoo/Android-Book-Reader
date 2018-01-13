package com.github.axet.bookreader.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
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
import org.geometerplus.zlibrary.text.model.ExtensionEntry;
import org.geometerplus.zlibrary.text.model.ZLImageEntry;
import org.geometerplus.zlibrary.text.model.ZLTextMark;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.model.ZLTextParagraph;
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry;
import org.geometerplus.zlibrary.text.model.ZLVideoEntry;
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PDFPlugin extends BuiltinFormatPlugin {

    public static final String EXT = "pdf";

    public static class PDFTextEntryIterator extends EntryIterator {
        StartTextParagraph par;
        int index = -1;

        public PDFTextEntryIterator(final StartTextParagraph p) {
            this.par = p;
            if (p.entries == null)
                p.text();
        }

        @Override
        public byte getType() {
            Object o = par.entries.get(index);
            if (o instanceof String)
                return ZLTextParagraph.Entry.TEXT;
            if (o instanceof ZLImageEntry)
                return ZLTextParagraph.Entry.IMAGE;
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

    public static class StartTextParagraph implements ZLTextParagraph {
        PDFTextModel model;
        int index;
        int offset;
        int length;
        ArrayList<Object> entries;
        StartTextParagraph prev;

        public StartTextParagraph(PDFTextModel model, int index, StartTextParagraph prev) {
            this.model = model;
            this.index = index;
            this.prev = prev;
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

                if (prev != null)
                    offset = prev.offset + prev.length;

                PDResources res = p.getResources();
                for (COSName n : res.getXObjectNames()) {
                    PDXObject o = res.getXObject(n);
                    if (o instanceof PDImage) {
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
                throw new RuntimeException(e);
            }
        }

        void render() {
            entries = new ArrayList<>();

            PDPage p = model.doc.getPage(this.index);

            int height = Resources.getSystem().getDisplayMetrics().heightPixels;
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;

            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
            Bitmap bm = Bitmap.createBitmap(width, height, conf);
            Canvas canvas = new Canvas(bm);

            PDFRenderer r = new PDFRenderer(model.doc);
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            try {
                r.renderPage(p, paint, canvas, width, height, 1f, 1f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String id = "1";
            ZLBitmapImage image = new ZLBitmapImage(bm);
            Map<String, ZLImage> imageMap = new TreeMap<>();
            imageMap.put(id, image);
            ZLImageEntry e = new ZLImageEntry(imageMap, id, (short) 0, true);

            entries.add(e);
        }

        @Override
        public EntryIterator iterator() {
            return new PDFTextEntryIterator(this);
        }

        @Override
        public byte getKind() {
            return Kind.TEXT_PARAGRAPH;
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

    public static class PDFTextModel implements ZLTextModel {
        PDDocument doc;
        ArrayList<ZLTextParagraph> pars = new ArrayList<>();

        public PDFTextModel(Book book) {
            ZLFile f = BookUtil.fileByBook(book);
            try {
                doc = PDDocument.load(new File(f.getPath()));
                StartTextParagraph last = null;
                for (int i = 0; i < doc.getNumberOfPages(); i++) {
                    StartTextParagraph p = new StartTextParagraph(this, i, last);
                    pars.add(p);
                    pars.add(new EndTextParagraph(ZLTextParagraph.Kind.END_OF_TEXT_PARAGRAPH));
                    last = p;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
