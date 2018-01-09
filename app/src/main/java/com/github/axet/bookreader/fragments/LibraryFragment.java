package com.github.axet.bookreader.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.github.axet.androidlibrary.services.FileProvider;
import com.github.axet.androidlibrary.widgets.HeaderGridView;
import com.github.axet.androidlibrary.widgets.OpenFileDialog;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.activities.MainActivity;
import com.github.axet.bookreader.app.MainApplication;
import com.github.axet.bookreader.app.Storage;

import java.util.ArrayList;

public class LibraryFragment extends Fragment {
    public static final String TAG = LibraryFragment.class.getSimpleName();

    BooksAdapter books;
    HeaderGridView grid;
    Storage storage;

    public class BooksAdapter implements ListAdapter {
        ArrayList<Storage.StoredBook> list;
        DataSetObserver listener;

        public BooksAdapter() {
            refresh();
        }

        public void refresh() {
            list = storage.list();
            notifyDataSetChanged();
        }

        public void notifyDataSetChanged() {
            if (listener != null)
                listener.onChanged();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            listener = observer;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            listener = null;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Storage.StoredBook getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View book = inflater.inflate(R.layout.book_view, null, false);
            ImageView image = (ImageView) book.findViewById(R.id.imageView);
            TextView text = (TextView) book.findViewById(R.id.textView);

            Storage.StoredBook b = list.get(position);

            if (b.cover != null) {
                Bitmap bmp = BitmapFactory.decodeFile(b.cover.getPath());
                image.setImageBitmap(bmp);
            }

            text.setText(b.info.title);

            return book;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
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

        final MainActivity main = (MainActivity) getActivity();
        main.navigationView.getMenu().findItem(R.id.nav_library).setChecked(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_library, container, false);

        grid = (HeaderGridView) v.findViewById(R.id.grid);

        final MainActivity main = (MainActivity) getActivity();
        main.toolbar.setTitle(R.string.app_name);
        books = new BooksAdapter();
        grid.setAdapter(books);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Storage.StoredBook b = books.getItem(position);
                main.loadBook(b);
            }
        });

        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Storage.StoredBook b = books.getItem(position);
                PopupMenu popup = new PopupMenu(getContext(), view);
                popup.inflate(R.menu.book_menu);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.action_rename) {
                            final OpenFileDialog.EditTextDialog e = new OpenFileDialog.EditTextDialog(getContext());
                            e.setTitle("Rename Book");
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
                            String type = MainApplication.getTypeByName(b.file.getName());
                            Uri uri = FileProvider.getUriForFile(getContext(), type, b.info.title + "." + ext, b.file);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType(type);
                            intent.putExtra(Intent.EXTRA_EMAIL, "");
                            intent.putExtra(Intent.EXTRA_SUBJECT, b.info.title);
                            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.shared_via, getString(R.string.app_name)));
                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                            FileProvider.grantPermissions(getContext(), intent, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivity(intent);
                        }
                        if (item.getItemId() == R.id.action_delete) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("Delete Book?");
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
    }

}
