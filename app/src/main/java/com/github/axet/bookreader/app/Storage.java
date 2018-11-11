package com.github.axet.bookreader.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.axet.androidlibrary.app.AlarmManager;
import com.github.axet.androidlibrary.net.HttpClient;
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.androidlibrary.widgets.WebViewCustom;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.widgets.FBReaderView;
import com.github.axet.bookreader.widgets.PluginView;
import com.github.axet.wget.SpeedInfo;

import org.apache.commons.io.IOUtils;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.formats.BookReadingException;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLFileImageProxy;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.image.ZLStreamImage;
import org.geometerplus.zlibrary.core.util.SystemInfo;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.rarfile.FileHeader;

public class Storage extends com.github.axet.androidlibrary.app.Storage {
    public static String TAG = Storage.class.getCanonicalName();

    public static final int MD5_SIZE = 32;
    public static final int COVER_SIZE = 128;
    public static final int BUF_SIZE = 1024;
    public static final String JSON_EXT = "json";
    public static final String ZIP_EXT = "zip";

    public static Detector[] supported() {
        return new Detector[]{new FileFB2(), new FileFB2Zip(), new FileEPUB(), new FileHTML(), new FileHTMLZip(),
                new FilePDF(), new FileDjvu(), new FileRTF(), new FileRTFZip(), new FileDoc(),
                new FileMobi(), new FileTxt(), new FileTxtZip(), new FileCbz(), new FileCbr()};
    }

    public static String detecting(Storage storage, Detector[] dd, InputStream is, OutputStream os, Uri u) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        FileTypeDetectorXml xml = new FileTypeDetectorXml(dd);
        FileTypeDetectorZip zip = new FileTypeDetectorZip(dd);
        FileTypeDetector bin = new FileTypeDetector(dd);
        ExtDetector ext = new ExtDetector(dd);

        byte[] buf = new byte[BUF_SIZE];
        int len;
        while ((len = is.read(buf)) > 0) {
            if (Thread.currentThread().isInterrupted())
                throw new DownloadInterrupted();
            digest.update(buf, 0, len);
            if (os != null)
                os.write(buf, 0, len);
            xml.write(buf, 0, len);
            zip.write(buf, 0, len);
            bin.write(buf, 0, len);
        }

        if (os != null)
            os.close();
        bin.close();
        zip.close();
        xml.close();

        String s = u.getScheme();
        if (s.equals(ContentResolver.SCHEME_CONTENT) || s.equals(ContentResolver.SCHEME_FILE)) // ext detection works for local files only
            ext.detect(storage, u);

