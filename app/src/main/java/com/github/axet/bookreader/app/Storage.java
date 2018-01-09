package com.github.axet.bookreader.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.github.axet.androidlibrary.widgets.WebViewCustom;
import com.github.axet.bookreader.widgets.FBReaderView;

import org.apache.commons.io.IOUtils;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.zlibrary.core.image.ZLFileImage;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.text.model.ZLImageEntry;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.model.ZLTextParagraph;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
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
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Storage extends com.github.axet.androidlibrary.app.Storage {

    public static final int MD5_SIZE = 32;
    public static final String COVER_EXT = "png";
    public static final String JSON_EXT = "json";

    public static Detector[] DETECTORS = new Detector[]{new FileFB2(), new FileEPUB(), new FileHTML(),
            new FilePDF(), new FileRTF(), new FileMobi(), new FileTxt()};

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

    public static String getTitle(StoredBook book) {
        String a = book.book.authorsString(", ");
        String t = book.book.getTitle();
        if (t.equals(book.md5))
            t = null;
        String m;
        if (a == null && t == null) {
            m = book.info.title;
            if (m == null)
                m = book.md5;
        } else if (a == null)
            m = t;
        else if (t == null)
            m = a;
        else
            m = a + " - " + t;
        return m;
    }

    public static File coverFile(StoredBook book) {
        File p = book.file.getParentFile();
        return new File(p, book.md5 + "." + COVER_EXT);
    }

    public static File recentFile(StoredBook book) {
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

            public Handler(String ext, byte[] b) {
                super(ext);
                first = b;
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

        public FileTypeDetector() {
            for (Detector d : DETECTORS) {
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

        public FileTypeDetectorZip() {
            for (Detector d : DETECTORS) {
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

        public FileTypeDetectorXml() {
            for (Detector d : DETECTORS) {
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
        byte[] b;

        public FileTxt() {
            super("txt");
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                os.write(new byte[]{7, 8, 9, 10, 12, 13, 27});
                for (int i = 0x20; i <= 0x100; i++) {
                    if (i == 0x7f)
                        continue;
                    os.write(i);
                }
                b = os.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            if (os.size() >= 1000) {
                done = true;
                byte[] bb = os.toByteArray();
                for (int i = 0; i < bb.length; i++) {
                    boolean v = false;
                    for (int k = 0; k < b.length; k++) {
                        if (bb[i] == b[k])
                            v = true;
                    }
                    if (!v)
                        return;
                }
                detected = true;
            }
        }
    }

    public static class StoredBook {
        public File file;
        public String md5;
        public Book book;
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
                file = com.github.axet.androidlibrary.app.Storage.move(file, f);
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
                    file = com.github.axet.androidlibrary.app.Storage.move(file, f);
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
            JSONArray a = o.getJSONArray("position");
            position = new ZLTextFixedPosition(a.getInt(0), a.getInt(1), a.getInt(2));
        }

        public JSONObject save() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("md5", md5);
            o.put("created", created);
            o.put("last", last);
            o.put("title", title);
            JSONArray a = new JSONArray();
            a.put(position.getParagraphIndex());
            a.put(position.getElementIndex());
            a.put(position.getCharIndex());
            o.put("position", a);
            return o;
        }
    }

    public Storage(Context context) {
        super(context);
    }

    public void save(StoredBook book) {
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

    public StoredBook load(Uri uri) {
        StoredBook fbook;
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
                URL url = new URL(uri.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream is = new BufferedInputStream(urlConnection.getInputStream());
                    fbook = load(is, null);
                } finally {
                    urlConnection.disconnect();
                }
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
        File f = recentFile(fbook);
        if (f.exists()) {
            fbook.info = new RecentInfo(f);
        }
        if (fbook.info == null) {
            fbook.info = new RecentInfo();
            fbook.info.created = System.currentTimeMillis();
        }
        fbook.info.title = Storage.getNameNoExt(uri.getLastPathSegment());
        load(fbook);
        return fbook;
    }

    public StoredBook load(InputStream is, File f) {
        StoredBook fbook = new StoredBook();
        try {
            FileOutputStream os = null;

            fbook.file = f;
            if (fbook.file == null) {
                fbook.file = File.createTempFile("book", ".tmp");
                os = new FileOutputStream(fbook.file);
            }

            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            FileTypeDetectorXml xml = new FileTypeDetectorXml();
            FileTypeDetectorZip zip = new FileTypeDetectorZip();
            FileTypeDetector bin = new FileTypeDetector();

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

            for (Detector d : DETECTORS) {
                if (d.detected) {
                    fbook.ext = d.ext;
                    break; // priority first - more imporant
                }
            }

            byte messageDigest[] = digest.digest();

            fbook.md5 = toHex(messageDigest);
        } catch (Exception e) {
            if (fbook.file != null)
                fbook.file.delete();
            throw new RuntimeException(e);
        }
        File storage = getLocalStorage();
        fbook.store(storage, f == null);
        return fbook;
    }

    public ZLFileImage loadCover(StoredBook book) {
        try {
            final PluginCollection pluginCollection = PluginCollection.Instance(new FBReaderView.Info(context));
            FormatPlugin plugin = BookUtil.getPlugin(pluginCollection, book.book);
            BookModel Model = BookModel.createModel(book.book, plugin);
            ZLTextModel text = Model.getTextModel();
            ZLImage first = null;
            for (int i = 0; i < text.getParagraphsNumber(); i++) {
                ZLTextParagraph p = text.getParagraph(i);
                ZLTextParagraph.EntryIterator ei = p.iterator();
                while (ei.next()) {
                    ZLImageEntry image = ei.getImageEntry();
                    if (image != null) {
                        if (first == null)
                            first = image.getImage();
                        if (image.IsCover) {
                            ZLImage img = image.getImage();
                            if (img instanceof ZLFileImage) {
                                ZLFileImage z = (ZLFileImage) img;
                                return z;
                            }
                        }
                    }
                }
                if (first != null) {
                    if (first instanceof ZLFileImage) {
                        ZLFileImage z = (ZLFileImage) first;
                        return z;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void load(StoredBook fbook) {
        if (fbook.info == null) {
            File r = recentFile(fbook);
            if (r.exists())
                fbook.info = new RecentInfo(r);
        }
        if (fbook.info == null)
            fbook.info = new Storage.RecentInfo();
        fbook.info.md5 = fbook.md5;
        try {
            FBReaderView.getApp(context);
            final PluginCollection pluginCollection = PluginCollection.Instance(new FBReaderView.Info(context));
            fbook.book = new Book(-1, fbook.file.getPath(), null, null, null);
            BookUtil.reloadInfoFromFile(fbook.book, pluginCollection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        fbook.info.title = getTitle(fbook);
        ZLFileImage image = loadCover(fbook);
        if (image != null) {
            fbook.cover = coverFile(fbook);
            Bitmap bm = BitmapFactory.decodeStream(image.inputStream());
            try {
                FileOutputStream os = new FileOutputStream(fbook.cover);
                bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ArrayList<StoredBook> list() {
        ArrayList<StoredBook> list = new ArrayList<>();
        list(list, getLocalInternal());
        list(list, getLocalExternal());
        return list;
    }

    public void list(ArrayList<StoredBook> list, File storage) {
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
            StoredBook b = new StoredBook();
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

    public void delete(StoredBook book) {
        book.file.delete();
        if (book.cover != null)
            book.cover.delete();
        File r = recentFile(book);
        r.delete();
    }
}