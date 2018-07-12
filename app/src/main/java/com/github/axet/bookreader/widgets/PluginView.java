package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.github.axet.bookreader.app.Storage;

import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextView;

import java.io.IOException;
import java.util.Map;

public class PluginView {
    public Bitmap wallpaper;
    public int wallpaperColor;
    public Paint paint = new Paint();
    public PluginPage current;
    public boolean reflow = false;
    public boolean reflowDebug;
    public Reflow reflower;
    public Selection selection;

    public static class Selection { // plugin coords (render bm size's)
        public interface Setter {
            void setStart(int x, int y);

            void setEnd(int x, int y);

            Bounds getBounds();
        }

        public static class Bounds {
            public Rect[] rr;
            public boolean reverse;
            public boolean start;
            public boolean end;

            public Bounds() {
            }

            public Bounds(Rect[] r, boolean b) {
                rr = r;
                reverse = b;
            }
        }

        public static class Page {
            public int page;
            public int w;
            public int h;

            public Page(int p, int w, int h) {
                this.page = p;
                this.w = w;
                this.h = h;
            }
        }

        public static class Point {
            public int x;
            public int y;

            public Point(int x, int y) {
                this.x = x;
                this.y = y;
            }

            public Point(android.graphics.Point p) {
                this.x = p.x;
                this.y = p.y;
            }
        }

        public boolean isWord(Character c) {
            if (Character.isSpaceChar(c))
                return false;
            return Character.isDigit(c) || Character.isLetter(c) || Character.isLetterOrDigit(c) || c == '[' || c == ']' || c == '(' || c == ')';
        }

        public void setStart(Page page, Point point) {
        }

        public int getStart() {
            return -1;
        }

        public void setEnd(Page page, Point point) {
        }

        public int getEnd() {
            return -1;
        }

        public String getText() {
            return null;
        }

        public Bounds getBounds(Page page) {
            return null;
        }

        public void close() {
        }
    }