        return toHex(digest.digest());
    }

    public static FormatPlugin getPlugin(Storage.Info info, Storage.FBook b) {
        PluginCollection c = PluginCollection.Instance(info);
        ZLFile f = BookUtil.fileByBook(b.book);
        switch (f.getExtension()) {
            case PDFPlugin.EXT:
                return PDFPlugin.create(info);
            case DjvuPlugin.EXT:
                return DjvuPlugin.create(info);
            case ComicsPlugin.EXTZ:
            case ComicsPlugin.EXTR:
                return new ComicsPlugin(info);
        }
        try {
            return BookUtil.getPlugin(c, b.book);
        } catch (BookReadingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class DownloadInterrupted extends RuntimeException {
    }

    public static class Info implements SystemInfo {
        public Context context;

        public Info(Context context) {
            this.context = context;
        }

        @Override
        public String tempDirectory() {
            return context.getCacheDir().getPath();
        }

        @Override
        public String networkCacheDirectory() {
            return context.getCacheDir().getPath();
        }
    }

    public static class Progress {
        public SpeedInfo info = new SpeedInfo();
        public long last;

        public Progress() {
            info.start(0);
        }

        public void update(long read, long total) {
            long time = System.currentTimeMillis();
            if (last + AlarmManager.SEC1 < time) {
                info.step(read);
                last = time;
                progress(read, total);
            }
        }

        public void progress(long read, long total) {
        }
    }

    public static class ProgresInputstream extends InputStream {
        long read;
        long total;
        InputStream is;
        Progress progress;

        public ProgresInputstream(InputStream is, long total, Progress progress) {
            this.is = is;
            this.total = total;
            this.progress = progress;
            this.progress.update(0, total);
        }

        @Override
        public int read() throws IOException {
            read++;
            progress.update(read, total);
            return is.read();
        }
    }

    public static String getTitle(Book book, FBook fbook) {
        String t = fbook.book.getTitle();
        if (t.equals(book.md5))
            t = null;
        return t;
    }

    public static String getTitle(RecentInfo info) {
        String s = "";
        if (info.authors != null && !info.authors.isEmpty())
            s += info.authors;
        if (info.title != null && !info.title.isEmpty()) {
            if (!s.isEmpty())
                s += " - ";
            s += info.title;
        }
        return s;
    }

    public static File coverFile(Context context, Book book) {
        return CacheImagesAdapter.cacheUri(context, book.url);
    }

    public static File recentFile(Book book) {
        File f = getFile(book.url);
        File p = f.getParentFile();
        return new File(p, book.md5 + "." + JSON_EXT);
    }

    public Uri recentUri(Book book) {
        String s = book.url.getScheme();
        if (Build.VERSION.SDK_INT >= 21 && s.equals(ContentResolver.SCHEME_CONTENT)) {
            String id = book.md5 + "." + JSON_EXT;
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(Storage.getDocumentTreeUri(book.url), DocumentsContract.getTreeDocumentId(book.url));
            return child(doc, id);
        } else if (s.equals(ContentResolver.SCHEME_FILE)) {
            return Uri.fromFile(recentFile(book));
        } else {
            throw new UnknownUri();
        }
    }

    public List<Uri> recentUris(final Book book) {
        List<Uri> list = new ArrayList<>();
        Uri storage = getStoragePath();
        String s = storage.getScheme();
        if (Build.VERSION.SDK_INT >= 21 && s.equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver contentResolver = context.getContentResolver();
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(storage, DocumentsContract.getTreeDocumentId(storage));
            Cursor childCursor = contentResolver.query(childrenUri, null, null, null, null);
            if (childCursor != null) {
                try {
                    while (childCursor.moveToNext()) {
                        String id = childCursor.getString(childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                        String t = childCursor.getString(childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                        String e = getExt(t).toLowerCase();
                        if (t.startsWith(book.md5) && e.equals(JSON_EXT)) { // delete all but json
                            Uri k = DocumentsContract.buildDocumentUriUsingTree(storage, id);
                            list.add(k);
                        }
                    }
                } finally {
                    childCursor.close();
                }
            }
        } else if (s.equals(ContentResolver.SCHEME_FILE)) {
            File dir = getFile(storage);
            File[] ff = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    String e = getExt(name).toLowerCase();
                    return name.startsWith(book.md5) && e.equals(JSON_EXT);
                }
            });
            if (ff != null) {
                for (File f : ff) {
                    list.add(Uri.fromFile(f));
                }
            }
        } else {
            throw new UnknownUri();
        }
        return list;
    }

    public static ZLTextPosition loadPosition(String s) {
        if (s == null || s.isEmpty())
            return null;
        try {
            JSONArray o = new JSONArray(s);
            return loadPosition(o);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static ZLTextPosition loadPosition(JSONArray a) throws JSONException {
        if (a == null || a.length() == 0)
            return null;
        return new ZLTextFixedPosition(a.getInt(0), a.getInt(1), a.getInt(2));
    }

    public static JSONArray savePosition(ZLTextPosition position) {
        if (position == null)
            return null;
        JSONArray a = new JSONArray();
        a.put(position.getParagraphIndex());
        a.put(position.getElementIndex());
        a.put(position.getCharIndex());
        return a;
    }

    public static class Detector {
        public boolean done;
        public boolean detected;
        public String ext;

        public Detector(String ext) {
            this.ext = ext;
        }

        public void clear() {
            done = false;
            detected = false;
        }
    }

    public static class FileTypeDetector {
        ArrayList<Handler> list = new ArrayList<>();

        public static class Handler extends Detector {
            byte[] first;
            ByteArrayOutputStream os; // no need to close

            public Handler(String ext) {
                super(ext);
                clear();
            }

            public Handler(String ext, String str) {
                super(ext);
                first = str.getBytes(Charset.defaultCharset());
                clear();
            }

            public Handler(String ext, int[] b) {
                super(ext);
                first = new byte[b.length];
                for (int i = 0; i < b.length; i++)
                    first[i] = (byte) b[i];
                clear();
            }

            public void write(byte[] buf, int off, int len) {
                if (first != null) {
                    int left = first.length - os.size();
                    if (len > left)
                        len = left;
                    os.write(buf, off, len);
                    left = first.length - os.size();
                    if (left == 0) {
                        done = true;
                        detected = equals(os.toByteArray(), first);
                    }
                } else {
                    os.write(buf, off, len);
                }
            }

            public boolean equals(byte[] buf1, byte[] buf2) {
                int len = buf1.length;
                if (len != buf2.length)
                    return false;
                for (int i = 0; i < len; i++) {
                    if (buf1[i] != buf2[i])
                        return false;
                }
                return true;
            }

            public byte[] head(byte[] buf, int head) {
                byte[] b = new byte[head];
                System.arraycopy(buf, 0, b, 0, head);
                return b;
            }

            public byte[] tail(byte[] buf, int tail) {
                byte[] b = new byte[tail];
                System.arraycopy(buf, buf.length - tail, b, 0, tail);
                return b;
            }

            public void clear() {
                super.clear();
                os = new ByteArrayOutputStream();
            }
        }

        public FileTypeDetector(Detector[] dd) {
            for (Detector d : dd) {
                if (d instanceof Handler) {
                    Handler h = (Handler) d;
                    h.clear();
                    list.add(h);
                }
            }
        }

        public void write(byte[] buf, int off, int len) {
            for (Handler h : new ArrayList<>(list)) {
                h.write(buf, off, len);
                if (h.done)
                    list.remove(h);
            }
        }

        public void close() {
        }

    }

    public static class ExtDetector extends FileTypeDetector {
        ArrayList<Handler> list = new ArrayList<>();

        public static class Handler extends FileTypeDetector.Handler {
            public Handler(String ext) {
                super(ext);
                clear();
            }

            public Handler(String ext, String str) {
                super(ext, str);
            }

            public Handler(String ext, int[] b) {
                super(ext, b);
            }
        }

        public ExtDetector(Detector[] dd) {
            super(dd);
            for (Detector d : dd) {
                if (d instanceof Handler) {
                    Handler h = (Handler) d;
                    h.clear();
                    list.add(h);
                }
            }
        }

        public void detect(Storage storage, Uri u) {
            String name = storage.getName(u);
            String e = Storage.getExt(name).toLowerCase();
            for (Handler h : list) {
                if (h.done && h.detected && e.equals(h.ext)) {
                    h.detected = true;
                    h.done = true;
                } else {
                    h.detected = false;
                }
            }
        }

        public void close() {
        }

    }

    public static class FileTypeDetectorZipExtract extends FileTypeDetectorZip {
        public static class Handler extends FileTypeDetectorZip.Handler {
            public Handler(String ext) {
                super(ext);
            }

            public String extract(File f, File t) {
                return null;
            }

            public String extract(ZipInputStream zip, File t) {
                try {
                    MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
                    FileOutputStream os = new FileOutputStream(t);

                    byte[] buf = new byte[BUF_SIZE];
                    int len;
                    while ((len = zip.read(buf)) > 0) {
                        digest.update(buf, 0, len);
                        os.write(buf, 0, len);
                    }

                    os.close();
                    return Storage.toHex(digest.digest());
                } catch (RuntimeException r) {
                    throw r;
                } catch (Exception r) {
                    throw new RuntimeException(r);
                }
            }

            public String extract(ZipEntry e, File f, File t) {
                try {
                    MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
                    ZipFile zip = new ZipFile(f);
                    InputStream is = zip.getInputStream(e);
                    FileOutputStream os = new FileOutputStream(t);

                    byte[] buf = new byte[BUF_SIZE];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        digest.update(buf, 0, len);
                        os.write(buf, 0, len);
                    }

                    os.close();
                    is.close();
                    return Storage.toHex(digest.digest());
                } catch (RuntimeException r) {
                    throw r;
                } catch (Exception r) {
                    throw new RuntimeException(r);
                }
            }
        }

        public FileTypeDetectorZipExtract(Detector[] dd) {
            super(dd);
        }
    }

    public static class FileTypeDetectorIO {
        ParcelFileDescriptor.AutoCloseInputStream is;
        ParcelFileDescriptor.AutoCloseOutputStream os;

        public static class Handler extends Detector {
            public Handler(String ext) {
                super(ext);
            }
        }

        public FileTypeDetectorIO() {
            try {
                ParcelFileDescriptor[] pp = ParcelFileDescriptor.createPipe();
                is = new ParcelFileDescriptor.AutoCloseInputStream(pp[0]);
                os = new ParcelFileDescriptor.AutoCloseOutputStream(pp[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void write(byte[] buf, int off, int len) {
            try {
                os.write(buf, off, len);
            } catch (IOException e) { // ignore expcetions, stream can be closed by reading thread
            }
        }

        public void close() {
            try {
                os.close();
            } catch (IOException e) {
            }
        }
    }

    public static class FileTypeDetectorZip extends FileTypeDetectorIO {
        ArrayList<Handler> list = new ArrayList<>();
        Thread thread;

        public static class Handler extends FileTypeDetectorIO.Handler {
            public Handler(String ext) {
                super(ext);
            }

            public void nextEntry(ZipEntry entry) {
            }
        }

        public FileTypeDetectorZip(Detector[] dd) {
            super();

            for (Detector d : dd) {
                if (d instanceof Handler) {
                    Handler h = (Handler) d;
                    h.clear();
                    list.add(h);
                }
            }

            thread = new Thread("zip detector") {
                @Override
                public void run() {
                    ZipInputStream zip = null;
                    try {
                        zip = new ZipInputStream(is); // throws MALFORMED if encoding is incorrect
                        ZipEntry entry;
                        while ((entry = zip.getNextEntry()) != null) {
                            for (Handler h : new ArrayList<>(list)) {
                                h.nextEntry(entry);
                                if (h.done)
                                    list.remove(h);
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "zip Error", e);
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e1) {
                        }
                        try {
                            zip.close();
                        } catch (IOException e) {
                        }
                    }
                }
            };
            thread.start();
        }

        public void close() {
            super.close();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static class FileTypeDetectorXml extends FileTypeDetectorIO {
        ArrayList<Handler> list = new ArrayList<>();
        Thread thread;

        public static class Handler extends FileTypeDetectorIO.Handler {
            boolean first = true;
            String firstTag;

            public Handler(String ext, String firstTag) {
                super(ext);
                this.firstTag = firstTag;
            }

            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                if (first) {
                    if (firstTag != null) {
                        done = true;
                        if (localName.equals(firstTag)) {
                            detected = true;
                        }
                    }
                }
                first = false;
            }

            @Override
            public void clear() {
                super.clear();
                first = true;
            }
        }

        public FileTypeDetectorXml(Detector[] dd) {
            for (Detector d : dd) {
                if (d instanceof Handler) {
                    Handler h = (Handler) d;
                    h.clear();
                    list.add(h);
                }
            }

            thread = new Thread("xml detector") {
                @Override
                public void run() {
                    try {
                        SAXParserFactory saxPF = SAXParserFactory.newInstance();
                        SAXParser saxP = saxPF.newSAXParser();
                        XMLReader xmlR = saxP.getXMLReader();
                        DefaultHandler myXMLHandler = new DefaultHandler() {
                            @Override
                            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                                super.startElement(uri, localName, qName, attributes);
                                for (Handler h : new ArrayList<>(list)) {
                                    h.startElement(uri, localName, qName, attributes);
                                    if (h.done)
                                        list.remove(h);
                                }
                            }
                        };
                        xmlR.setContentHandler(myXMLHandler);
                        xmlR.parse(new InputSource(is));
                    } catch (Exception e) {
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                        }
                    }
                }
            };
            thread.start();
        }

        public void close() {
            super.close();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static class FileFB2 extends FileTypeDetectorXml.Handler {
        public FileFB2() {
            super("fb2", "FictionBook");
        }
    }

    public static class FileFB2Zip extends FileTypeDetectorZipExtract.Handler {
        ZipEntry e;

        public FileFB2Zip() {
            super("fb2");
        }

        @Override
        public void nextEntry(ZipEntry entry) {
            if (Storage.getExt(entry.getName()).toLowerCase().equals("fb2")) {
                e = entry;
                detected = true;
                done = true;
            }
        }

        @Override
        public String extract(File f, File t) {
            return extract(e, f, t);
        }
    }

    public static class FileTxtZip extends FileTypeDetectorZipExtract.Handler {
        ZipEntry e;

        public FileTxtZip() {
            super("txt");
        }

        @Override
        public void nextEntry(ZipEntry entry) {
            if (Storage.getExt(entry.getName()).toLowerCase().equals("txt")) {
                e = entry;
                detected = true;
                done = true;
            }
        }

        @Override
        public String extract(File f, File t) {
            return extract(e, f, t);
        }
    }

    public static class FileRTFZip extends FileTypeDetectorZipExtract.Handler {
        ZipEntry e;

        public FileRTFZip() {
            super("rtf");
        }

        @Override
        public void nextEntry(ZipEntry entry) {
            if (Storage.getExt(entry.getName()).toLowerCase().equals("rtf")) {
                e = entry;
                detected = true;
                done = true;
            }
        }

        @Override
        public String extract(File f, File t) {
            return extract(e, f, t);
        }
    }

    public static class FileHTMLZip extends FileTypeDetectorZipExtract.Handler {
        ZipEntry e;

        public FileHTMLZip() {
            super("html");
        }

        @Override
        public void nextEntry(ZipEntry entry) {
            String ext = Storage.getExt(entry.getName()).toLowerCase();
            if (ext.equals("html")) {
                e = entry;
                detected = true;
                done = true;
            }
        }

        @Override
        public String extract(File f, File t) {
            return extract(e, f, t);
        }
    }

    public static class FileHTML extends FileTypeDetectorXml.Handler {

        public FileHTML() {
            super("html", "html");
        }

    }

    public static class FileEPUB extends FileTypeDetectorZip.Handler {
        public FileEPUB() {
            super("epub");
        }

        @Override
        public void nextEntry(ZipEntry entry) {
            if (entry.getName().equals("META-INF/container.xml")) {
                detected = true;
                done = true;
            }
        }
    }

    public static class FilePDF extends FileTypeDetector.Handler {
        public FilePDF() {
            super("pdf", "%PDF-");
        }
    }

    public static class FileDjvu extends FileTypeDetector.Handler {
        public FileDjvu() {
            super("djvu", "AT&TF");
        }
    }

    public static class FileDoc extends FileTypeDetector.Handler {
        public FileDoc() {
            super("doc", new int[]{0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1, 0});
        }
    }

    public static class FileRTF extends FileTypeDetector.Handler {
        public FileRTF() {
            super("rtf", "{\\rtf1");
        }
    }

    public static class FileMobi extends FileTypeDetector.Handler { // PdbReader.cpp
        byte[] m = "BOOKMOBI".getBytes(Charset.defaultCharset());

        public FileMobi() {
            super("mobi");
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            if (os.size() >= 68) { // 60 offset + 8 len
                done = true;
                byte[] head = head(os.toByteArray(), 68);
                byte[] id = tail(head, 8);
                detected = equals(id, m);
            }
        }
    }

    public static class FileCbz extends ExtDetector.Handler {
        public FileCbz() {
            super(ComicsPlugin.EXTZ, new int[]{0x50, 0x4B, 0x03, 0x04});
        }
    }

    public static class FileCbr extends ExtDetector.Handler {
        public FileCbr() {
            super(ComicsPlugin.EXTR, "Rar!");
        }
    }

    // https://stackoverflow.com/questions/898669/how-can-i-detect-if-a-file-is-binary-non-text-in-python
    public static class FileTxt extends FileTypeDetector.Handler {
        public static final int F = 0; /* character never appears in text */
        public static final int T = 1; /* character appears in plain ASCII text */
        public static final int I = 2; /* character appears in ISO-8859 text */
        public static final int X = 3; /* character appears in non-ISO extended ASCII (Mac, IBM PC) */
        public static final int R = 4; // lib.ru formatting, ^T and ^U

        // https://github.com/file/file/blob/f2a6e7cb7db9b5fd86100403df6b2f830c7f22ba/src/encoding.c#L151-L228
        byte[] text_chars = new byte[]
                {
                        /*                  BEL BS HT LF VT FF CR    */
                        F, F, F, F, F, F, F, T, T, T, T, T, T, T, F, F,  /* 0x0X */
                        /*                              ESC          */
                        F, F, F, F, R, R, F, F, F, F, F, T, F, F, F, F,  /* 0x1X */
                        T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T,  /* 0x2X */
                        T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T,  /* 0x3X */
                        T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T,  /* 0x4X */
                        T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T,  /* 0x5X */
                        T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T,  /* 0x6X */
                        T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, F,  /* 0x7X */
                        /*            NEL                            */
                        X, X, X, X, X, T, X, X, X, X, X, X, X, X, X, X,  /* 0x8X */
                        X, X, X, X, X, X, X, X, X, X, X, X, X, X, X, X,  /* 0x9X */
                        I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I,  /* 0xaX */
                        I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I,  /* 0xbX */
                        I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I,  /* 0xcX */
                        I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I,  /* 0xdX */
                        I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I,  /* 0xeX */
                        I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I   /* 0xfX */
                };

        public int count = 0;

        public FileTxt() {
            super("txt");
        }

        public FileTxt(String ext) {
            super(ext);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            int end = off + len;
            for (int i = off; i < end; i++) {
                int b = buf[i] & 0xFF;
                for (int k = 0; k < text_chars.length; k++) {
                    if (text_chars[b] == F) {
                        done = true;
                        detected = false;
                        return;
                    }
                    count++;
                }
            }
            if (count >= 1000) {
                done = true;
                detected = true;
            }
        }
    }

    public static class FBook {
        public File tmp; // tmp file
        public org.geometerplus.fbreader.book.Book book;
        public RecentInfo info;

        public void close() {
            if (tmp != null) {
                tmp.delete();
                tmp = null;
            }
            book = null;
        }
    }

    public static class Book {
        public Uri url;
        public String ext;
        public String md5; // can be filename if user renamed file
        public RecentInfo info;
        public File cover;

        public Book() {
        }

        public Book(Uri u) {
            String name = Storage.getDocumentName(u);
            url = u;
            md5 = Storage.getNameNoExt(name);
            ext = Storage.getExt(name);
        }
    }

    public static class RecentInfo {
        public long created; // date added to the my readings
        public long last; // last write time
        public ZLTextPosition position;
        public String authors;
        public String title;
        public Map<String, ZLPaintContext.ScalingType> scales = new HashMap<>(); // individual scales
        public FBView.ImageFitting scale; // all images
        public Integer fontsize; // FBView size or Reflow / 100
        public Bookmarks bookmarks;

        public RecentInfo() {
        }

        public RecentInfo(RecentInfo info) {
            created = info.created;
            last = info.last;
            if (info.position != null)
                position = new ZLTextFixedPosition(info.position);
            authors = info.authors;
            title = info.title;
            scale = info.scale;
            scales = new HashMap<>(info.scales);
            fontsize = info.fontsize;
            if (info.bookmarks != null)
                bookmarks = new Bookmarks(info.bookmarks);
        }

        public RecentInfo(File f) {
            try {
                FileInputStream is = new FileInputStream(f);
                load(is);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public RecentInfo(Context context, Uri u) {
            try {
                ContentResolver resolver = context.getContentResolver();
                InputStream is;
                String s = u.getScheme();
                if (s.equals(ContentResolver.SCHEME_CONTENT)) {
                    is = resolver.openInputStream(u);
                } else if (s.equals(ContentResolver.SCHEME_FILE)) {
                    is = new FileInputStream(Storage.getFile(u));
                } else {
                    throw new UnknownUri();
                }
                load(is);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public RecentInfo(JSONObject o) throws JSONException {
            load(o);
        }

        public void load(InputStream is) throws Exception {
            String json = IOUtils.toString(is, Charset.defaultCharset());
            JSONObject j = new JSONObject(json);
            load(j);
            is.close();
        }

        public void load(JSONObject o) throws JSONException {
            created = o.optLong("created", 0);
            last = o.getLong("last");
            authors = o.optString("authors", null);
            title = o.optString("title", null);
            position = loadPosition(o.optJSONArray("position"));
            String scale = o.optString("scale");
            if (scale != null && !scale.isEmpty())
                this.scale = FBView.ImageFitting.valueOf(scale);
            Object scales = o.opt("scales");
            if (scales != null) {
                Map<String, Object> map = WebViewCustom.toMap((JSONObject) scales);
                for (String key : map.keySet()) {
                    String v = (String) map.get(key);
                    this.scales.put(key, ZLPaintContext.ScalingType.valueOf(v));
                }
            }
            fontsize = o.optInt("fontsize", -1);
            if (fontsize == -1)
                fontsize = null;
            JSONArray b = o.optJSONArray("bookmarks");
            if (b != null && b.length() > 0)
                bookmarks = new Bookmarks(b);
        }

        public JSONObject save() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("created", created);
            o.put("last", last);
            o.put("authors", authors);
            o.put("title", title);
            JSONArray p = savePosition(position);
            if (p != null)
                o.put("position", p);
            if (scale != null)
                o.put("scale", scale.name());
            if (!scales.isEmpty())
                o.put("scales", WebViewCustom.toJSON(scales));
            if (fontsize != null)
                o.put("fontsize", fontsize);
            if (bookmarks != null)
                o.put("bookmarks", bookmarks.save());
            return o;
        }

        public void merge(RecentInfo info) {
            if (created > info.created)
                created = info.created;
            if (position == null || last < info.last)
                position = new ZLTextFixedPosition(info.position);
            if (authors == null || last < info.last)
                authors = info.authors;
            if (title == null || last < info.last)
                title = info.title;
            if (scale == null || last < info.last)
                scale = info.scale;
            for (String k : info.scales.keySet()) {
                ZLPaintContext.ScalingType v = info.scales.get(k);
                if (last < info.last) // replace with new values
                    scales.put(k, v);
                else if (!scales.containsKey(k)) // only add non existent values to the list
                    scales.put(k, v);
            }
            if (fontsize == null || last < info.last)
                fontsize = info.fontsize;
            if (bookmarks == null) {
                bookmarks = info.bookmarks;
            } else if (info.bookmarks != null) {
                for (Bookmark b : info.bookmarks) {
                    boolean found = false;
                    for (int i = 0; i < bookmarks.size(); i++) {
                        Bookmark m = bookmarks.get(i);
                        if (b.start.samePositionAs(m.start) && b.end.samePositionAs(m.end) && m.last < b.last) {
                            found = true;
                            bookmarks.set(i, b);
                        }
                    }
                    if (!found)
                        bookmarks.add(b);
                }
            }
        }
    }

    public static class Bookmark {
        public long last; // last change event
        public String name;
        public String text;
        public int color;
        public ZLTextPosition start;
        public ZLTextPosition end;

        public Bookmark() {
        }

        public Bookmark(Bookmark b) {
            last = b.last;
            name = b.name;
            text = b.text;
            color = b.color;
            start = b.start;
            end = b.end;
        }

        public Bookmark(String t, ZLTextPosition s, ZLTextPosition e) {
            last = System.currentTimeMillis();
            text = t;
            start = s;
            end = e;
        }

        public Bookmark(JSONObject j) throws JSONException {
            load(j);
        }

        public void load(JSONObject j) throws JSONException {
            last = j.optLong("last");
            name = j.optString("name");
            text = j.optString("text");
            color = j.optInt("color");
            start = loadPosition(j.optJSONArray("start"));
            end = loadPosition(j.optJSONArray("end"));
        }

        public JSONObject save() throws JSONException {
            JSONObject j = new JSONObject();
            j.put("last", last);
            if (name != null)
                j.put("name", name);
            j.put("text", text);
            j.put("color", color);
            JSONArray s = savePosition(start);
            if (s != null)
                j.put("start", s);
            JSONArray e = savePosition(end);
            if (e != null)
                j.put("end", e);
            return j;
        }

        public boolean equals(Bookmark b) {
            if (name != null && b.name != null) {
                if (!name.equals(b.name))
                    return false;
            } else {
                return false;
            }
            if (color != b.color)
                return false;
            if (!text.equals(b.text))
                return false;
            return start.samePositionAs(b.start) && end.samePositionAs(b.end);
        }
    }

    public static class Bookmarks extends ArrayList<Bookmark> {
        public Bookmarks() {
        }

        public Bookmarks(Bookmarks bb) {
            for (Bookmark b : bb)
                add(new Bookmark(b));
        }

        public Bookmarks(JSONArray a) throws JSONException {
            load(a);
        }

        public JSONArray save() throws JSONException {
            JSONArray a = new JSONArray();
            for (Bookmark b : this)
                a.put(b.save());
            return a;
        }

        public void load(JSONArray json) throws JSONException {
            for (int i = 0; i < json.length(); i++) {
                add(new Bookmark(json.getJSONObject(i)));
            }
        }

        public ArrayList<Bookmark> getBookmarks(PluginView.Selection.Page page) {
            ArrayList<Bookmark> list = new ArrayList<>();
            for (Bookmark b : this) {
                if (b.start.getParagraphIndex() == page.page || b.end.getParagraphIndex() == page.page)
                    list.add(b);
            }
            return list;
        }

        public boolean equals(Bookmarks b) {
            int s = b.size();
            if (size() != s)
                return false;
            for (int i = 0; i < s; i++) {
                if (!get(i).equals(b.get(i)))
                    return false;
            }
            return true;
        }
    }

    public Storage(Context context) {
        super(context);
    }

    public void save(Book book) {
        book.info.last = System.currentTimeMillis();
        Uri u = recentUri(book);
        String s = u.getScheme();
        if (Build.VERSION.SDK_INT >= 21 && s.equals(ContentResolver.SCHEME_CONTENT)) {
            Uri root = Storage.getDocumentTreeUri(u);
            Uri o = createFile(root, Storage.getDocumentChildPath(u));
            ContentResolver resolver = context.getContentResolver();
            ParcelFileDescriptor fd;
            try {
                fd = resolver.openFileDescriptor(o, "rw");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            FileDescriptor out = fd.getFileDescriptor();
            try {
                String json = book.info.save().toString(2);
                Writer w = new FileWriter(out);
                IOUtils.write(json, w);
                w.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (s.equals(ContentResolver.SCHEME_FILE)) {
            try {
                File f = Storage.getFile(u);
                String json = book.info.save().toString(2);
                Writer w = new FileWriter(f);
                IOUtils.write(json, w);
                w.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new UnknownUri();
        }
    }

    public Book load(Uri uri) {
        return load(uri, null);
    }

    public Book load(Uri uri, Progress progress) {
        Book book;
        String contentDisposition = null;
        String s = uri.getScheme();
        if (s.equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver resolver = context.getContentResolver();
            try {
                Cursor meta = resolver.query(uri, null, null, null, null);
                if (meta != null) {
                    try {
                        if (meta.moveToFirst()) {
                            contentDisposition = meta.getString(meta.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                            contentDisposition = Storage.getNameNoExt(contentDisposition);
                        }
                    } finally {
                        meta.close();
                    }
                }
                AssetFileDescriptor fd = resolver.openAssetFileDescriptor(uri, "r");
                InputStream is = new AssetFileDescriptor.AutoCloseInputStream(fd);
                long len = fd.getDeclaredLength();
                if (len == AssetFileDescriptor.UNKNOWN_LENGTH)
                    len = fd.getLength();
                is = new BufferedInputStream(is);
                if (progress != null)
                    is = new ProgresInputstream(is, len, progress);
                book = load(is, uri);
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (s.startsWith(WebViewCustom.SCHEME_HTTP)) {
            try {
                InputStream is;
                if (Build.VERSION.SDK_INT < 11) {
                    HttpURLConnection conn = HttpClient.openConnection(uri, HttpClient.USER_AGENT);
                    is = conn.getInputStream();
                    is = new BufferedInputStream(is);
                    if (progress != null)
                        is = new ProgresInputstream(is, conn.getContentLength(), progress);
                } else {
                    HttpClient client = new HttpClient();
                    HttpClient.DownloadResponse w = client.getResponse(null, uri.toString());
                    if (w.getError() != null)
                        throw new RuntimeException(w.getError() + ": " + uri);
                    if (w.contentDisposition != null) {
                        Pattern cp = Pattern.compile("filename=[\"]*([^\"]*)[\"]*");
                        Matcher cm = cp.matcher(w.contentDisposition);
                        if (cm.find()) {
                            contentDisposition = cm.group(1);
                            contentDisposition = Storage.getNameNoExt(contentDisposition);
                        }
                    }
                    is = new BufferedInputStream(w.getInputStream());
                    if (progress != null)
                        is = new ProgresInputstream(is, w.contentLength, progress);
                }
                book = load(is, uri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else { // file:// or /path/file
            File f = getFile(uri);
            try {
                FileInputStream fis = new FileInputStream(f);
                InputStream is = fis;
                is = new BufferedInputStream(is);
                if (progress != null)
                    is = new ProgresInputstream(is, fis.getChannel().size(), progress);
                book = load(is, Uri.fromFile(f));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Uri r = recentUri(book);
        if (exists(r)) {
            try {
                book.info = new RecentInfo(context, r);
            } catch (RuntimeException e) {
                Log.d(TAG, "Unable to load info", e);
            }
        }
        if (book.info == null) {
            book.info = new RecentInfo();
            book.info.created = System.currentTimeMillis();
        }
        load(book);
        if (book.info.title == null || book.info.title.isEmpty() || book.info.title.equals(book.md5)) {
            if (contentDisposition != null && !contentDisposition.isEmpty())
                book.info.title = contentDisposition;
            else
                book.info.title = Storage.getNameNoExt(uri.getLastPathSegment());
        }
        if (!exists(r))
            save(book);
        return book;
    }

    public File getCache() {
        File cache = context.getExternalCacheDir();
        if (cache == null || !canWrite(cache))
            cache = context.getCacheDir();
        return cache;
    }

    public File createTempBook(String ext) throws IOException {
        return File.createTempFile("book", "." + ext, getCache());
    }

    public Book load(InputStream is, Uri u) {
        Uri storage = getStoragePath();

        String s = storage.getScheme();

        if (Build.VERSION.SDK_INT >= 21 && s.equals(ContentResolver.SCHEME_CONTENT)) {
            String ss = u.getScheme();
            if (ss.equals(ContentResolver.SCHEME_CONTENT) && DocumentsContract.getDocumentId(u).startsWith(DocumentsContract.getTreeDocumentId(storage))) // else we can't get from content://storage to real path
                return new Book(DocumentsContract.buildDocumentUriUsingTree(storage, DocumentsContract.getDocumentId(u)));
        }
        if (s.equals(ContentResolver.SCHEME_FILE) && u.getPath().startsWith(storage.getPath()))
            return new Book(u);

        boolean tmp = false;
        File file = null;

        final Book book = new Book();
        try {
            OutputStream os = null;

            if (u.getScheme().equals(ContentResolver.SCHEME_FILE)) {
                file = Storage.getFile(u);
            } else {
                file = createTempBook("tmp");
                os = new FileOutputStream(file);
                os = new BufferedOutputStream(os);
                tmp = true;
            }

            Detector[] dd = supported();

            book.md5 = detecting(this, dd, is, os, u);

            for (Detector d : dd) {
                if (d.detected) {
                    book.ext = d.ext;
                    if (d instanceof FileTypeDetectorZipExtract.Handler) {
                        FileTypeDetectorZipExtract.Handler e = (FileTypeDetectorZipExtract.Handler) d;
                        if (!tmp) { // !tmp
                            File z = file;
                            file = createTempBook("tmp");
                            book.md5 = e.extract(z, file);
                            tmp = true; // force to delete 'fbook.file'
                        } else { // tmp
                            File tt = createTempBook("tmp");
                            book.md5 = e.extract(file, tt);
                            file.delete(); // delete old
                            file = tt; // tmp = true
                        }
                    }
                    break; // priority first - more imporant
                }
            }

            if (book.ext == null)
                throw new RuntimeException("Unsupported format");

            if (book.ext.equals(ComicsPlugin.EXTR)) { // handling cbz solid archives
                final FileInputStream fis = new FileInputStream(file);
                final FileChannel fc = fis.getChannel();
                File cbz = null;
                try {
                    final Archive archive = new Archive(new ComicsPlugin.RarStore(fc));
                    if (archive.getMainHeader().isSolid()) {
                        cbz = createTempBook("tmp");
                        OutputStream zos = new FileOutputStream(cbz);
                        zos = new BufferedOutputStream(zos);
                        ZipOutputStream out = new ZipOutputStream(zos);
                        List<FileHeader> list = archive.getFileHeaders();
                        for (FileHeader h : list) {
                            if (h.isDirectory())
                                continue;

                            ZipEntry entry = new ZipEntry(ComicsPlugin.getRarFileName(h));
                            out.putNextEntry(entry);

                            archive.extractFile(h, out);
                        }
                        out.close();
                        if (tmp)
                            file.delete();
                        book.ext = ComicsPlugin.EXTZ;
                        file = cbz;
                        tmp = true;
                    }
                } catch (Exception e) {
                    if (cbz != null)
                        cbz.delete();
                    throw new RuntimeException("unsupported rar", e);
                }
            }

            if (Build.VERSION.SDK_INT >= 21 && s.equals(ContentResolver.SCHEME_CONTENT)) {
                ContentResolver contentResolver = context.getContentResolver();
                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(storage, DocumentsContract.getTreeDocumentId(storage));
                Cursor childCursor = contentResolver.query(childrenUri, null, null, null, null);
                if (childCursor != null) {
                    while (childCursor.moveToNext()) {
                        String id = childCursor.getString(childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                        String t = childCursor.getString(childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                        String n = Storage.getNameNoExt(t);
                        String e = Storage.getExt(t);
                        if (n.equals(book.md5) && !e.equals(JSON_EXT)) { // delete all but json
                            Uri k = DocumentsContract.buildDocumentUriUsingTree(childrenUri, id);
                            delete(k);
                        }
                    }
                }
                String id = book.md5 + "." + book.ext;
                Uri o = createFile(storage, id);
                ContentResolver resolver = context.getContentResolver();

                ParcelFileDescriptor fd = resolver.openFileDescriptor(o, "rw");

                FileDescriptor out = fd.getFileDescriptor();
                FileInputStream fis = new FileInputStream(file);
                OutputStream fos = new FileOutputStream(out);
                fos = new BufferedOutputStream(fos);
                IOUtils.copy(fis, fos);
                fis.close();
                fos.close();

                book.url = o;

                if (tmp)
                    file.delete();
            } else if (s.equals(ContentResolver.SCHEME_FILE)) {
                File f = getFile(storage);
                File[] ff = f.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(book.md5);
                    }
                });
                if (ff != null) {
                    for (File k : ff) {
                        if (!getExt(k).toLowerCase().equals(JSON_EXT))
                            k.delete();
                    }
                }
                File to = new File(f, book.md5 + "." + book.ext);
                if (tmp)
                    Storage.move(file, to);
                else
                    Storage.copy(file, to);
                book.url = Uri.fromFile(to);
            } else {
                throw new UnknownUri();
            }
        } catch (RuntimeException e) {
            if (tmp && file != null)
                file.delete();
            throw e;
        } catch (IOException | NoSuchAlgorithmException e) {
            if (tmp && file != null)
                file.delete();
            throw new RuntimeException(e);
        }
        return book;
    }

    public ZLImage loadCover(FBook book) {
        try {
            FormatPlugin plugin = getPlugin(new Info(context), book);
            ZLFile file = BookUtil.fileByBook(book.book);
            return plugin.readCover(file);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void load(Book book) {
        if (book.info == null) {
            Uri r = recentUri(book);
            if (exists(r))
                try {
                    book.info = new RecentInfo(context, r);
                } catch (RuntimeException e) {
                    Log.d(TAG, "Unable to load info", e);
                }
        }
        if (book.info == null) {
            book.info = new RecentInfo();
            book.info.created = System.currentTimeMillis();
        }
        FBook fbook = null;
        if (book.info.authors == null || book.info.authors.isEmpty()) {
            if (fbook == null)
                fbook = read(book);
            book.info.authors = fbook.book.authorsString(", ");
        }
        if (book.info.title == null || book.info.title.isEmpty() || book.info.title.equals(book.md5)) {
            if (fbook == null)
                fbook = read(book);
            book.info.title = getTitle(book, fbook);
        }
        if (fbook != null)
            fbook.close();
    }

    public void createCover(FBook fbook, File cover) {
        ZLImage image = loadCover(fbook);
        if (image != null) {
            Bitmap bm = null;
            if (image instanceof ZLFileImageProxy) {
                ZLFileImageProxy p = (ZLFileImageProxy) image;
                if (!p.isSynchronized())
                    p.synchronize();
                image = p.getRealImage();
            }
            if (image instanceof ZLStreamImage) {
                bm = BitmapFactory.decodeStream(((ZLStreamImage) image).inputStream());
            }
            if (image instanceof ZLBitmapImage) {
                bm = ((ZLBitmapImage) image).getBitmap();
            }
            boolean a = fbook.book.authors() != null && !fbook.book.authors().isEmpty();
            boolean t = fbook.book.getTitle() != null && !fbook.book.getTitle().isEmpty();
            if (bm == null && (a || t)) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                View v = inflater.inflate(R.layout.cover_generate, null);
                TextView aa = (TextView) v.findViewById(R.id.author);
                aa.setText(fbook.book.authorsString(", "));
                TextView tt = (TextView) v.findViewById(R.id.title);
                tt.setText(fbook.book.getTitle());
                bm = renderView(v);
            }
            if (bm == null) {
                FBReaderView v = new FBReaderView(getContext());
                v.loadBook(fbook);
                bm = renderView(v);
            }
            if (bm == null)
                return;
            try {
                float ratio = COVER_SIZE / (float) bm.getWidth();
                Bitmap sbm = Bitmap.createScaledBitmap(bm, (int) (bm.getWidth() * ratio), (int) (bm.getHeight() * ratio), true);
                if (sbm != bm)
                    bm.recycle();
                OutputStream os = new FileOutputStream(cover);
                os = new BufferedOutputStream(os);
                sbm.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.close();
                sbm.recycle();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    Bitmap renderView(View v) {
        DisplayMetrics m = getContext().getResources().getDisplayMetrics();
        int w = (int) (720 * m.density / 2);
        int h = (int) (1280 * m.density / 2);
        int ws = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY);
        int hs = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY);
        v.measure(ws, hs);
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        v.draw(c);
        return bm;
    }

    public void list(ArrayList<Book> list, File storage) {
        if (storage == null)
            return;
        File[] ff = storage.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String n = Storage.getNameNoExt(name);
                String e = getExt(name);
                e = e.toLowerCase();
                if (n.length() != MD5_SIZE)
                    return false;
                Detector[] dd = supported();
                for (Detector d : dd) {
                    if (e.equals(d.ext))
                        return true;
                }
                return false;
            }
        });
        if (ff == null)
            return;
        for (File f : ff) {
            Book b = new Book();
            b.md5 = getNameNoExt(f);
            b.url = Uri.fromFile(f);
            File cover = coverFile(context, b);
            if (cover.exists())
                b.cover = cover;
            File r = recentFile(b);
            if (r.exists()) {
                try {
                    b.info = new RecentInfo(r);
                } catch (RuntimeException e) {
                    Log.d(TAG, "Unable to load info", e);
                }
            }
            if (b.info == null) {
                b.info = new RecentInfo();
                b.info.created = System.currentTimeMillis();
            }
            list.add(b);
        }
    }

    public ArrayList<Book> list() {
        Uri uri = getStoragePath();
        ArrayList<Book> list = new ArrayList<>();
        String s = uri.getScheme();
        if (Build.VERSION.SDK_INT >= 21 && s.startsWith(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver contentResolver = context.getContentResolver();
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
            Cursor childCursor = contentResolver.query(childrenUri, null, null, null, null);
            if (childCursor != null) {
                try {
                    while (childCursor.moveToNext()) {
                        String id = childCursor.getString(childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                        String t = childCursor.getString(childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                        long size = childCursor.getLong(childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE));
                        if (size > 0) {
                            t = t.toLowerCase();
                            String n = Storage.getNameNoExt(t);
                            if (n.length() != MD5_SIZE)
                                continue;
                            Detector[] dd = supported();
                            for (Detector d : dd) {
                                if (t.endsWith("." + d.ext)) {
                                    Uri k = DocumentsContract.buildDocumentUriUsingTree(uri, id);
                                    Book b = new Book();
                                    b.md5 = getNameNoExt(k);
                                    b.url = k;
                                    File cover = coverFile(context, b);
                                    if (cover.exists())
                                        b.cover = cover;
                                    Uri r = recentUri(b);
                                    if (exists(r)) {
                                        try {
                                            b.info = new RecentInfo(context, r);
                                        } catch (RuntimeException e) {
                                            Log.d(TAG, "Unable to load info", e);
                                        }
                                    }
                                    if (b.info == null) {
                                        b.info = new RecentInfo();
                                        b.info.created = System.currentTimeMillis();
                                    }
                                    list.add(b);
                                    break; // break dd
                                }
                            }
                        }
                    }
                } finally {
                    childCursor.close();
                }
            }
        } else if (s.startsWith(ContentResolver.SCHEME_FILE)) {
            File dir = getFile(uri);
            list(list, dir);
        } else {
            throw new UnknownUri();
        }
        return list;
    }

    public void delete(final Book book) {
        delete(book.url);
        if (book.cover != null)
            book.cover.delete();

        delete(recentUri(book));

        // delete all md5.* files (old, cover images, and sync conflicts files)
        Uri storage = getStoragePath();
        String s = storage.getScheme();
        if (Build.VERSION.SDK_INT >= 21 && s.equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver contentResolver = context.getContentResolver();
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(storage, DocumentsContract.getTreeDocumentId(storage));
            Cursor childCursor = contentResolver.query(childrenUri, null, null, null, null);
            if (childCursor != null) {
                try {
                    while (childCursor.moveToNext()) {
                        String id = childCursor.getString(childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                        String t = childCursor.getString(childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                        if (t.startsWith(book.md5)) { // delete all but json
                            Uri k = DocumentsContract.buildDocumentUriUsingTree(storage, id);
                            delete(k);
                        }
                    }
                } finally {
                    childCursor.close();
                }
            }
        } else if (s.equals(ContentResolver.SCHEME_FILE)) {
            File dir = getFile(storage);
            File[] ff = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(book.md5);
                }
            });
            if (ff != null) {
                for (File f : ff) {
                    f.delete();
                }
            }
        } else {
            throw new UnknownUri();
        }
    }

    public FBook read(Book b) {
        try {
            FBook fbook = new FBook();
            if (b.info != null)
                fbook.info = new RecentInfo(b.info);

            File file;

            String s = b.url.getScheme();
            if (s.equals(ContentResolver.SCHEME_CONTENT)) {
                String ext = getExt(b.url);
                fbook.tmp = createTempBook(ext);
                OutputStream os = new FileOutputStream(fbook.tmp);
                os = new BufferedOutputStream(os);
                ContentResolver resolver = getContext().getContentResolver();
                InputStream is = resolver.openInputStream(b.url);
                IOUtils.copy(is, os);
                file = fbook.tmp;
                is.close();
                os.close();
            } else if (s.equals(ContentResolver.SCHEME_FILE)) {
                file = getFile(b.url);
            } else {
                throw new UnknownUri();
            }

            String ext = getExt(file).toLowerCase();
            if (ext.equals(Storage.ZIP_EXT)) { // handle zip files manually, better perfomance
                Detector[] dd = supported();
                try {
                    InputStream is = new FileInputStream(file);
                    Storage.detecting(this, dd, is, null, Uri.fromFile(file));
                } catch (IOException | NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                for (Storage.Detector d : dd) {
                    if (d.detected) {
                        if (d instanceof FileTypeDetectorZipExtract.Handler) {
                            FileTypeDetectorZipExtract.Handler e = (FileTypeDetectorZipExtract.Handler) d;
                            if (fbook.tmp == null) { // !tmp
                                File z = file;
                                file = createTempBook(d.ext);
                                e.extract(z, file);
                                fbook.tmp = file;
                            } else { // tmp
                                File tt = createTempBook(d.ext);
                                e.extract(file, tt);
                                file.delete(); // delete old
                                fbook.tmp = tt;
                                file = tt;
                            }
                        }
                        break; // priority first - more imporant
                    }
                }
            }

            fbook.book = new org.geometerplus.fbreader.book.Book(-1, file.getPath(), null, null, null);
            FormatPlugin plugin = Storage.getPlugin(new Info(context), fbook);
            try {
                plugin.readMetainfo(fbook.book);
            } catch (BookReadingException e) {
                throw new RuntimeException(e);
            }

            return fbook;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Uri getStoragePath() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        String path = shared.getString(BookApplication.PREFERENCE_STORAGE, null);
        if (path == null)
            return Uri.fromFile(getLocalStorage());
        else
            return getStoragePath(path);
    }

    public static void migrateLocalStorageDialog(final Context context, final Handler handler, final Storage storage) {
        int dp10 = ThemeUtils.dp2px(context, 10);
        ProgressBar progress = new ProgressBar(context);
        progress.setIndeterminate(true);
        progress.setPadding(dp10, dp10, dp10, dp10);
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("Migrating data");
        b.setView(progress);
        b.setCancelable(false);
        final AlertDialog dialog = b.create();
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    storage.migrateLocalStorage();
                } catch (final RuntimeException e) {
                    Log.d(TAG, "migrate error", e);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.cancel();
                    }
                });
            }
        });
        dialog.show();
        thread.start();
    }

    public void migrateLocalStorage() {
        migrateLocalStorage(getLocalInternal());
        migrateLocalStorage(getLocalExternal());
    }

    public void migrateLocalStorage(File l) {
        if (l == null)
            return;

        if (!canWrite(l))
            return;

        Uri path = getStoragePath();

        String s = path.getScheme();
        if (s.equals(ContentResolver.SCHEME_FILE)) {
            File p = getFile(path);
            if (!canWrite(p))
                return;
            if (l.equals(p)) // same storage path
                return;
        }

        Uri u = Uri.fromFile(l);
        if (u.equals(path)) // same storage path
            return;

        File[] ff = l.listFiles();

        if (ff == null)
            return;

        for (File f : ff) {
            if (!f.isFile())
                continue;
            boolean m = false;
            String e = Storage.getExt(f).toLowerCase();
            if (e.equals(JSON_EXT))
                m = true;
            else {
                Detector[] dd = supported();
                for (Detector d : dd) {
                    if (e.equals(d.ext))
                        m = true;
                }
            }
            if (m)
                migrate(f, path);
        }
    }

    public Uri move(Uri u, Uri dir) {
        try {
            Uri n = getNextFile(getStoragePath(), getDocumentName(u), Storage.JSON_EXT);
            InputStream is;
            OutputStream os;
            String s = u.getScheme();
            if (Build.VERSION.SDK_INT >= 21 && s.startsWith(ContentResolver.SCHEME_CONTENT)) {
                ContentResolver resolver = getContext().getContentResolver();
                is = resolver.openInputStream(u);
                n = createFile(dir, Storage.getDocumentChildPath(n));
                os = resolver.openOutputStream(n);
            } else if (s.startsWith(ContentResolver.SCHEME_FILE)) {
                is = new FileInputStream(Storage.getFile(u));
                os = new FileOutputStream(Storage.getFile(n));
                os = new BufferedOutputStream(os);
            } else {
                throw new UnknownUri();
            }
            IOUtils.copy(is, os);
            is.close();
            os.close();
            delete(u);
            return n;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
