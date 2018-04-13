package com.github.axet.bookreader.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
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
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter;
import com.github.axet.androidlibrary.widgets.CacheImagesListAdapter;
import com.github.axet.androidlibrary.widgets.HeaderGridView;
import com.github.axet.androidlibrary.widgets.OpenFileDialog;
import com.github.axet.androidlibrary.widgets.TextMax;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.activities.MainActivity;
import com.github.axet.bookreader.app.MainApplication;
import com.github.axet.bookreader.app.Storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

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
        HeaderGridView grid;

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

        public FragmentHolder(Context context) {
            this.context = context;
        }

        public void create(View v) {
            grid = (HeaderGridView) v.findViewById(R.id.grid);

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

            grid.addFooterView(footer);

            updateGrid();
        }

        public String getLayout() {
            return "library";
        }

        public void updateGrid() {
            final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

            layout = R.layout.book_item;
            if (shared.getString(MainApplication.PREFERENCE_LIBRARY_LAYOUT + getLayout(), "").equals("book_list_item")) {
                grid.setNumColumns(1);
                layout = R.layout.book_list_item;
            } else {
                grid.setNumColumns(4);
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

        public LibraryAdapter() {
            super(LibraryFragment.this.getContext());
        }

        @Override
        public int getLayout() {
            return holder.layout;
        }

        public Uri getCover(int position) {
            Storage.Book b = list.get(position);
            if (b.cover != null)
                return Uri.fromFile(b.cover);
            return null;
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
        public int getCount() {
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
                    if (b.info.title.toLowerCase(Locale.US).contains(filter.toLowerCase(Locale.US))) {
                        list.add(b);
                    }
                }
            }
            Collections.sort(list, new ByCreated());
            notifyDataSetChanged();
        }
    }

    public static abstract class BooksAdapter extends CacheImagesListAdapter {
        String filter;

        public static class BookHolder {
            TextView aa;
            TextView tt;

            public BookHolder(View itemView) {
                aa = (TextView) itemView.findViewById(R.id.book_authors);
                tt = (TextView) itemView.findViewById(R.id.book_title);
            }
        }

        public BooksAdapter(Context context) {
            super(context);
        }

        public Uri getCover(int position) {
            return null;
        }

        public int getLayout() {
            return -1;
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
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return getLayout() == R.layout.book_list_item ? 1 : 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            if (convertView == null)
                convertView = inflater.inflate(getLayout(), null, false);
            BookHolder h = new BookHolder(convertView);

            Uri cover = getCover(position);

            if (cover != null) {
                downloadTask(cover, convertView);
            } else {
                downloadTaskUpdate(null, cover, convertView);
            }

            setText(h.aa, getAuthors(position));
            setText(h.tt, getTitle(position));

            return convertView;
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
            return downloadImage((Uri) task.item);
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
        books = new LibraryAdapter();
        books.refresh();
        setHasOptionsMenu(true);
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
        holder.grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final MainActivity main = (MainActivity) getActivity();
                Storage.Book b = books.getItem(position);
                main.loadBook(b);
            }
        });
        holder.grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
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
                            String ext = Storage.getExt(b.file);
                            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                            String name = Storage.getNameNoExt(b.file);
                            Uri uri = FileProvider.getUriForFile(getContext(), type, name, b.file);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, type);
                            FileProvider.grantPermissions(getContext(), intent, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivity(intent);
                        }
                        if (item.getItemId() == R.id.action_share) {
                            String ext = Storage.getExt(b.file);
                            String type = Storage.getTypeByName(b.file.getName());
                            Uri uri = FileProvider.getUriForFile(getContext(), type, b.info.title + "." + ext, b.file);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType(type);
                            intent.putExtra(Intent.EXTRA_EMAIL, "");
                            intent.putExtra(Intent.EXTRA_SUBJECT, b.info.title + "." + ext);
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

        reflow.setVisible(false);
        searchMenu.setVisible(true);
        homeMenu.setVisible(false);
        tocMenu.setVisible(false);
        fontsize.setVisible(false);
        debug.setVisible(false);
        rtl.setVisible(false);

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