    public PluginView() {
        try {
            org.geometerplus.fbreader.fbreader.FBReaderApp app = new org.geometerplus.fbreader.fbreader.FBReaderApp(null, new BookCollectionShadow());
            ZLFile wallpaper = app.BookTextView.getWallpaperFile();
            if (wallpaper != null)
                this.wallpaper = BitmapFactory.decodeStream(wallpaper.getInputStream());
            wallpaperColor = (0xff << 24) | app.BookTextView.getBackgroundColor().intValue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void drawWallpaper(Canvas canvas) {
        if (wallpaper != null) {
            float dx = wallpaper.getWidth();
            float dy = wallpaper.getHeight();
            for (int cw = 0; cw < canvas.getWidth() + dx; cw += dx) {
                for (int ch = 0; ch < canvas.getHeight() + dy; ch += dy) {
                    canvas.drawBitmap(wallpaper, cw - dx, ch - dy, paint);
                }
            }
        } else {
            canvas.drawColor(wallpaperColor);
        }
    }

    public void gotoPosition(ZLTextPosition p) {
        if (p == null)
            return;
        if (current.pageNumber != p.getParagraphIndex() || current.pageOffset != p.getElementIndex())
            current.load(p);
        if (reflower != null) {
            if (reflower.page != p.getParagraphIndex()) {
                reflower.reset();
                reflower.page = current.pageNumber;
            }
            reflower.current = p.getElementIndex();
        }
    }

    public boolean onScrollingFinished(ZLViewEnums.PageIndex index) {
        if (reflow && reflowDebug) {
            switch (index) {
                case previous:
                    current.pageNumber--;
                    current.pageOffset = 0;
                    current.load();
                    break;
                case next:
                    current.pageNumber++;
                    current.pageOffset = 0;
                    current.load();
                    break;
            }
            return false;
        }
        if (reflower != null) {
            reflower.onScrollingFinished(index);
            if (reflower.page != current.pageNumber) {
                current.pageNumber = reflower.page;
                current.pageOffset = 0;
                current.load();
            }
            if (reflower.current == -1) {
                current.pageNumber = reflower.page - 1;
                current.pageOffset = 0;
                current.load();
            }
            if (reflower.current >= reflower.emptyCount()) { // current points to next page +1
                current.pageNumber = reflower.page + 1;
                current.pageOffset = 0;
                current.load();
            }
            return false;
        }
        PluginPage old = new PluginPage(current) {
            @Override
            public void load() {
            }

            @Override
            public int getPagesCount() {
                return current.getPagesCount();
            }
        };
        current.load(index);
        PluginPage r;
        switch (index) {
            case previous:
                r = new PluginPage(current, ZLViewEnums.PageIndex.next) {
                    @Override
                    public void load() {
                    }

                    @Override
                    public int getPagesCount() {
                        return current.getPagesCount();
                    }
                };
                break;
            case next:
                r = new PluginPage(current, ZLViewEnums.PageIndex.previous) {
                    @Override
                    public void load() {
                    }

                    @Override
                    public int getPagesCount() {
                        return current.getPagesCount();
                    }
                };
                break;
            default:
                return false;
        }
        return !old.equals(r.pageNumber, r.pageOffset); // need reset cache true/false?
    }

    public ZLTextFixedPosition getPosition() {
        return new ZLTextFixedPosition(current.pageNumber, current.pageOffset, 0);
    }

    public ZLTextFixedPosition getNextPosition() {
        if (current.w == 0 || current.h == 0)
            return null; // after reset() we do not know display size
        PluginPage next = new PluginPage(current, ZLViewEnums.PageIndex.next) {
            @Override
            public void load() {
            }

            @Override
            public int getPagesCount() {
                return current.getPagesCount();
            }
        };
        if (current.equals(next.pageNumber, next.pageOffset))
            return null; // !canScroll()
        ZLTextFixedPosition e = new ZLTextFixedPosition(next.pageNumber, next.pageOffset, 0);
        if (e.ParagraphIndex >= next.getPagesCount())
            return null;
        return e;
    }

    public boolean canScroll(ZLView.PageIndex index) {
        if (reflower != null) {
            if (reflower.canScroll(index))
                return true;
            switch (index) {
                case previous:
                    if (current.pageNumber > 0)
                        return true;
                    if (current.pageNumber != reflower.page) { // only happens to 0 page of document, we need to know it reflow count
                        int render = reflower.current;
                        Bitmap bm = render(reflower.rw, reflower.h, current.pageNumber); // 0 page
                        reflower.load(bm, current.pageNumber, 0);
                        bm.recycle();
                        int count = reflower.count();
                        count += render;
                        reflower.current = count;
                        return count > 0;
                    }
                    return false;
                case next:
                    if (current.pageNumber + 1 < current.getPagesCount())
                        return true;
                    if (current.pageNumber != reflower.page) { // only happens to last page of document, we need to know it reflow count
                        int render = reflower.current - reflower.count();
                        Bitmap bm = render(reflower.rw, reflower.h, current.pageNumber); // last page
                        reflower.load(bm, current.pageNumber, 0);
                        bm.recycle();
                        reflower.current = render;
                        return render + 1 < reflower.count();
                    }
                    return false;
                default:
                    return true; // current???
            }
        }
        PluginPage r = new PluginPage(current, index) {
            @Override
            public void load() {
            }

            @Override
            public int getPagesCount() {
                return current.getPagesCount();
            }
        };
        return !r.equals(current.pageNumber, current.pageOffset);
    }

    public ZLTextView.PagePosition pagePosition() {
        return new ZLTextView.PagePosition(current.pageNumber + 1, current.getPagesCount());
    }

    public Bitmap render(int w, int h, int page, Bitmap.Config c) {
        return null;
    }

    public Bitmap render(int w, int h, int page) {
        return render(w, h, page, Bitmap.Config.RGB_565); // reflower active, always 565
    }

    public void drawOnBitmap(Context context, Bitmap bitmap, int w, int h, ZLView.PageIndex index, FBReaderView.CustomView custom, Storage.RecentInfo info) {
        Canvas canvas = new Canvas(bitmap);
        drawOnCanvas(context, canvas, w, h, index, custom, info);
    }

    public double getPageHeight(int w, FBReaderView.ScrollView.ScrollAdapter.PageCursor c) {
        return -1;
    }

    public void drawOnCanvas(Context context, Canvas canvas, int w, int h, ZLView.PageIndex index, FBReaderView.CustomView custom, Storage.RecentInfo info) {
        if (reflow) {
            if (reflower == null) {
                int page = current.pageNumber;
                reflower = new Reflow(context, w, h, page, custom, info);
            }
            Bitmap bm = null;
            reflower.reset(w, h);
            int render = reflower.current; // render reflow page index
            int page = reflower.page; // render pageNumber
            if (reflowDebug) {
                switch (index) {
                    case previous:
                        page = current.pageNumber - 1;
                        break;
                    case next:
                        page = current.pageNumber + 1;
                        break;
                    case current:
                        break;
                }
                index = ZLViewEnums.PageIndex.current;
                render = 0;
            }
            switch (index) {
                case previous: // prev can point to many (no more then 2) pages behind, we need to walk every page manually
                    render -= 1;
                    while (render < 0) {
                        page--;
                        if (bm != null)
                            bm.recycle();
                        bm = render(reflower.rw, reflower.h, page);
                        reflower.load(bm);
                        render = render + reflower.emptyCount();
                        reflower.page = page;
                        reflower.current = render + 1; // onScrollingFinished - 1
                    }
                    if (reflower.count() > render) {
                        if (bm != null)
                            bm.recycle();
                        bm = reflower.render(render);
                    }
                    break;
                case current:
                    bm = render(reflower.rw, reflower.h, page);
                    if (reflowDebug) {
                        reflower.k2.setVerbose(true);
                        reflower.k2.setShowMarkedSource(true);
                    }
                    reflower.load(bm, page, render);
                    if (reflowDebug) {
                        reflower.bm = null; // do not recycle
                        reflower.close();
                        reflower = null;
                    } else {
                        if (reflower.count() > render) { // empty source page
                            bm.recycle();
                            bm = reflower.render(render);
                        }
                    }
                    break;
                case next: // next can point to many (no more then 2) pages ahead, we need to walk every page manually
                    render += 1;
                    while (reflower.emptyCount() - render <= 0) {
                        page++;
                        render -= reflower.emptyCount();
                        if (bm != null)
                            bm.recycle();
                        bm = render(reflower.rw, reflower.h, page);
                        reflower.load(bm, page, render - 1); // onScrollingFinished + 1
                    }
                    if (reflower.count() > render) {
                        if (bm != null)
                            bm.recycle();
                        bm = reflower.render(render);
                    }
                    break;
            }
            if (bm != null) {
                if (reflower == null || reflower.bm == bm)
                    drawWallpaper(canvas); // we are about to draw original page, perapre bacgkournd
                else
                    canvas.drawColor(Color.WHITE); // prepare white
                drawPage(canvas, w, h, bm);
                bm.recycle();
                return;
            }
        }
        if (reflower != null) {
            reflower.close();
            reflower = null;
        }
        drawWallpaper(canvas);
        draw(canvas, w, h, index);
    }

    public void draw(Canvas bitmap, int w, int h, ZLView.PageIndex index, Bitmap.Config c) {
    }

    public void draw(Canvas bitmap, int w, int h, ZLView.PageIndex index) {
        try {
            draw(bitmap, w, h, index, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            draw(bitmap, w, h, index, Bitmap.Config.RGB_565);
        }
    }

    public void drawPage(Canvas canvas, int w, int h, Bitmap bm) {
        Rect src = new Rect(0, 0, bm.getWidth(), bm.getHeight());
        float wr = w / (float) bm.getWidth();
        float hr = h / (float) bm.getHeight();
        int dh = (int) (bm.getHeight() * wr);
        int dw = (int) (bm.getWidth() * hr);
        Rect dst;
        if (dh > h) { // scaling width max makes it too high
            int mid = (w - dw) / 2;
            dst = new Rect(mid, 0, dw + mid, h); // scale it by height max and take calulated width
        } else { // take width
            int mid = (h - dh) / 2;
            dst = new Rect(0, mid, w, dh + mid); // scale it by width max and take calulated height
        }
        canvas.drawBitmap(bm, src, dst, paint);
    }

    public void close() {
    }

    public TOCTree getCurrentTOCElement(TOCTree TOCTree) {
        TOCTree treeToSelect = null;
        for (TOCTree tree : TOCTree) {
            final TOCTree.Reference reference = tree.getReference();
            if (reference == null) {
                continue;
            }
            if (reference.ParagraphIndex > current.pageNumber) {
                break;
            }
            treeToSelect = tree;
        }
        return treeToSelect;
    }

    public Selection select(Selection.Page p, Selection.Point point) {
        return null;
    }

    Selection.Page selectPage(ZLTextPosition start, Reflow.Info info, int w, int h) {
        if (reflow)
            return new PluginView.Selection.Page(start.getParagraphIndex(), info.bm.width(), info.bm.height());
        else
            return new PluginView.Selection.Page(start.getParagraphIndex(), w, h);
    }

    Selection.Point selectPoint(ZLTextPosition start, Reflow.Info info, int x, int y) {
        if (reflow) {
            x = x - info.margin.left;
            Map<Rect, Rect> dst = info.dst.get(start.getElementIndex());
            for (Rect k : dst.keySet()) {
                if (k.contains(x, y)) {
                    Rect v = dst.get(k);

                    double kx = v.width() / (double) k.width();
                    double ky = v.height() / (double) k.height();

                    return new Selection.Point(v.left + (int) ((x - k.left) * kx), v.top + (int) ((y - k.top) * ky));
                }
            }
            return null;
        } else {
            return new Selection.Point(x, y);
        }
    }

    public Selection select(ZLTextPosition start, Reflow.Info info, int w, int h, int x, int y) {
        selection = select(selectPage(start, info, w, h), selectPoint(start, info, x, y));
        return selection;
    }

}
