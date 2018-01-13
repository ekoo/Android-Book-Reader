package com.github.axet.bookreader.fragments;

import android.content.Context;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.axet.androidlibrary.net.HttpClient;
import com.github.axet.androidlibrary.widgets.HeaderGridView;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.activities.MainActivity;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.widgets.BookDialog;
import com.github.axet.bookreader.widgets.BrowserDialogFragment;

import org.geometerplus.android.fbreader.network.auth.AndroidNetworkContext;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.network.INetworkLink;
import org.geometerplus.fbreader.network.NetworkCatalogItem;
import org.geometerplus.fbreader.network.NetworkImage;
import org.geometerplus.fbreader.network.NetworkLibrary;
import org.geometerplus.fbreader.network.tree.CatalogExpander;
import org.geometerplus.fbreader.network.tree.NetworkBookTree;
import org.geometerplus.fbreader.network.tree.NetworkCatalogTree;
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader;
import org.geometerplus.fbreader.network.tree.SearchCatalogTree;
import org.geometerplus.fbreader.network.urlInfo.UrlInfo;
import org.geometerplus.fbreader.tree.FBTree;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class NetworkLibraryFragment extends Fragment {
    public static final String TAG = NetworkLibraryFragment.class.getSimpleName();

    BooksAdapter books;
    HeaderGridView grid;
    Storage storage;
    INetworkLink n;
    NetworkLibrary lib;
    AndroidNetworkContext nc;
    SearchCatalogTree searchCatalog;

    View searchpanel;
    ViewGroup searchtoolbar;
    EditText edit;
    View toolbar;
    ArrayList<NetworkCatalogTree> toolbarItems = new ArrayList<>();
    List<FBTree> bookItems = new ArrayList<>();

    public class BooksAdapter implements ListAdapter {
        List<FBTree> list = new ArrayList<>();
        Map<Uri, LibraryFragment.BookViewHolder> views = new TreeMap<>();
        Map<ImageView, LibraryFragment.BookViewHolder> images = new HashMap<>();
        DataSetObserver listener;
        String filter;

        public BooksAdapter() {
        }

        public void notifyDataSetChanged() {
            if (listener != null)
                listener.onChanged();
        }

        void refresh() {
            if (filter == null || filter.isEmpty()) {
                list = bookItems;
                views.clear();
                images.clear();
            } else {
                list = new ArrayList<>();
                for (FBTree b : bookItems) {
                    if (b.getName().toLowerCase(Locale.US).contains(filter.toLowerCase(Locale.US))) {
                        list.add(b);
                    }
                }
            }
            notifyDataSetChanged();
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
            View book = inflater.inflate(R.layout.bookitem_view, null, false);
            ImageView image = (ImageView) book.findViewById(R.id.imageView);
            TextView text = (TextView) book.findViewById(R.id.textView);
            ProgressBar progress = (ProgressBar) book.findViewById(R.id.update_progress);

            FBTree b = list.get(position);

            ZLImage cover = b.getCover();

            progress.setVisibility(View.GONE);

            if (cover != null && cover instanceof NetworkImage) {
                Uri u = Uri.parse(((NetworkImage) cover).Url);
                LibraryFragment.BookViewHolder task = images.get(image);
                if (task != null) { // reuse imageview
                    task.views.remove(image);
                    task.progress = null;
                }
                task = views.get(u);
                if (task != null) { // add new ImageView to populate on finish
                    task.views.add(image);
                }
                if (task == null) {
                    task = new LibraryFragment.BookViewHolder(progress, image);
                    views.put(u, task);
                    images.put(image, task);
                    new LibraryFragment.DownloadImageTask(task).execute(u);
                } else if (task.bm != null) {
                    image.setImageBitmap(task.bm);
                }
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
        lib = NetworkLibrary.Instance(new Storage.Info(getContext()));
        n = lib.getLinkByUrl(u);

        nc = new AndroidNetworkContext() {
            @Override
            protected Context getContext() {
                return NetworkLibraryFragment.this.getContext();
            }

            @Override
            protected Map<String, String> authenticateWeb(URI uri, String realm, String authUrl, String completeUrl, String verificationUrl) {
                return null;
            }

            @Override
            protected void perform(ZLNetworkRequest request, int socketTimeout, int connectionTimeout) throws ZLNetworkException {
                super.perform(request, HttpClient.CONNECTION_TIMEOUT, HttpClient.CONNECTION_TIMEOUT);
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
                    if (l.Tree.subtrees().isEmpty())
                        i.loadChildren(l);
                    toolbarItems.clear();
                    expandCatalogs(toolbarItems, l.Tree);
                    boolean books = false;
                    for (FBTree f : l.Tree.subtrees()) {
                        if (f instanceof NetworkBookTree)
                            books = true;
                    }
                    if (books) {
                        bookItems = l.Tree.subtrees();
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadView();
                        }
                    });
                } catch (Exception e) {
                    MainActivity main = (MainActivity) getActivity();
                    main.Post(e);
                }
            }
        }, getContext());
    }

    void loadView() {
        loadBooks();
        loadtoolBar();
    }

    boolean expandCatalogs(ArrayList<NetworkCatalogTree> all, NetworkCatalogTree tree) {
        boolean c = false;
        for (FBTree f : tree.subtrees()) {
            if (all.size() > 4)
                return true;
            if (f instanceof NetworkCatalogTree) {
                c = true;
                NetworkCatalogTree t = (NetworkCatalogTree) f;
                if (t.subtrees().isEmpty())
                    new CatalogExpander(nc, t, false, false).run();
                boolean e = expandCatalogs(all, t);
                if (!e)
                    all.add(t);
            }
        }
        return c;
    }

    void loadBooks() {
        books.views.clear();
        books.images.clear();
        books.refresh();
    }

    void loadtoolBar() {
        searchtoolbar.removeAllViews();
        for (FBTree b : toolbarItems) {
            if (b instanceof SearchCatalogTree) {
                searchCatalog = (SearchCatalogTree) b;
                continue;
            }
            LayoutInflater inflater = LayoutInflater.from(getContext());
            final View t = inflater.inflate(R.layout.toolbar_icon, null);
            ImageView iv = (ImageView) t.findViewById(R.id.toolbar_icon_image);
            iv.setImageResource(R.drawable.ic_sort_black_24dp);
            TextView tv = (TextView) t.findViewById(R.id.toolbar_icon_text);
            tv.setText(b.getName());
            t.setTag(b);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectToolbar(t);
                }
            });
            searchtoolbar.addView(t);
        }

        if (searchtoolbar.getChildCount() == 0)
            toolbar.setVisibility(View.GONE);
        else
            toolbar.setVisibility(View.VISIBLE);
    }

    void selectToolbar(View v) {
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

        final NetworkCatalogTree tree = (NetworkCatalogTree) v.getTag();
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

        UIUtil.wait("load book", new Runnable() {
            @Override
            public void run() {
                try {
                    if (l.Tree.subtrees().isEmpty()) {
                        new CatalogExpander(nc, l.Tree, false, false).run();
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getArguments().putString("toolbar", l.Tree.getUniqueKey().Id);
                            bookItems = l.Tree.subtrees();
                            selectToolbar();
                            loadBooks();
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, getContext());
    }

    void selectToolbar() {
        String id = getArguments().getString("toolbar");
        if (id == null)
            return;
        for (int i = 0; i < searchtoolbar.getChildCount(); i++) {
            View v = searchtoolbar.getChildAt(i);
            NetworkCatalogTree b = (NetworkCatalogTree) v.getTag();
            ImageButton k = (ImageButton) v.findViewById(R.id.toolbar_icon_image);
            if (b.getUniqueKey().Id.equals(id)) {
                int[] states = new int[]{
                        android.R.attr.state_checked,
                };
                k.setImageState(states, false);
            } else {
                int[] states = new int[]{
                        -android.R.attr.state_checked,
                };
                k.setImageState(states, false);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_library, container, false);

        final View clear = v.findViewById(R.id.search_header_clear);
        edit = (EditText) v.findViewById(R.id.search_header_text);
        View home = v.findViewById(R.id.search_header_home);
        final View search = v.findViewById(R.id.search_header_search);
        View login = v.findViewById(R.id.search_header_login);
        toolbar = v.findViewById(R.id.search_header_toolbar_parent);
        View progress = v.findViewById(R.id.search_header_progress);
        View stop = v.findViewById(R.id.search_header_stop);
        searchpanel = v.findViewById(R.id.search_panel);
        searchtoolbar = (ViewGroup) v.findViewById(R.id.search_header_toolbar);

        toolbar.setVisibility(View.GONE);

        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String t = s.toString();
                if (t.isEmpty()) {
                    clear.setVisibility(View.GONE);
                } else {
                    clear.setVisibility(View.VISIBLE);
                }
            }
        });
        edit.setText("");

        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search.performClick();
                    return true;
                }
                return false;
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
                hideKeyboard();
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit.setText("");
                search();
                hideKeyboard();
            }
        });

        final String host = n.getHostName();
        if (host == null || host.isEmpty()) {
            home.setVisibility(View.GONE);
        } else {
            home.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openBrowser("http://" + host);
                }
            });
        }

        progress.setVisibility(View.GONE);
        stop.setVisibility(View.GONE);
        login.setVisibility(View.GONE);

        grid = (HeaderGridView) v.findViewById(R.id.grid);

        final MainActivity main = (MainActivity) getActivity();
        main.toolbar.setTitle(R.string.app_name);
        books = new BooksAdapter();
        grid.setAdapter(books);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    FBTree b = books.getItem(position);
                    final NetworkBookTree n = (NetworkBookTree) b;
                    String u = n.Book.getUrl(UrlInfo.Type.Book);
                    if (u == null) {
                        u = n.Book.getUrl(UrlInfo.Type.BookBuyInBrowser);
                        if (u == null)
                            u = n.Book.getUrl(UrlInfo.Type.HtmlPage);
                        if (n.Book.Id.startsWith("http"))
                            u = n.Book.Id;
                        if (u == null) {
                            Thread thread = new Thread() {
                                @Override
                                public void run() {
                                    n.Book.loadFullInformation(nc);
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            BookDialog d = new BookDialog();
                                            d.a.myTree = n;
                                            d.a.myBook = n.Book;
                                            d.show(getFragmentManager(), "");
                                        }
                                    });
                                }
                            };
                            thread.start();
                            return;

                        }
                        openBrowser(u);
                    } else {
                        main.loadBook(Uri.parse(u));
                    }
                } catch (RuntimeException e) {
                    main.Error(e);
                }
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadBooks();
        loadtoolBar();
        selectToolbar();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).setFullscreen(false);
        ((MainActivity) getActivity()).restoreNetworkSelection(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    void search() {
        if (searchCatalog == null) {
            books.filter = edit.getText().toString();
            books.refresh();
        } else {
            books.filter = null;
            books.refresh();
        }
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

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void hideKeyboard() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
            }
        });
    }

    public void openBrowser(String u) {
        BrowserDialogFragment b = BrowserDialogFragment.create(u);
        b.show(getFragmentManager(), "");
    }

}
