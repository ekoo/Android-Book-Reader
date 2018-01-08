package com.github.axet.bookreader.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;

import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.zlibrary.core.util.SystemInfo;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Storage extends com.github.axet.androidlibrary.app.Storage {

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
            ByteArrayOutputStream os;

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
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
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
            for (Handler h : new ArrayList<Handler>(list)) {
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
                os.close();
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
        public Context context;
        public File file;
        public String md5;
        public Book book;
        public String ext;
        public Storage.RecentInfo info;

        public StoredBook(Context context, InputStream is) {
            this.context = context;
            try {
                file = File.createTempFile("book", ".tmp");

                FileOutputStream os = new FileOutputStream(file);
                MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
                FileTypeDetectorXml xml = new FileTypeDetectorXml();
                FileTypeDetectorZip zip = new FileTypeDetectorZip();
                FileTypeDetector bin = new FileTypeDetector();

                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) > 0) {
                    digest.update(buf, 0, len);
                    os.write(buf, 0, len);
                    xml.write(buf, 0, len);
                    zip.write(buf, 0, len);
                    bin.write(buf, 0, len);
                }

                os.close();
                bin.close();
                zip.close();
                xml.close();

                for (Detector d : DETECTORS) {
                    if (d.detected) {
                        ext = d.ext;
                        break; // priority first - more imporant
                    }
                }

                byte messageDigest[] = digest.digest();

                md5 = toHex(messageDigest);
            } catch (Exception e) {
                if (file != null)
                    file.delete();
                throw new RuntimeException(e);
            }
        }

        public File exists(File s) {
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
            return ff[0];
        }

        public void store(File s) {
            File f = new File(s, md5 + "." + ext);
            File e = exists(s);
            if (e == null) {
                file = com.github.axet.androidlibrary.app.Storage.move(file, f);
            } else {
                String ee = Storage.getExt(e);
                if (!ee.equals(ext)) { // different ext same md5?
                    e.delete();
                    file = com.github.axet.androidlibrary.app.Storage.move(file, f);
                } else {
                    file.delete();
                    file = f;
                }
            }
        }

        public void load(SystemInfo info) {
            try {
                final PluginCollection pluginCollection = PluginCollection.Instance(info);
                book = new Book(-1, file.getPath(), null, null, null);
                BookUtil.reloadInfoFromFile(book, pluginCollection);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class RecentInfo {
        public String md5;
        public long last;
        public ZLTextPosition position;

        public RecentInfo() {
        }

        public RecentInfo(JSONObject o) throws JSONException {
            load(o);
        }

        public void load(JSONObject o) throws JSONException {
            md5 = o.getString("md5");
            last = o.getLong("last");
            JSONArray a = o.getJSONArray("position");
            position = new ZLTextFixedPosition(a.getInt(0), a.getInt(1), a.getInt(2));
        }

        public JSONObject save() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("md5", md5);
            o.put("last", last);
            JSONArray a = new JSONArray();
            a.put(position.getParagraphIndex());
            a.put(position.getElementIndex());
            a.put(position.getCharIndex());
            o.put("position", a);
            return o;
        }
    }

    public static class Recents extends HashMap<String, RecentInfo> {
        public Context context;

        public Recents(Context context) {
            this.context = context;
            load();
        }

        public void load() {
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
            String json = shared.getString(MainApplication.PREFERENCE_RECENTS, null);
            if (json == null || json.isEmpty())
                return;
            try {
                JSONArray j = new JSONArray(json);
                for (int i = 0; i < j.length(); i++) {
                    JSONObject o = (JSONObject) j.get(i);
                    RecentInfo info = new RecentInfo(o);
                    put(info.md5, info);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        public void save() {
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
            shared.getString(MainApplication.PREFERENCE_RECENTS, "");
            SharedPreferences.Editor editor = shared.edit();
            try {
                JSONArray o = new JSONArray();
                for (String key : keySet()) {
                    RecentInfo info = get(key);
                    o.put(info.save());
                }
                editor.putString(MainApplication.PREFERENCE_RECENTS, o.toString());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            editor.commit();
        }
    }

    public Storage(Context context) {
        super(context);
    }

}
