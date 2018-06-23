package com.github.axet.bookreader.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.github.axet.bookreader.widgets.FBReaderView;

import net.lingala.zip4j.core.ZipFile;

import org.apache.commons.io.IOUtils;
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
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.NativeFile;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;
import de.innosystec.unrar.rarfile.HostSystem;

public class ComicsPlugin extends BuiltinFormatPlugin {
    public static String TAG = ComicsPlugin.class.getSimpleName();

    public static final String EXTZ = "cbz";
    public static final String EXTR = "cbr";

    public static boolean isImage(ArchiveFile a) {
        File f = new File(a.getPath());
        String e = Storage.getExt(f).toLowerCase();
        switch (e) {
            case "bmp":
            case "png":
            case "jpeg":
            case "gif":
            case "jpg":
            case "webp":
                return true;
        }
        return false;
    }

    public static FBReaderView.PluginRect getImageSize(InputStream is) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Rect outPadding = new Rect();
        BitmapFactory.decodeStream(is, outPadding, options);
        try {
            is.close();
        } catch (IOException e) {
            Log.d(TAG, "unable to close is", e);
        }
        if (options.outWidth == -1 || options.outHeight == -1)
            return null;
        return new FBReaderView.PluginRect(0, 0, options.outWidth, options.outHeight);
    }

    public static String getRarFileName(FileHeader header) {
        String s = header.getFileNameW();
        if (s == null || s.isEmpty())
            s = header.getFileNameString();
        if (header.getHostOS().equals(HostSystem.win32))
            s = s.replaceAll("\\\\", "/");
        return s;
    }

    public static class ZipStore extends net.lingala.zip4j.core.NativeStorage {
        FileChannel fc;

        public ZipStore(FileChannel fc) {
            super((File) null);
            this.fc = fc;
        }

        public net.lingala.zip4j.core.NativeFile read() throws FileNotFoundException {
            return new ZipNativeFile(fc);
        }

        public net.lingala.zip4j.core.NativeFile write() throws FileNotFoundException {
            throw new RuntimeException("not supported");
        }

        public net.lingala.zip4j.core.NativeStorage open(String name) {
            throw new RuntimeException("not supported");
        }

        public boolean exists() {
            return true;
        }

        public boolean canRead() {
            return true;
        }

        public boolean canWrite() {
            return false;
        }

        public boolean isHidden() {
            return false;
        }

        public net.lingala.zip4j.core.NativeStorage getParent() {
            throw new RuntimeException("not supported");
        }

        public String getName() {
            throw new RuntimeException("not supported");
        }

        public boolean isDirectory() {
            return false;
        }

        public long lastModified() {
            throw new RuntimeException("not supported");
        }

        public long length() {
            try {
                return fc.size();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean renameTo(net.lingala.zip4j.core.NativeStorage f) {
            throw new RuntimeException("not supported");
        }

        public void setLastModified(long l) {
            throw new RuntimeException("not supported");
        }

        public void setReadOnly() {
            throw new RuntimeException("not supported");
        }

        public boolean mkdirs() {
            throw new RuntimeException("not supported");
        }

        public boolean delete() {
            throw new RuntimeException("not supported");
        }

        public net.lingala.zip4j.core.NativeStorage[] listFiles() {
            throw new RuntimeException("not supported");
        }

        public String getPath() {
            throw new RuntimeException("not supported");
        }

        public String getRelPath(net.lingala.zip4j.core.NativeStorage child) {
            throw new RuntimeException("not supported");
        }
    }

    public static class RarStore extends de.innosystec.unrar.NativeStorage {
        FileChannel fc;

        public RarStore(FileChannel fc) {
            super((File) null);
            this.fc = fc;
        }

        @Override
        public NativeFile read() throws FileNotFoundException {
            return new RarFile(fc);
        }

        @Override
        public de.innosystec.unrar.NativeStorage open(String name) {
            throw new RuntimeException("not supported");
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public de.innosystec.unrar.NativeStorage getParent() {
            throw new RuntimeException("not supported");
        }

        @Override
        public long length() {
            try {
                return fc.size();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getPath() {
            throw new RuntimeException("not supported");
        }
    }

    public static class ArchiveToc {
        public int level;
        public String name;
        public int page;

        public ArchiveToc(String name, int page, int level) {
            this.name = name;
            this.page = page;
            this.level = level;
        }
    }

    public static class SortByName implements Comparator<ArchiveFile> {
        @Override
        public int compare(ArchiveFile o1, ArchiveFile o2) {
            return o1.getPath().compareTo(o2.getPath());
        }
    }

    public static class Decoder {
        public ArrayList<ArchiveToc> toc;
        public ArrayList<ArchiveFile> pages;
        public Paint paint = new Paint();

        public Decoder() {
        }

        public Bitmap render(int p) {
            ArchiveFile f = pages.get(p);
            InputStream is = f.open();
            Bitmap bm = BitmapFactory.decodeStream(is);
            try {
                is.close();
            } catch (IOException e) {
                Log.d(TAG, "closing stream", e);
            }
            return bm;
        }

        void load(FileDescriptor fd) {
            pages = list(fd);
            Collections.sort(pages, new SortByName());
            loadTOC();
        }

        ArrayList<ArchiveFile> list(FileDescriptor fd) {
            return null;
        }

        void loadTOC() {
            String last = "";
            ArrayList<ArchiveToc> toc = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                ArchiveFile p = pages.get(i);
                File f = new File(p.getPath());
                File n = f.getParentFile();
                if (n != null) {
                    String fn = n.getName();
                    int level = n.getPath().split(Pattern.quote(File.separator)).length - 1;
                    if (!last.equals(fn)) {
                        toc.add(new ArchiveToc(fn, i, level));
                        last = fn;
                    }
                }
            }
            if (toc.size() > 1)
                this.toc = toc;
        }

        void clear() {
        }

        void close() {
        }
    }

    public interface ArchiveFile {
        String getPath();

        InputStream open();

        void copy(OutputStream os);

        long getLength();

        FBReaderView.PluginRect getRect();
    }

    public static class RarFile extends NativeFile {
        FileChannel fc;

        public RarFile(FileChannel fc) {
            this.fc = fc;
        }

        public void setPosition(long s) throws IOException {
            fc.position(s);
        }

        public int read() throws IOException {
            ByteBuffer bb = ByteBuffer.allocate(1);
            fc.read(bb);
            bb.flip();
            return bb.getInt();
        }

        public int readFully(byte[] buf, int len) throws IOException {
            ByteBuffer bb = ByteBuffer.allocate(len);
            fc.read(bb);
            bb.flip();
            ByteBuffer.wrap(buf).put(bb);
            return len;
        }

        public int read(byte[] buf, int off, int len) throws IOException {
            ByteBuffer bb = ByteBuffer.allocate(len);
            fc.read(bb);
            bb.flip();
            ByteBuffer.wrap(buf, off, len).put(bb);
            return len;
        }

        public long getPosition() throws IOException {
            return fc.position();
        }

        public void close() throws IOException {
            fc.close();
        }
    }

    public static class ZipNativeFile extends net.lingala.zip4j.core.NativeFile {
        FileChannel fc;

        public ZipNativeFile(FileChannel fc) {
            this.fc = fc;
        }

        public long length() throws IOException {
            return fc.size();
        }

        public void seek(long s) throws IOException {
            fc.position(s);
        }

        public void readFully(byte[] buf, int off, int len) throws IOException {
            read(buf, off, len);
        }

        public int read(byte[] buf) throws IOException {
            ByteBuffer bb = ByteBuffer.wrap(buf);
            int l = fc.read(bb);
            bb.flip();
            return l;
        }

        public int read(byte[] buf, int off, int len) throws IOException {
            ByteBuffer bb = ByteBuffer.wrap(buf, off, len);
            fc.read(bb);
            bb.flip();
            return len;
        }

        public long getFilePointer() throws IOException {
            return fc.position();
        }

        public void close() throws IOException {
        }

        public void write(byte[] buf) throws IOException {
            throw new RuntimeException("not supported");
        }

        public void write(byte[] b, int off, int len) throws IOException {
            throw new RuntimeException("not supported");
        }
    }

    public static class RarDecoder extends Decoder {
        ArrayList<Archive> aa = new ArrayList<>();

        public RarDecoder(FileDescriptor fd) {
            load(fd);
        }

        @Override
        public ArrayList<ArchiveFile> list(FileDescriptor fd) {
            try {
                ArrayList<ArchiveFile> ff = new ArrayList<>();
                final FileInputStream fis = new FileInputStream(fd);
                final FileChannel fc = fis.getChannel();
                final Archive archive = new Archive(new RarStore(fc));
                List<FileHeader> list = archive.getFileHeaders();
                for (FileHeader h : list) {
                    if (h.isDirectory())
                        continue;
                    final FileHeader header = h;
                    ArchiveFile a = new ArchiveFile() {
                        FBReaderView.PluginRect r = null;

                        @Override
                        public FBReaderView.PluginRect getRect() {
                            if (r == null)
                                r = getImageSize(open());
                            return r;
                        }

                        @Override
                        public String getPath() {
                            return getRarFileName(header);
                        }

                        @Override
                        public InputStream open() {
                            try {
                                final PipedInputStream is = new PipedInputStream();
                                final PipedOutputStream os = new PipedOutputStream(is);
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            archive.extractFile(header, os);
                                            os.flush();
                                            os.close();
                                        } catch (Throwable e) {
                                            Log.d(TAG, "extract file broken", e);
                                        }
                                    }
                                }, "Write Archive File");
                                thread.start();
                                return is;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void copy(OutputStream os) {
                            try {
                                archive.extractFile(header, os);
                            } catch (RarException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public long getLength() {
                            return header.getFullUnpackSize();
                        }
                    };
                    if (isImage(a))
                        ff.add(a);
                }
                return ff;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void clear() {
            try {
                for (Archive a : aa) {
                    a.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            aa.clear();
        }
    }

    public static class ZipDecoder extends Decoder {
        ArrayList<ZipFile> aa = new ArrayList<>();

        public ZipDecoder(FileDescriptor fd) {
            load(fd);
        }

        @Override
        public ArrayList<ArchiveFile> list(FileDescriptor fd) {
            try {
                final FileInputStream fis = new FileInputStream(fd);
                final FileChannel fc = fis.getChannel();
                ArrayList<ArchiveFile> ff = new ArrayList<>();
                final ZipFile zip = new ZipFile(new ZipStore(fc));
                aa.add(zip);
                List list = zip.getFileHeaders();
                for (Object o : list) {
                    final net.lingala.zip4j.model.FileHeader zipEntry = (net.lingala.zip4j.model.FileHeader) o;
                    if (zipEntry.isDirectory())
                        continue;
                    ArchiveFile a = new ArchiveFile() {
                        FBReaderView.PluginRect r = null;

                        @Override
                        public FBReaderView.PluginRect getRect() {
                            if (r == null)
                                r = getImageSize(open());
                            return r;
                        }

                        @Override
                        public String getPath() {
                            return zipEntry.getFileName();
                        }

                        @Override
                        public InputStream open() {
                            try {
                                return zip.getInputStream(zipEntry);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        public void copy(OutputStream os) {
                            try {
                                InputStream is = zip.getInputStream(zipEntry);
                                IOUtils.copy(is, os);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public long getLength() {
                            return zipEntry.getUncompressedSize();
                        }
                    };
                    if (isImage(a))
                        ff.add(a);
                }
                return ff;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void clear() {
            aa.clear();
        }
    }

    public static class PluginPage extends FBReaderView.PluginPage {
        public Decoder doc;

        public PluginPage(PluginPage r) {
            super(r);
            doc = r.doc;
        }

        public PluginPage(PluginPage r, ZLViewEnums.PageIndex index, int w, int h) {
            this(r);
            this.w = w;
            this.h = h;
            load(index);
            if (index == ZLViewEnums.PageIndex.current) {
                load();
                renderPage();
            }
        }

        public PluginPage(PluginPage r, int page, int w, int h) {
            this(r);
            this.w = w;
            this.h = h;
            pageNumber = page;
            pageOffset = 0;
            load();
            renderPage();
        }

        public PluginPage(Decoder d) {
            doc = d;
            load();
        }

        public void load() {
            ArchiveFile f = doc.pages.get(pageNumber);
            pageBox = f.getRect();
            if (pageBox == null)
                pageBox = new FBReaderView.PluginRect(0, 0, 100, 100);
            dpi = 72;
        }

        @Override
        public int getPagesCount() {
            return doc.pages.size();
        }
    }

    public static class ComicsView extends FBReaderView.PluginView {
        public Decoder doc;
        Paint paint = new Paint();
        FileInputStream is;

        public ComicsView(ZLFile f) {
            try {
                File ff = new File(f.getPath());
                is = new FileInputStream(ff);
                if (ff.toString().toLowerCase().endsWith(EXTZ))
                    doc = new ZipDecoder(is.getFD());
                if (ff.toString().toLowerCase().endsWith(EXTR))
                    doc = new RarDecoder(is.getFD());
                current = new PluginPage(doc);
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
            PluginPage r = new PluginPage((PluginPage) current, page, w, 0);
            return r.pageBox.h / r.ratio;
        }

        @Override
        public Bitmap render(int w, int h, int page, Bitmap.Config c) {
            PluginPage r = new PluginPage((PluginPage) current, page, w, h);
            Bitmap bm = doc.render(r.pageNumber);
            bm.setDensity(r.dpi);
            return bm;
        }

        @Override
        public void draw(Canvas canvas, int w, int h, ZLView.PageIndex index, Bitmap.Config c) {
            PluginPage r = new PluginPage((PluginPage) current, index, w, h);
            if (index == ZLViewEnums.PageIndex.current)
                current.updatePage(r);

            FBReaderView.RenderRect render = r.renderRect();

            Bitmap bm = doc.render(r.pageNumber);
            if (bm != null) {
                canvas.drawBitmap(bm, render.toRect(r.pageBox.w, r.pageBox.h), render.dst, paint);
                bm.recycle();
            }
        }
    }

    public static class ComicsTextModel extends ComicsView implements ZLTextModel {
        public ComicsTextModel(ZLFile f) {
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
            return doc.pages.size();
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

    public ComicsPlugin() {
        super(Storage.systeminfo, EXTZ);
    }

    @Override
    public void readMetainfo(AbstractBook book) throws BookReadingException {
    }

    @Override
    public void readUids(AbstractBook book) throws BookReadingException {
    }

    @Override
    public void detectLanguageAndEncoding(AbstractBook book) throws BookReadingException {
    }

    @Override
    public ZLImage readCover(ZLFile file) {
        ComicsView view = new ComicsView(file);
        int m = Math.max(view.current.pageBox.w, view.current.pageBox.h);
        double ratio = Storage.COVER_SIZE / (double) m;
        int w = (int) (view.current.pageBox.w * ratio);
        int h = (int) (view.current.pageBox.h * ratio);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
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
        ComicsTextModel m = new ComicsTextModel(BookUtil.fileByBook(model.Book));
        model.setBookTextModel(m);
        if (m.doc.toc == null)
            return;
        loadTOC(0, 0, m.doc.toc, model.TOCTree);
    }

    int loadTOC(int pos, int level, ArrayList<ArchiveToc> bb, TOCTree tree) {
        int count = 0;
        TOCTree last = null;
        for (int i = pos; i < bb.size(); ) {
            ArchiveToc b = bb.get(i);
            String tt = b.name;
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
