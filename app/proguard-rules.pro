-dontobfuscate
-dontoptimize
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5
-printusage

-keep class com.github.axet.djvulibre.DjvuLibre {*;}
-keep class com.github.axet.djvulibre.DjvuLibre$Page {*;}
-keep class com.github.axet.djvulibre.DjvuLibre$Bookmark {*;}

-keep class com.shockwave.pdfium.PdfiumCore {*;}
-keep class com.shockwave.pdfium.util.Size {*;}

-keep class com.github.axet.k2pdfopt.K2PdfOpt {*;}

-keep class com.github.axet.androidlibrary.widgets.SearchView {*;}
-keep class com.github.axet.bookreader.widgets.Toolbar* {*;}

-dontwarn org.geometerplus.**
-dontwarn org.apache.**
-dontwarn yuku.ambilwarna.**
-dontwarn org.spongycastle.**

# androidFilesChoicer

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# fbreader

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep class * extends java.lang.Exception

-keep class org.geometerplus.zlibrary.text.model.ZLTextPlainModel$EntryIteratorImpl { *; }
-keep class org.geometerplus.zlibrary.text.view.ZLTextParagraphCursor$Processor { *; }

-keep class org.geometerplus.zlibrary.core.library.ZLibrary
-keepclassmembers class org.geometerplus.zlibrary.core.library.ZLibrary {
    public static ** Instance();
    public ** getVersionName();
}
-keep class org.geometerplus.zlibrary.core.filesystem.ZLFile
-keepclassmembers class org.geometerplus.zlibrary.core.filesystem.ZLFile {
    public long lastModified();
    public static ** createFileByPath(...);
    public ** children();
    public boolean exists();
    public boolean isDirectory();
    public ** getInputStream();
    public ** getPath();
    public long size();
}
-keep class org.geometerplus.zlibrary.core.fonts.FileInfo
-keepclassmembers class org.geometerplus.zlibrary.core.fonts.FileInfo {
		public <init>(...);
}
-keep class org.geometerplus.zlibrary.core.drm.FileEncryptionInfo
-keepclassmembers class org.geometerplus.zlibrary.core.drm.FileEncryptionInfo {
		public <init>(...);
}
-keep class org.geometerplus.zlibrary.core.image.ZLImage
-keep class org.geometerplus.zlibrary.core.image.ZLFileImage
-keepclassmembers class org.geometerplus.zlibrary.core.image.ZLFileImage {
		public <init>(...);
}
-keep class org.geometerplus.zlibrary.text.model.ZLTextModel
-keep class org.geometerplus.fbreader.formats.PluginCollection
-keepclassmembers class org.geometerplus.fbreader.formats.PluginCollection {
    public static ** Instance();
}
-keepclassmembers class org.geometerplus.fbreader.formats.FormatPlugin {
    public ** supportedFileType();
}
-keep class org.geometerplus.fbreader.formats.NativeFormatPlugin
-keepclassmembers class org.geometerplus.fbreader.formats.NativeFormatPlugin {
    public static ** create(...);
}
-keep class org.geometerplus.zlibrary.core.encodings.Encoding
-keepclassmembers class org.geometerplus.zlibrary.core.encodings.Encoding {
		public ** createConverter();
}
-keep class org.geometerplus.zlibrary.core.encodings.EncodingConverter
-keepclassmembers class org.geometerplus.zlibrary.core.encodings.EncodingConverter {
    public ** Name;
		public int convert(byte[],int,int,char[]);
		public void reset();
}
-keep class org.geometerplus.zlibrary.core.encodings.JavaEncodingCollection
-keepclassmembers class org.geometerplus.zlibrary.core.encodings.JavaEncodingCollection {
    public static ** Instance();
    public ** getEncoding(java.lang.String);
    public ** getEncoding(int);
		public boolean providesConverterFor(java.lang.String);
}
-keep class org.geometerplus.fbreader.Paths
-keepclassmembers class org.geometerplus.fbreader.Paths {
    public static ** cacheDirectory();
}
-keep class org.geometerplus.fbreader.book.Book
-keepclassmembers class org.geometerplus.fbreader.book.Book {
    public ** File;
    public ** getTitle();
    public ** getLanguage();
    public ** getEncodingNoDetection();
    public void setTitle(...);
    public void setSeriesInfo(...);
    public void setLanguage(...);
    public void setEncoding(...);
    public void addAuthor(...);
    public void addTag(...);
    public void addUid(...);
}
-keep class org.geometerplus.fbreader.book.Tag
-keepclassmembers class org.geometerplus.fbreader.book.Tag {
    public static ** getTag(...);
}
-keepclassmembers class org.geometerplus.fbreader.bookmodel.BookModelImpl {
		public void addImage(...);
}
-keep class org.geometerplus.fbreader.bookmodel.NativeBookModel
-keepclassmembers class org.geometerplus.fbreader.bookmodel.NativeBookModel {
		public ** Book;
		public void initInternalHyperlinks(...);
		public void addTOCItem(...);
		public void leaveTOCItem();
		public ** createTextModel(...);
		public void setBookTextModel(...);
		public void setFootnoteModel(...);
}
-keep class org.geometerplus.fbreader.bookmodel.BookModel
-keepclassmembers class org.geometerplus.fbreader.bookmodel.BookModel {
		public void addTOCItem(...);
		public void leaveTOCItem();
		public ** createTextModel(...);
		public void setBookTextModel(...);
		public void setFootnoteModel(...);
		public void addImage(...);
		public void registerFontFamilyList(...);
		public void registerFontEntry(...);
		public void initInternalHyperlinks(...);
}
-keepclassmembers class org.geometerplus.fbreader.bookmodel.BookReadingException {
    public static void throwForFile(...);
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
