package com.github.axet.bookreader.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.github.axet.androidlibrary.app.Natives;
import com.github.axet.androidlibrary.net.HttpClient;
import com.github.axet.androidlibrary.widgets.WebViewCustom;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.widgets.FBReaderView;

import org.apache.commons.io.IOUtils;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.formats.BookReadingException;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLFileImageProxy;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.image.ZLStreamImage;
import org.geometerplus.zlibrary.core.util.SystemInfo;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Storage extends com.github.axet.androidlibrary.app.Storage {

    public static String TAG = Storage.class.getCanonicalName();

    public static final int MD5_SIZE = 32;
    public static final int COVER_SIZE = 128;
    public static final int BUF_SIZE = 1024;
    public static final String COVER_EXT = "png";
    public static final String JSON_EXT = "json";

    public static ZLAndroidApplication zlib;
    public static Storage.Info systeminfo;

    public static void init(final Context context) {
        if (Storage.systeminfo == null)
            Storage.systeminfo = new Storage.Info(context);
        if (Storage.zlib == null) {
            Storage.zlib = new ZLAndroidApplication() {
                {
                    attachBaseContext(context);
                    onCreate();
                }
            };
        }
    }

    public static void K2PdfOptInit(Context context) {
        if (com.github.axet.k2pdfopt.Config.natives) {
            Natives.loadLibraries(context, "willus", "k2pdfopt", "k2pdfoptjni");
            com.github.axet.k2pdfopt.Config.natives = false;
        }
    }

    public static Detector[] supported() {
        return new Detector[]{new FileFB2(), new FileFB2Zip(), new FileEPUB(), new FileHTML(), new FileHTMLZip(),
                new FilePDF(), new FileDjvu(), new FileRTF(), new FileRTFZip(), new FileDoc(),
                new FileMobi(), new FileTxt(), new FileTxtZip()};
    }

    public static FormatPlugin getPlugin(PluginCollection c, Storage.Book b) {
        ZLFile f = BookUtil.fileByBook(b.book);
        switch (f.getExtension()) {
            case PDFPlugin.EXT:
                return new PDFPlugin();
            case DjvuPlugin.EXT:
                return new DjvuPlugin();
        }
        try {
            return BookUtil.getPlugin(c, b.book);
        } catch (BookReadingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Info implements SystemInfo {
        Context context;

        public Info(Context context) {
            this.context = context;
        }

        @Override
        public String tempDirectory() {
            return context.getFilesDir().getPath();
        }

        @Override
        public String networkCacheDirectory() {
            return context.getFilesDir().getPath();
        }
    }

    public static String getName(Book book) {
        String a = book.book.authorsString(", ");
        String t = book.book.getTitle();
        if (t.equals(book.md5))
            t = null;
        String m;
        if (a == null && t == null) {
            return null;
        } else if (a == null)
            m = t;
        else if (t == null)
            m = a;
        else
            m = a + " - " + t;
        return m;
    }

    public static String getTitle(Book book) {
        String t = book.book.getTitle();
        if (t.equals(book.md5))
            t = null;
        return t;
    }

    public static File coverFile(Book book) {
        File p = book.file.getParentFile();
        return new File(p, book.md5 + "." + COVER_EXT);
    }

    public static File recentFile(Book book) {
        File p = book.file.getParentFile();
        return new File(p, book.md5 + "." + JSON_EXT);
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

    public static class FileTypeDetectorZipExtract extends FileTypeDetectorZip {
        public static class Handler extends FileTypeDetectorZip.Handler {
            public Handler(String ext) {
                super(ext);
            }

            public String extract(File f, File t) {
                return null;
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

    public static class FileTypeDetectorZip {
        ArrayList<Handler> list = new ArrayList<>();
        ParcelFileDescriptor.AutoCloseOutputStream os;
        Thread thread;

        public static class Handler extends Detector {
            public Handler(String ext) {
                super(ext);
            }

            public void nextEntry(ZipEntry entry) {
            }
        }

        public FileTypeDetectorZip(Detector[] dd) {
            for (Detector d : dd) {
                if (d instanceof Handler) {
                    Handler h = (Handler) d;
                    h.clear();
                    list.add(h);
                }
            }

            final ParcelFileDescriptor.AutoCloseInputStream is;
            try {
                ParcelFileDescriptor[] pp = ParcelFileDescriptor.createPipe();
                is = new ParcelFileDescriptor.AutoCloseInputStream(pp[0]);
                os = new ParcelFileDescriptor.AutoCloseOutputStream(pp[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
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

        public void write(byte[] buf, int off, int len) {
            try {
                os.write(buf, off, len);
            } catch (IOException e) {
            }
        }

        public void close() {
            try {
                os.close();
            } catch (IOException e) {
            }
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static class FileTypeDetectorXml {
        ArrayList<Handler> list = new ArrayList<>();
        ParcelFileDescriptor.AutoCloseInputStream is;
        ParcelFileDescriptor.AutoCloseOutputStream os;
        Thread thread;

        public static class Handler extends Detector {
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

            try {
                ParcelFileDescriptor[] pp = ParcelFileDescriptor.createPipe();
                is = new ParcelFileDescriptor.AutoCloseInputStream(pp[0]);
                os = new ParcelFileDescriptor.AutoCloseOutputStream(pp[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
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

        public void write(byte[] buf, int off, int len) {
            try {
                os.write(buf, off, len);
            } catch (IOException e) {
            }
        }

        public void close() {
            try {
                os.close();
            } catch (IOException e) {
            }
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

    public static class Book {
        public File file;
        public String md5;
        public org.geometerplus.fbreader.book.Book book;
        public String ext;
        public RecentInfo info;
        public File cover;

        public boolean isLoaded() {
            return book != null;
        }

        public File[] exists(File s) {
            File[] ff = s.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(md5);
                }
            });
            if (ff == null)
                return null;
            if (ff.length == 0)
                return null;
            return ff;
        }

        public void store(File s, boolean tmp) {
            File f = new File(s, md5 + "." + ext);
            if (f.equals(file))
                return;
            File[] ee = exists(s);
            if (ee == null) {
                if (tmp)
                    file = Storage.move(file, f);
                else
                    file = Storage.copy(file, f);
            } else {
                boolean same = false;
                for (File e : ee) {
                    if (e.equals(file)) {
                        same = true;
                        break;
                    }
                    if (getExt(e).toLowerCase().equals(ext)) {
                        same = true;
                        break;
                    }
                }
                if (same) { // delete temp file
                    if (tmp) {
                        file.delete();
                    }
                    file = f;
                } else {
                    for (File e : ee) {
                        e.delete();
                    }
                    file = Storage.move(file, f);
                }
            }
        }

    }

    public static class RecentInfo {
        public String md5;
        public long created;
        public long last;
        public ZLTextPosition position;
        public String authors;
        public String title;

        public RecentInfo() {
        }

        public RecentInfo(File f) {
            try {
                FileInputStream is = new FileInputStream(f);
                String json = IOUtils.toString(is, Charset.defaultCharset());
                JSONObject j = new JSONObject(json);
                load(j);
                is.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public RecentInfo(JSONObject o) throws JSONException {
            load(o);
        }

        public void load(JSONObject o) throws JSONException {
            md5 = o.getString("md5");
            created = o.optLong("created", 0);
            last = o.getLong("last");
            authors = o.optString("authors", null);
            title = o.optString("title", null);
            JSONArray a = o.optJSONArray("position");
            if (a != null)
                position = new ZLTextFixedPosition(a.getInt(0), a.getInt(1), a.getInt(2));
        }

        public JSONObject save() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("md5", md5);
            o.put("created", created);
            o.put("last", last);
            o.put("authors", authors);
            o.put("title", title);
            if (position != null) {
                JSONArray a = new JSONArray();
                a.put(position.getParagraphIndex());
                a.put(position.getElementIndex());
                a.put(position.getCharIndex());
                o.put("position", a);
            }
            return o;
        }
    }

    public Storage(Context context) {
        super(context);
    }

    public void save(Book book) {
        book.info.last = System.currentTimeMillis();
        File f = recentFile(book);
        try {
            String json = book.info.save().toString();
            Writer w = new FileWriter(f);
            IOUtils.write(json, w);
            w.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Book load(Uri uri) {
        Book fbook;
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
                AssetFileDescriptor.AutoCloseInputStream is = new AssetFileDescriptor.AutoCloseInputStream(fd);
                fbook = load(is, null);
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
                }
                fbook = load(is, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else { // file:// or /path/file
            File f = new File(uri.getPath());
            try {
                FileInputStream is = new FileInputStream(f);
                fbook = load(is, f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        File r = recentFile(fbook);
        if (r.exists()) {
            try {
                fbook.info = new RecentInfo(r);
            } catch (RuntimeException e) {
                Log.d(TAG, "Unable to load info", e);
            }
        }
        if (fbook.info == null) {
            fbook.info = new RecentInfo();
            fbook.info.created = System.currentTimeMillis();
        }
        load(fbook);
        if (fbook.info.title == null || fbook.info.title.isEmpty() || fbook.info.title.equals(fbook.md5)) {
            if (contentDisposition != null && !contentDisposition.isEmpty())
                fbook.info.title = contentDisposition;
            else
                fbook.info.title = Storage.getNameNoExt(uri.getLastPathSegment());
        }
        if (!r.exists())
            save(fbook);
        return fbook;
    }

    public File getCache() {
        File cache = context.getExternalCacheDir();
        if (cache == null || !canWrite(cache))
            cache = context.getCacheDir();
        return cache;
    }

    public Book load(InputStream is, File f) {
        boolean tmp = f == null;
        Book fbook = new Book();
        try {
            FileOutputStream os = null;

            fbook.file = f;
            if (fbook.file == null) {
                fbook.file = File.createTempFile("book", ".tmp", getCache());
                os = new FileOutputStream(fbook.file);
            }

            Detector[] dd = supported();

            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            FileTypeDetectorXml xml = new FileTypeDetectorXml(dd);
            FileTypeDetectorZip zip = new FileTypeDetectorZip(dd);
            FileTypeDetector bin = new FileTypeDetector(dd);

            byte[] buf = new byte[BUF_SIZE];
            int len;
            while ((len = is.read(buf)) > 0) {
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

            fbook.md5 = toHex(digest.digest());

            for (Detector d : dd) {
                if (d.detected) {
                    fbook.ext = d.ext;
                    if (d instanceof FileTypeDetectorZipExtract.Handler) {
                        FileTypeDetectorZipExtract.Handler e = (FileTypeDetectorZipExtract.Handler) d;
                        File z = fbook.file;
                        if (!tmp) {
                            fbook.file = File.createTempFile("book", ".tmp", getCache());
                            fbook.md5 = e.extract(z, fbook.file);
                            tmp = true; // force to delete 'fbook.file'
                        } else {
                            File tt = File.createTempFile("book", ".tmp", getCache());
                            fbook.md5 = e.extract(z, tt);
                            z.delete();
                            fbook.file = tt;
                        }
                    }
                    break; // priority first - more imporant
                }
            }

            if (fbook.ext == null) {
                throw new RuntimeException("Unsupported format");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException | NoSuchAlgorithmException e) {
            if (tmp && fbook.file != null)
                fbook.file.delete();
            throw new RuntimeException(e);
        }
        File storage = getLocalStorage();
        fbook.store(storage, tmp);
        return fbook;
    }

    public ZLImage loadCover(Book book) {
        try {
            final PluginCollection pluginCollection = PluginCollection.Instance(new Info(context));
            FormatPlugin plugin = getPlugin(pluginCollection, book);
            ZLFile file = BookUtil.fileByBook(book.book);
            return plugin.readCover(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void load(Book fbook) {
        File r = recentFile(fbook);
        if (fbook.info == null) {
            if (r.exists())
                try {
                    fbook.info = new RecentInfo(r);
                } catch (RuntimeException e) {
                    Log.d(TAG, "Unable to load info", e);
                }
        }
        if (fbook.info == null) {
            fbook.info = new RecentInfo();
            fbook.info.created = System.currentTimeMillis();
        }
        fbook.info.md5 = fbook.md5;
        final PluginCollection pluginCollection = PluginCollection.Instance(new Info(context));
        fbook.book = new org.geometerplus.fbreader.book.Book(-1, fbook.file.getPath(), null, null, null);
        FormatPlugin plugin = getPlugin(pluginCollection, fbook);
        try {
            plugin.readMetainfo(fbook.book);
        } catch (BookReadingException e) {
            throw new RuntimeException(e);
        }
        if (fbook.info.authors == null || fbook.info.authors.isEmpty()) {
            fbook.info.authors = fbook.book.authorsString(", ");
        }
        if (fbook.info.title == null || fbook.info.title.isEmpty() || fbook.info.title.equals(fbook.md5)) {
            fbook.info.title = getTitle(fbook);
        }
        File cover = coverFile(fbook);
        if (!cover.exists() || cover.length() == 0) {
            ZLImage image = loadCover(fbook);
            if (image != null) {
                fbook.cover = cover;
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
                    bm.recycle();
                    FileOutputStream os = new FileOutputStream(fbook.cover);
                    sbm.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.close();
                    sbm.recycle();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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

    public ArrayList<Book> list() {
        ArrayList<Book> list = new ArrayList<>();
        list(list, getLocalInternal());
        list(list, getLocalExternal());
        return list;
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
                if (e.equals(COVER_EXT))
                    return false;
                if (e.equals(JSON_EXT))
                    return false;
                return n.length() == MD5_SIZE;
            }
        });
        if (ff == null)
            return;
        for (File f : ff) {
            Book b = new Book();
            b.md5 = getNameNoExt(f);
            b.file = f;
            File cover = coverFile(b);
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

    public void delete(Book book) {
        book.file.delete();
        if (book.cover != null)
            book.cover.delete();
        File r = recentFile(book);
        r.delete();
    }

}
