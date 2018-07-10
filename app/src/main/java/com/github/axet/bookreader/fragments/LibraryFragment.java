package com.github.axet.bookreader.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.axet.androidlibrary.services.FileProvider;
import com.github.axet.androidlibrary.services.StorageProvider;
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter;
import com.github.axet.androidlibrary.widgets.CacheImagesRecyclerAdapter;
import com.github.axet.androidlibrary.widgets.OpenFileDialog;
import com.github.axet.androidlibrary.widgets.SearchView;
import com.github.axet.androidlibrary.widgets.TextMax;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.activities.MainActivity;
import com.github.axet.bookreader.app.MainApplication;
import com.github.axet.bookreader.app.Storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class LibraryFragment extends Fragment implements MainActivity.SearchListener {
    public static final String TAG = LibraryFragment.class.getSimpleName();

    LibraryAdapter books;
    Storage storage;
    String lastSearch = "";
    FragmentHolder holder;
    Runnable invalidateOptionsMenu = new Runnable() {
        @Override
        public void run() {
            ActivityCompat.invalidateOptionsMenu(getActivity());
        }
    };


    public static class FragmentHolder {
        RecyclerView grid;

        public int layout;

        View toolbar;
        View searchpanel;
        LinearLayout searchtoolbar;
        View footer;
        View footerButtons;
        View footerNext;
        View footerProgress;
        View footerStop;

        Context context;
        AdapterView.OnItemClickListener clickListener;
        AdapterView.OnItemLongClickListener longClickListener;

        public FragmentHolder(Context context) {
            this.context = context;
        }

        public void create(View v) {
            grid = (RecyclerView) v.findViewById(R.id.grid);

            // DividerItemDecoration divider = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            // grid.addItemDecoration(divider);

            LayoutInflater inflater = LayoutInflater.from(context);

            toolbar = v.findViewById(R.id.search_header_toolbar_parent);
            searchpanel = v.findViewById(R.id.search_panel);
            searchtoolbar = (LinearLayout) v.findViewById(R.id.search_header_toolbar);

            toolbar.setVisibility(View.GONE);

            footer = inflater.inflate(R.layout.library_footer, null);
            footerButtons = footer.findViewById(R.id.search_footer_buttons);
            footerNext = footer.findViewById(R.id.search_footer_next);
            footerProgress = footer.findViewById(R.id.search_footer_progress);
            footerStop = footer.findViewById(R.id.search_footer_stop);

            footerNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "footer next");
                }
            });

            addFooterView(footer);

            updateGrid();
        }

        public String getLayout() {
            return "library";
        }

        public void updateGrid() {
            final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

            layout = R.layout.book_item;
            if (shared.getString(MainApplication.PREFERENCE_LIBRARY_LAYOUT + getLayout(), "").equals("book_list_item")) {
                setNumColumns(1);
                layout = R.layout.book_list_item;
            } else {
                setNumColumns(4);
                layout = R.layout.book_item;
            }

            BooksAdapter a = (BooksAdapter) grid.getAdapter();
            if (a != null)
                a.notifyDataSetChanged();
        }

        public boolean onOptionsItemSelected(MenuItem item) {
            final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
            if (item.getItemId() == R.id.action_grid) {
                SharedPreferences.Editor editor = shared.edit();
                if (layout == R.layout.book_list_item) {
                    editor.putString(MainApplication.PREFERENCE_LIBRARY_LAYOUT + getLayout(), "book_item");
                } else {
                    editor.putString(MainApplication.PREFERENCE_LIBRARY_LAYOUT + getLayout(), "book_list_item");
                }
                editor.commit();
                updateGrid();
                return true;
            }
            return false;
        }

        public void addFooterView(View v) {
        }

        public void setNumColumns(int i) {
            if (i == 1) {
                LinearLayoutManager lm = new LinearLayoutManager(context);
                grid.setLayoutManager(lm);
            } else {
                GridLayoutManager lm = new GridLayoutManager(context, i);
                lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return FragmentHolder.this.getSpanSize(position);
                    }
                });
                grid.setLayoutManager(lm);
            }
        }

        public int getSpanSize(int position) {
            return 1;
        }

        public void setOnItemClickListener(AdapterView.OnItemClickListener l) {
            clickListener = l;
        }

        public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener l) {
            longClickListener = l;
        }
    }

    public static class ByRecent implements Comparator<Storage.Book> {

        @Override
        public int compare(Storage.Book o1, Storage.Book o2) {
            return Long.valueOf(o2.info.last).compareTo(o1.info.last);
        }

    }

    public static class ByCreated implements Comparator<Storage.Book> {

        @Override
        public int compare(Storage.Book o1, Storage.Book o2) {
            return Long.valueOf(o1.info.created).compareTo(o2.info.created);
        }

    }

    public class LibraryAdapter extends BooksAdapter {
        ArrayList<Storage.Book> list = new ArrayList<>();

        public LibraryAdapter(FragmentHolder holder) {
            super(LibraryFragment.this.getContext(), holder);
        }

        @Override
        public int getItemViewType(int position) {
            return holder.layout;
        }

        @Override
        public String getAuthors(int position) {
            Storage.Book b = list.get(position);
            return b.info.authors;
        }

        @Override
        public String getTitle(int position) {
            Storage.Book b = list.get(position);
            return b.info.title;
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public Storage.Book getItem(int position) {
            return list.get(position);
        }

        public void refresh() {
            list.clear();
            ArrayList<Storage.Book> ll = storage.list();
            if (filter == null || filter.isEmpty()) {
                list = ll;
                clearTasks();
            } else {
                for (Storage.Book b : ll) {
                    if (SearchView.filter(filter, b.info.title)) {
                        list.add(b);
                    }
                }
            }
            Collections.sort(list, new ByCreated());
            notifyDataSetChanged();
        }

        @Override
        public void onBindViewHolder(final BookHolder h, int position) {
            super.onBindViewHolder(h, position);

            Storage.Book b = list.get(position);

            View convertView = h.itemView;

            if (b.cover == null || !b.cover.exists()) {
                downloadTask(b, convertView);
            } else {
                downloadTaskClean(convertView);
                downloadTaskUpdate(null, b, convertView);
            }
        }

        @Override
        public Bitmap downloadImageTask(CacheImagesAdapter.DownloadImageTask task) {
            try {
                Storage.Book book = (Storage.Book) task.item;
                Storage.FBook fbook = storage.read(book);
                File cover = Storage.coverFile(getContext(), book);
                if (!cover.exists() || cover.length() == 0) {
                    storage.createCover(fbook, cover);
                }
                fbook.close();
                book.cover = cover;
                try {
                    Bitmap bm = BitmapFactory.decodeStream(new FileInputStream(cover));
                    return bm;
                } catch (IOException e) {
                    cover.delete();
                    throw new RuntimeException(e);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to load cover", e);
            }
            return null;
        }


        @Override
        public void downloadTaskUpdate(CacheImagesAdapter.DownloadImageTask task, Object item, Object view) {
            super.downloadTaskUpdate(task, item, view);
            BookHolder h = new BookHolder((View) view);

            Storage.Book b = (Storage.Book) item;

            if (b.cover != null && b.cover.exists()) {
                ImageView image = (ImageView) ((View) view).findViewById(R.id.book_cover);
                try {
                    Bitmap bm = BitmapFactory.decodeStream(new FileInputStream(b.cover));
                    image.setImageBitmap(bm);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    public static abstract class BooksAdapter extends CacheImagesRecyclerAdapter<BooksAdapter.BookHolder> {
        String filter;
        FragmentHolder holder;

        public static class BookHolder extends RecyclerView.ViewHolder {
            TextView aa;
            TextView tt;

            public BookHolder(View itemView) {
                super(itemView);
                aa = (TextView) itemView.findViewById(R.id.book_authors);
                tt = (TextView) itemView.findViewById(R.id.book_title);
            }
        }

        public BooksAdapter(Context context, FragmentHolder holder) {
            super(context);
            this.holder = holder;
        }

        public Uri getCover(int position) {
            return null;
        }

        public String getAuthors(int position) {
            return "";
        }

        public String getTitle(int position) {
            return "";
        }

        public void refresh() {
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return -1;
        }

        @Override
        public BookHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View convertView = inflater.inflate(viewType, parent, false);
            BookHolder h = new BookHolder(convertView);
            return h;
        }

        @Override
        public void onBindViewHolder(final BookHolder h, int position) {
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.clickListener != null)
                        holder.clickListener.onItemClick(null, v, h.getAdapterPosition(), -1);
                }
            });
            h.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (holder.longClickListener != null)
                        holder.longClickListener.onItemLongClick(null, v, h.getAdapterPosition(), -1);
                    return true;
                }
            });

            setText(h.aa, getAuthors(position));
            setText(h.tt, getTitle(position));
        }

        @Override
        public void downloadTaskUpdate(CacheImagesAdapter.DownloadImageTask task, Object item, Object view) {
            View convertView = (View) view;
            ImageView image = (ImageView) convertView.findViewById(R.id.book_cover);
            ProgressBar progress = (ProgressBar) convertView.findViewById(R.id.book_progress);
            updateView(task, image, progress);
        }

        @Override
        public Bitmap downloadImageTask(CacheImagesAdapter.DownloadImageTask task) {
            Uri u = (Uri) task.item;
            Bitmap bm = downloadImage(u);
            if (bm == null)
                return null;
            if (bm.getWidth() > Storage.COVER_SIZE || bm.getHeight() > Storage.COVER_SIZE) {
                try {
                    File cover = CacheImagesAdapter.cacheUri(getContext(), u);
                    int m = Math.max(bm.getWidth(), bm.getHeight());
                    float ratio = Storage.COVER_SIZE / (float) m;
                    Bitmap sbm = Bitmap.createScaledBitmap(bm, (int) (bm.getWidth() * ratio), (int) (bm.getHeight() * ratio), true);
                    if (sbm == bm)
                        return bm;
                    bm.recycle();
                    FileOutputStream os = new FileOutputStream(cover);
                    sbm.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.close();
                    return sbm;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return bm;
        }

        void setText(TextView t, String s) {
            if (t == null)
                return;
            TextMax m = null;
            if (t.getParent() instanceof TextMax)
                m = (TextMax) t.getParent();
            ViewParent p = t.getParent();
            if (s == null || s.isEmpty()) {
                t.setVisibility(View.GONE);
                if (m != null)
                    m.setVisibility(View.GONE);
                return;
            }
            t.setVisibility(View.VISIBLE);
            t.setText(s);
            if (m != null)
                m.setVisibility(View.VISIBLE);
        }
    }

    public LibraryFragment() {
    }

    public static LibraryFragment newInstance() {
        LibraryFragment fragment = new LibraryFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = new Storage(getContext());
        holder = new FragmentHolder(getContext());
        books = new LibraryAdapter(holder);
        books.refresh();
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        books.refresh();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_library, container, false);

        holder.create(v);
        holder.footer.setVisibility(View.GONE);

        final MainActivity main = (MainActivity) getActivity();
        main.toolbar.setTitle(R.string.app_name);
        holder.grid.setAdapter(books);
        holder.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final MainActivity main = (MainActivity) getActivity();
                Storage.Book b = books.getItem(position);
                main.loadBook(b);
            }
        });
        holder.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Storage.Book b = books.getItem(position);
                PopupMenu popup = new PopupMenu(getContext(), view);
                popup.inflate(R.menu.bookitem_menu);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.action_rename) {
                            final OpenFileDialog.EditTextDialog e = new OpenFileDialog.EditTextDialog(getContext());
                            e.setTitle(R.string.book_rename);
                            e.setText(b.info.title);
                            e.setPositiveButton(new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String name = e.getText();
                                    b.info.title = name;
                                    storage.save(b);
                                    books.notifyDataSetChanged();
                                }
                            });
                            AlertDialog d = e.create();
                            d.show();
                        }
                        if (item.getItemId() == R.id.action_open) {
                            String ext = storage.getExt(b.url);
                            String t = b.info.title + "." + ext;
                            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                            Uri uri = StorageProvider.share(getContext(), b.url, t);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, type);
                            FileProvider.grantPermissions(getContext(), intent, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivity(intent);
                        }
                        if (item.getItemId() == R.id.action_share) {
                            String ext = storage.getExt(b.url);
                            String t = b.info.title + "." + ext;
                            String name = storage.getName(b.url);
                            String type = Storage.getTypeByName(name);
                            Uri uri = StorageProvider.share(getContext(), b.url, t);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType(type);
                            intent.putExtra(Intent.EXTRA_EMAIL, "");
                            intent.putExtra(Intent.EXTRA_SUBJECT, t);
                            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.shared_via, getString(R.string.app_name)));
                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                            FileProvider.grantPermissions(getContext(), intent, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivity(intent);
                        }
                        if (item.getItemId() == R.id.action_delete) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle(R.string.book_delete);
                            builder.setMessage(R.string.are_you_sure);
                            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    storage.delete(b);
                                    books.refresh();
                                }
                            });
                            builder.show();
                        }
                        return true;
                    }
                });
                popup.show();
                return true;
            }
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity main = ((MainActivity) getActivity());
        main.setFullscreen(false);
        main.clearMenu();
        main.libraryMenu.setChecked(true);
        books.refresh();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        books.clearTasks();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (Build.VERSION.SDK_INT < 11) {
            invalidateOptionsMenu = new Runnable() {
                @Override
                public void run() {
                    onCreateOptionsMenu(menu, null);
                }
            };
        }

        MenuItem homeMenu = menu.findItem(R.id.action_home);
        MenuItem tocMenu = menu.findItem(R.id.action_toc);
        MenuItem searchMenu = menu.findItem(R.id.action_search);
        MenuItem reflow = menu.findItem(R.id.action_reflow);
        MenuItem fontsize = menu.findItem(R.id.action_fontsize);
        MenuItem debug = menu.findItem(R.id.action_debug);
        MenuItem rtl = menu.findItem(R.id.action_rtl);
        MenuItem grid = menu.findItem(R.id.action_grid);
        MenuItem mode = menu.findItem(R.id.action_mode);

        reflow.setVisible(false);
        searchMenu.setVisible(true);
        homeMenu.setVisible(false);
        tocMenu.setVisible(false);
        fontsize.setVisible(false);
        debug.setVisible(false);
        rtl.setVisible(false);
        mode.setVisible(false);

        holder.updateGrid();
        if (holder.layout == R.layout.book_item) {
            grid.setIcon(R.drawable.ic_view_module_black_24dp);
        } else {
            grid.setIcon(R.drawable.ic_view_list_black_24dp);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (holder.onOptionsItemSelected(item)) {
            invalidateOptionsMenu.run();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void search(String s) {
        books.filter = s;
        books.refresh();
        lastSearch = books.filter;
    }

    @Override
    public void searchClose() {
        search("");
    }

    @Override
    public String getHint() {
        return getString(R.string.search_local);
    }
}
