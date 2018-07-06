package com.github.axet.bookreader.services;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.axet.androidlibrary.services.StorageProvider;

import org.geometerplus.zlibrary.core.image.ZLFileImage;
import org.geometerplus.zlibrary.core.image.ZLImageData;
import org.geometerplus.zlibrary.core.image.ZLImageManager;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;

import java.io.FileNotFoundException;

public class ImagesProvider extends StorageProvider {
    public static String TAG = ImagesProvider.class.getSimpleName();

    public static final String EXT = "png";

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Uri f = find(uri);
        if (f == null)
            return null;

        freeUris();

        final String url = f.toString();
        final String prefix = ZLFileImage.SCHEME + "://";

        final ZLFileImage image = ZLFileImage.byUrlPath(url.substring(prefix.length()));
        if (image == null)
            throw new FileNotFoundException();

        try {
            ParcelFileDescriptor pp[] = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor r = pp[0];
            final ParcelFileDescriptor w = pp[1];
            final ZLImageData imageData = ZLImageManager.Instance().getImageData(image);
            final Bitmap bm = ((ZLAndroidImageData) imageData).getFullSizeBitmap();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ParcelFileDescriptor.AutoCloseOutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(w);
                        bm.compress(Bitmap.CompressFormat.PNG, 100, out);
                        bm.recycle();
                        out.close();
                    } catch (Throwable e) {
                        Log.d(TAG, "extract file broken", e);
                    }
                }
            }, "Extract Image Book");
            thread.start();
            return r;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
