package com.github.axet.bookreader.fragments;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.github.axet.androidlibrary.widgets.HeaderGridView;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.activities.MainActivity;
import com.github.axet.bookreader.app.Storage;

import org.geometerplus.android.fbreader.network.auth.AndroidNetworkContext;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.network.INetworkLink;
import org.geometerplus.fbreader.network.NetworkCatalogItem;
import org.geometerplus.fbreader.network.NetworkImage;
import org.geometerplus.fbreader.network.NetworkLibrary;
import org.geometerplus.fbreader.network.tree.NetworkBookTree;
import org.geometerplus.fbreader.network.tree.NetworkCatalogTree;
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader;
import org.geometerplus.fbreader.network.tree.SearchCatalogTree;
import org.geometerplus.fbreader.network.urlInfo.UrlInfo;
import org.geometerplus.fbreader.tree.FBTree;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class NetworkLibraryFragment extends Fragment {
    public static final String TAG = NetworkLibraryFragment.class.getSimpleName();

    BooksAdapter books;
    HeaderGridView grid;
    Storage storage;
    INetworkLink n;

    View searchpanel;
    ViewGroup searchtoolbar;

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(TAG, "broken download", e);
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
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

    public class BooksAdapter implements ListAdapter {
        List<FBTree> list = new ArrayList<>();
        DataSetObserver listener;

        public BooksAdapter() {
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
        public FBTree getItem(int position) {
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

            FBTree b = list.get(position);

            ZLImage cover = b.getCover();

            if (cover != null && cover instanceof NetworkImage) {
                new DownloadImageTask(image).execute(((NetworkImage) cover).Url);
            }

            text.setText(b.getName());

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

    public NetworkLibraryFragment() {
    }

    public static NetworkLibraryFragment newInstance(String n) {
        NetworkLibraryFragment fragment = new NetworkLibraryFragment();
        Bundle args = new Bundle();
        args.putString("url", n);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = new Storage(getContext());
        String u = getArguments().getString("url");
        NetworkLibrary lib = NetworkLibrary.Instance(new Storage.Info(getContext()));
        n = lib.getLinkByUrl(u);

        final AndroidNetworkContext nc = new AndroidNetworkContext() {
            @Override
            protected Context getContext() {
                return NetworkLibraryFragment.this.getContext();
            }

            @Override
            protected Map<String, String> authenticateWeb(URI uri, String realm, String authUrl, String completeUrl, String verificationUrl) {
                return null;
            }
        };

        final NetworkCatalogItem i = n.libraryItem();
        final NetworkCatalogTree tree = lib.getFakeCatalogTree(i);
        final NetworkItemsLoader l = new NetworkItemsLoader(nc, tree) {
            @Override
            protected void onFinish(ZLNetworkException exception, boolean interrupted) {
            }

            @Override
            protected void doBefore() throws ZLNetworkException {
            }

            @Override
            protected void load() throws ZLNetworkException {
            }
        };
        UIUtil.wait("load books", new Runnable() {
            @Override
            public void run() {
                try {
                    l.Tree.clearCatalog();
                    i.loadChildren(l);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            boolean books = false;
                            boolean catalog = false;
                            for (FBTree f : l.Tree.subtrees()) {
                                if (f instanceof NetworkBookTree)
                                    books = true;
                                if (f instanceof SearchCatalogTree)
                                    catalog = true;
                                if (f instanceof NetworkCatalogTree)
                                    catalog = true;
                            }
                            if (books) {
                                loadBooks(l.Tree.subtrees());
                            }
                            if (catalog) {
                                loadtoolBar(l.Tree.subtrees());
                            }
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, getContext());
    }

    void loadBooks(List<FBTree> l) {
        books.list = l;
        books.notifyDataSetChanged();
    }

    void loadtoolBar(List<FBTree> l) {
        boolean search = false;
        for (FBTree b : l) {
            if (b instanceof SearchCatalogTree) {
                search = true;
            }
        }
        if (!search) {
            searchpanel.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_library, container, false);

        View login = v.findViewById(R.id.search_header_login);
        login.setVisibility(View.GONE);

        searchpanel = v.findViewById(R.id.search_panel);
        searchtoolbar = (ViewGroup) v.findViewById(R.id.search_header_toolbar);

        grid = (HeaderGridView) v.findViewById(R.id.grid);

        final MainActivity main = (MainActivity) getActivity();
        main.toolbar.setTitle(R.string.app_name);
        books = new BooksAdapter();
        grid.setAdapter(books);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FBTree b = books.getItem(position);
                NetworkBookTree n = (NetworkBookTree) b;
                String u = ((NetworkBookTree) b).Book.getUrl(UrlInfo.Type.Book);
                main.loadBook(Uri.parse(u));
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
