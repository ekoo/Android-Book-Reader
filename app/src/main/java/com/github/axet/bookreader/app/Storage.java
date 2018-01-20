package com.github.axet.bookreader.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v7.preference.PreferenceManager;

import com.github.axet.androidlibrary.app.Native;
import com.github.axet.androidlibrary.net.HttpClient;
import com.github.axet.androidlibrary.widgets.WebViewCustom;
import com.shockwave.pdfium.PdfiumCore;

import org.apache.commons.io.IOUtils;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.fbreader.network.auth.AndroidNetworkContext;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.formats.BookReadingException;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.network.NetworkLibrary;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLFileImageProxy;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.image.ZLStreamImage;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
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
import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Storage extends com.github.axet.androidlibrary.app.Storage {

    public static final int MD5_SIZE = 32;
    public static final String COVER_EXT = "png";
    public static final String JSON_EXT = "json";

    public static ZLAndroidApplication zlib;

    public static Detector[] supported() {
        return new Detector[]{new FileFB2(), new FileEPUB(), new FileHTML(),
                new FilePDF(), new FileDjvu(), new FileRTF(), new FileDoc(),
                new FileMobi(), new FileTxt()};
    }

    // disable broken, closed, or authorization only repos without free books / or open links
    public static List<String> disabledIds = Arrays.asList(
            "http://data.fbreader.org/catalogs/litres2/index.php5", // authorization
            "http://www.freebookshub.com/feed/", // fake links
            "http://ebooks.qumran.org/opds/?lang=en", // timeout
            "http://ebooks.qumran.org/opds/?lang=de", // timeout
            "http://www.epubbud.com/feeds/catalog.atom", // ePub Bud has decided to wind down
            "http://www.shucang.org/s/index.php" // timeout
    );

    public static List<String> libAllIds(NetworkLibrary nlib) {
        List<String> all = nlib.allIds();
        for (String id : disabledIds) {
            nlib.setLinkActive(id, false);
            all.remove(id);
        }
        return all;
    }

    public static NetworkLibrary getLib(final Context context) {
        AndroidNetworkContext nc = new AndroidNetworkContext() {
            @Override
            protected Context getContext() {
                return context;
            }

            @Override
            protected Map<String, String> authenticateWeb(URI uri, String realm, String authUrl, String completeUrl, String verificationUrl) {
                return null;
            }
        };
        NetworkLibrary nlib = NetworkLibrary.Instance(new Storage.Info(context));
        if (!nlib.isInitialized()) {
            try {
                nlib.initialize(nc);
            } catch (ZLNetworkException e) {
            }
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
            String json = shared.getString(MainApplication.PREFERENCE_CATALOGS, null);
            if (json != null && !json.isEmpty()) {
                try {
                    List<String> all = nlib.allIds();
                    for (String id : all)
                        nlib.setLinkActive(id, false);
                    JSONArray a = new JSONArray(json);
                    for (int i = 0; i < a.length(); i++) {
                        String id = a.getString(i);
                        if (!disabledIds.contains(id))
                            nlib.setLinkActive(id, true);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return nlib;
    }

    public static FBReaderApp getApp(final Context context) {
        if (zlib == null) {
            zlib = new ZLAndroidApplication() {
                {
                    attachBaseContext(context);
                    onCreate();
                }
            };
        }
        if (PDFPlugin.core == null) {
            Native.loadLibraries(context, new String[]{"modpng", "modft2", "modpdfium", "jniPdfium"});
            PDFPlugin.core = new PdfiumCore(context);
        }
        Info info = new Info(context);
        FBReaderApp app = (FBReaderApp) FBReaderApp.Instance();
        if (app == null) {
            app = new FBReaderApp(info, new BookCollectionShadow());
        }
        return app;
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

    public static String toHex(byte[] messageDigest) {
        StringBuilder hexString = new StringBuilder();
        for (byte aMessageDigest : messageDigest) {
            String h = Integer.toHexString(0xFF & aMessageDigest);
            while (h.length() < 2)
                h = "0" + h;
            hexString.append(h);
        }
        return hexString.toString();
    }

    public static String getTitle(Book book) {
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
            }

            public Handler(String ext, String str) {
                super(ext);
                first = str.getBytes(Charset.defaultCharset());
            }

            public Handler(String ext, int[] b) {
                super(ext);
                first = new byte[b.length];
                for (int i = 0; i < b.length; i++)
                    first[i] = (byte) b[i];
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

    public static class FileTypeDetectorZip {
        ArrayList<Handler> list = new ArrayList<>();
        ParcelFileDescriptor.AutoCloseInputStream is;
        ParcelFileDescriptor.AutoCloseOutputStream os;
        ZipInputStream zip;
        Thread thread;

        public static class Handler extends Detector {
            public Handler(String ext) {
                super(ext);
            }

            void nextEntry(ZipEntry entry) {
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
                    try {
                        zip = new ZipInputStream(is);
                        ZipEntry entry;
                        while ((entry = zip.getNextEntry()) != null) {
                            for (Handler h : new ArrayList<>(list)) {
                                h.nextEntry(entry);
                                if (h.done)
                                    list.remove(h);
                            }
                        }
                    } catch (Exception e) {
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

        public FileTxt() {
            super("txt");
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            if (os.size() >= 1000) {
                done = true;
                byte[] bb = os.toByteArray();
                for (int i = 0; i < bb.length; i++) {
                    int b = bb[i] & 0xFF;
                    for (int k = 0; k < text_chars.length; k++) {
                        if (text_chars[b] == F)
                            return;
                    }
                }
                detected = true;
            }
        }
    }

    public static class Book {
        public File file;
        public String md5;
        public org.geometerplus.fbreader.book.Book book;
        public String ext;
        public Storage.RecentInfo info;
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
                    if (getExt(e).equals(ext)) {
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
        getApp(context);
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
        String s = uri.getScheme();
        if (s.equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver resolver = context.getContentResolver();
            try {
                AssetFileDescriptor fd = resolver.openAssetFileDescriptor(uri, "r");
                AssetFileDescriptor.AutoCloseInputStream is = new AssetFileDescriptor.AutoCloseInputStream(fd);
                fbook = load(is, null);
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (s.startsWith(WebViewCustom.SCHEME_HTTP)) {
            try {
                HttpClient client = new HttpClient();
                HttpClient.DownloadResponse w = client.getResponse(null, uri.toString());
                if (w.getError() != null)
                    throw new RuntimeException(w.getError() + ": " + uri);
                InputStream is = new BufferedInputStream(w.getInputStream());
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
            fbook.info = new RecentInfo(r);
        }
        if (fbook.info == null) {
            fbook.info = new RecentInfo();
            fbook.info.created = System.currentTimeMillis();
        }
        load(fbook);
        if (fbook.info.title == null || fbook.info.title.isEmpty() || fbook.info.title.equals(fbook.md5)) {
            fbook.info.title = Storage.getNameNoExt(uri.getLastPathSegment());
        }
        if (!r.exists())
            save(fbook);
        return fbook;
    }

    public Book load(InputStream is, File f) {
        Book fbook = new Book();
        try {
            FileOutputStream os = null;

            fbook.file = f;
            if (fbook.file == null) {
                fbook.file = File.createTempFile("book", ".tmp");
                os = new FileOutputStream(fbook.file);
            }

            Detector[] dd = supported();

            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            FileTypeDetectorXml xml = new FileTypeDetectorXml(dd);
            FileTypeDetectorZip zip = new FileTypeDetectorZip(dd);
            FileTypeDetector bin = new FileTypeDetector(dd);

            byte[] buf = new byte[1024];
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

            for (Detector d : dd) {
                if (d.detected) {
                    fbook.ext = d.ext;
                    break; // priority first - more imporant
                }
            }

            if (fbook.ext == null) {
                throw new RuntimeException("Unsupported format");
            }

            byte messageDigest[] = digest.digest();

            fbook.md5 = toHex(messageDigest);
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException | NoSuchAlgorithmException e) {
            if (fbook.file != null)
                fbook.file.delete();
            throw new RuntimeException(e);
        }
        File storage = getLocalStorage();
        fbook.store(storage, f == null);
        return fbook;
    }

    public ZLImage loadCover(Book book) {
        try {
            final PluginCollection pluginCollection = PluginCollection.Instance(new Info(context));
            FormatPlugin plugin = getPlugin(pluginCollection, book);
            ZLFile file = BookUtil.fileByBook(book.book);
            ZLImage c = plugin.readCover(file);
            return c;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void load(Book fbook) {
        File r = recentFile(fbook);
        if (fbook.info == null) {
            if (r.exists())
                fbook.info = new RecentInfo(r);
        }
        if (fbook.info == null)
            fbook.info = new Storage.RecentInfo();
        fbook.info.md5 = fbook.md5;
        try {
            final PluginCollection pluginCollection = PluginCollection.Instance(new Info(context));
            fbook.book = new org.geometerplus.fbreader.book.Book(-1, fbook.file.getPath(), null, null, null);
            FormatPlugin plugin = getPlugin(pluginCollection, fbook);
            plugin.readMetainfo(fbook.book);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (fbook.info.title == null || fbook.info.title.isEmpty() || fbook.info.title.equals(fbook.md5)) {
            String title = getTitle(fbook);
            if (title != null && !title.isEmpty())
                fbook.info.title = title;
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
                if (bm == null)
                    return;
                try {
                    float ratio = 128f / bm.getWidth();
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

    public ArrayList<Book> list() {
        ArrayList<Book> list = new ArrayList<>();
        list(list, getLocalInternal());
        list(list, getLocalExternal());
        return list;
    }

    public void list(ArrayList<Book> list, File storage) {
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
            if (r.exists())
                b.info = new RecentInfo(r);
            if (b.info == null) {
                b.info = new Storage.RecentInfo();
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
