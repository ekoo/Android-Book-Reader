package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.axet.androidlibrary.widgets.TextMax;
import com.github.axet.androidlibrary.widgets.TreeListView;
import com.github.axet.androidlibrary.widgets.TreeRecyclerView;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.app.Storage;

import java.util.ArrayList;
import java.util.List;

public class BookmarksDialog extends AlertDialog.Builder {
    BMAdapter a;
    TreeRecyclerView tree;
    AlertDialog dialog;

    public static class BMHolder extends TreeRecyclerView.TreeHolder {
        ImageView image;
        TextView text;
        TextView name;

        public BMHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.image);
            text = (TextView) itemView.findViewById(R.id.text);
            name = (TextView) itemView.findViewById(R.id.name);
        }
    }

    public class BMAdapter extends TreeRecyclerView.TreeAdapter<BMHolder> {
        public BMAdapter() {
        }

        public BMAdapter(List<Storage.Bookmark> tree) {
            load(root, tree);
            load();
        }

        void load(TreeListView.TreeNode r, List<Storage.Bookmark> tree) {
            for (Storage.Bookmark t : tree) {
                TreeListView.TreeNode n = new TreeListView.TreeNode(r, t);
                r.nodes.add(n);
            }
        }

        @Override
        public BMHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View convertView = inflater.inflate(R.layout.bm_item, parent, false);
            return new BMHolder(convertView);
        }

        @Override
        public void onBindViewHolder(final BMHolder h, int position) {
            TreeListView.TreeNode t = getItem(h.getAdapterPosition(this));
            Storage.Bookmark tt = (Storage.Bookmark) t.tag;
            ImageView ex = (ImageView) h.itemView.findViewById(R.id.expand);
            if (t.nodes.isEmpty())
                ex.setVisibility(View.INVISIBLE);
            else
                ex.setVisibility(View.VISIBLE);
            ex.setImageResource(t.expanded ? R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);
            h.itemView.setPadding(20 * t.level, 0, 0, 0);
            if (t.selected) {
                h.text.setTypeface(null, Typeface.BOLD);
                h.image.setColorFilter(null);
            } else {
                h.image.setColorFilter(Color.GRAY);
                h.text.setTypeface(null, Typeface.NORMAL);
            }
            h.text.setText(tt.text.replaceAll("\n", " "));
            if (tt.name == null || tt.name.isEmpty()) {
                ((TextMax) h.name.getParent()).setVisibility(View.GONE);
            } else {
                h.name.setText(tt.name.replaceAll("\n", " "));
            }
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Storage.Bookmark n = (Storage.Bookmark) getItem(h.getAdapterPosition(BMAdapter.this)).tag;
                    selected(n);
                    dialog.dismiss();
                }
            });
        }
    }

    public class BMAdapterBooks extends BMAdapter {
        public BMAdapterBooks(ArrayList<Storage.Book> books) {
            loadBooks(root, books);
            load();
        }

        void loadBooks(TreeListView.TreeNode r, List<Storage.Book> books) {
            for (Storage.Book b : books) {
                if (b.info.bookmarks != null) {
                    TreeListView.TreeNode n = new TreeListView.TreeNode(r, b);
                    r.nodes.add(n);
                    load(n, b.info.bookmarks);
                }
            }
        }

        @Override
        public void onBindViewHolder(final BMHolder h, int position) {
            final TreeListView.TreeNode t = getItem(h.getAdapterPosition(this));
            if (t.tag instanceof Storage.Bookmark) {
                super.onBindViewHolder(h, position);
                h.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Storage.Book tt = (Storage.Book) t.parent.tag;
                        Storage.Bookmark n = (Storage.Bookmark) getItem(h.getAdapterPosition(BMAdapterBooks.this)).tag;
                        selected(tt, n);
                        dialog.dismiss();
                    }
                });
            } else {
                Storage.Book tt = (Storage.Book) t.tag;
                ImageView ex = (ImageView) h.itemView.findViewById(R.id.expand);
                if (t.nodes.isEmpty())
                    ex.setVisibility(View.INVISIBLE);
                else
                    ex.setVisibility(View.VISIBLE);
                ex.setImageResource(t.expanded ? R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);
                h.itemView.setPadding(20 * t.level, 0, 0, 0);
                if (t.selected) {
                    h.text.setTypeface(null, Typeface.BOLD);
                    h.image.setColorFilter(null);
                } else {
                    h.image.setColorFilter(Color.GRAY);
                    h.text.setTypeface(null, Typeface.NORMAL);
                }
                h.text.setText(Storage.getTitle(tt.info));
                ((TextMax) h.name.getParent()).setVisibility(View.GONE);
            }
        }
    }

    public BookmarksDialog(Context context) {
        super(context);
    }

    public void load(ArrayList<Storage.Book> all) {
        a = new BMAdapterBooks(all);
        tree = new TreeRecyclerView(getContext());
        tree.setAdapter(a);
        setView(tree);
        setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    public void load(Storage.Bookmarks bm) {
        a = new BMAdapter(bm);
        tree = new TreeRecyclerView(getContext());
        tree.setAdapter(a);
        setView(tree);
        setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    public AlertDialog create() {
        dialog = super.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
            }
        });
        return dialog;
    }

    @Override
    public AlertDialog show() {
        return super.show();
    }

    public void selected(Storage.Bookmark b) {
    }

    public void selected(Storage.Book book, Storage.Bookmark bm) {
    }
}
