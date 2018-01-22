package com.github.axet.bookreader.fragments;

import android.content.Context;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.axet.bookreader.R;
import com.github.axet.bookreader.activities.MainActivity;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.widgets.BookDialog;
import com.github.axet.bookreader.widgets.BrowserDialogFragment;

import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.network.INetworkLink;
import org.geometerplus.fbreader.network.NetworkCatalogItem;
import org.geometerplus.fbreader.network.NetworkImage;
import org.geometerplus.fbreader.network.NetworkLibrary;
import org.geometerplus.fbreader.network.NetworkOperationData;
import org.geometerplus.fbreader.network.SearchItem;
import org.geometerplus.fbreader.network.SingleCatalogSearchItem;
import org.geometerplus.fbreader.network.tree.CatalogExpander;
import org.geometerplus.fbreader.network.tree.NetworkBookTree;
import org.geometerplus.fbreader.network.tree.NetworkCatalogTree;
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader;
import org.geometerplus.fbreader.network.tree.SearchCatalogTree;
import org.geometerplus.fbreader.network.urlInfo.UrlInfo;
import org.geometerplus.fbreader.tree.FBTree;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.network.ZLNetworkContext;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class NetworkLibraryFragment extends Fragment implements MainActivity.SearchListener {
    public static final String TAG = NetworkLibraryFragment.class.getSimpleName();

    LibraryFragment.FragmentHolder holder;
    NetworkLibraryAdapter books;
    Storage storage;
    INetworkLink n;
    NetworkLibrary lib;
    Storage.NetworkContext nc;
    SearchCatalogTree searchCatalog;
    String host;

    ArrayList<NetworkCatalogTree> toolbarItems = new ArrayList<>();

    public class NetworkLibraryAdapter extends LibraryFragment.BooksAdapter {
        List<FBTree> list = new ArrayList<>();
        Map<Uri, LibraryFragment.BookViewHolder> views = new TreeMap<>();
        Map<ImageView, LibraryFragment.BookViewHolder> images = new HashMap<>();
        DataSetObserver listener;
        String filter;
        List<FBTree> bookItems = new ArrayList<>();

        public NetworkLibraryAdapter() {
            super(getContext());
        }

        public void refresh() {
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
        public int getCount() {
            return list.size();
        }

        @Override
        public FBTree getItem(int position) {
            return list.get(position);
        }

        @Override
        public String getTitle(int position) {
            FBTree b = list.get(position);
            return b.getName();
        }

        @Override
        public Uri getCover(int position) {
            FBTree b = list.get(position);

            ZLImage cover = b.getCover();
            if (cover != null && cover instanceof NetworkImage) {
                Uri u = Uri.parse(((NetworkImage) cover).Url);
                return u;
            }
            return null;
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
        holder = new LibraryFragment.FragmentHolder(getContext());
        String u = getArguments().getString("url");
        lib = NetworkLibrary.Instance(new Storage.Info(getContext()));
        n = lib.getLinkByUrl(u);
        books = new NetworkLibraryAdapter();

        setHasOptionsMenu(true);

        nc = new Storage.NetworkContext(getContext());

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
        UIUtil.wait("load catalogs", new Runnable() {
            @Override
            public void run() {
                try {
                    if (l.Tree.subtrees().isEmpty())
                        i.loadChildren(l);
                    toolbarItems.clear();
                    expandCatalogs(toolbarItems, l.Tree);
                    books.bookItems = new ArrayList<>();
                    for (FBTree c : l.Tree.subtrees()) {
                        if (c instanceof NetworkBookTree)
                            books.bookItems.add(c);
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
                if (t.subtrees().isEmpty()) {
                    CatalogExpander e = new CatalogExpander(nc, t, false, false) {
                        @Override
                        protected void onFinish(ZLNetworkException exception, boolean interrupted) {
                            super.onFinish(exception, interrupted);
                        }
                    };
                    e.run();
                }
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
        holder.searchtoolbar.removeAllViews();
        for (FBTree b : toolbarItems) {
            if (b instanceof SearchCatalogTree) {
                searchCatalog = (SearchCatalogTree) b;
                continue;
            }
            LayoutInflater inflater = LayoutInflater.from(getContext());
            final View t = inflater.inflate(R.layout.networktoolbar_item, null);
            ImageView iv = (ImageView) t.findViewById(R.id.toolbar_icon_image);
            iv.setImageResource(R.drawable.ic_sort_black_24dp);
            TextView tv = (TextView) t.findViewById(R.id.toolbar_icon_text);
            tv.setText(b.getName().trim());
            t.setTag(b);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectToolbar(t);
                }
            });
            holder.searchtoolbar.addView(t);
        }

        if (holder.searchtoolbar.getChildCount() == 0)
            holder.toolbar.setVisibility(View.GONE);
        else
            holder.toolbar.setVisibility(View.VISIBLE);
    }

    void selectToolbar(View v) {
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

        UIUtil.wait("load books", new Runnable() {
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
                            books.bookItems = l.Tree.subtrees();
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
        for (int i = 0; i < holder.searchtoolbar.getChildCount(); i++) {
            View v = holder.searchtoolbar.getChildAt(i);
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

        holder.create(v);

        final MainActivity main = (MainActivity) getActivity();

        host = n.getHostName();
        if (host == null || host.isEmpty()) {
            main.searchMenu.setVisible(false);
        } else {
            main.searchMenu.setVisible(true);
        }

        main.toolbar.setTitle(R.string.app_name);
        holder.grid.setAdapter(books);
        holder.grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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

    public void search(final String ss) {
        if (ss == null || ss.isEmpty())
            return;
        if (searchCatalog == null) {
            books.filter = ss;
            books.refresh();
        } else {
            UIUtil.wait("search", new Runnable() {
                @Override
                public void run() {
                    final SingleCatalogSearchItem s = new SingleCatalogSearchItem(n) {
                        NetworkOperationData data;

                        @Override
                        public void runSearch(ZLNetworkContext nc, NetworkItemsLoader loader, String pattern) throws ZLNetworkException {
                            NetworkOperationData data = Link.createOperationData(loader);
                            ZLNetworkRequest request = Link.simpleSearchRequest(pattern, data);
                            nc.perform(request);
                            if (loader.confirmInterruption()) {
                                return;
                            }
                        }

                        public void resume() throws ZLNetworkException {
                            ZLNetworkRequest request = data.resume();
                            nc.perform(request);
                        }
                    };
                    final NetworkCatalogItem i = n.libraryItem();
                    final String myPattern = ss;
                    final NetworkItemsLoader l = new NetworkItemsLoader(nc, searchCatalog) {
                        @Override
                        protected void onFinish(ZLNetworkException exception, boolean interrupted) {
                        }

                        @Override
                        protected void doBefore() throws ZLNetworkException {
                        }

                        @Override
                        protected void load() throws ZLNetworkException {
                            final SearchItem item = (SearchItem) Tree.Item;
                            if (myPattern.equals(item.getPattern())) {
                                if (Tree.hasChildren()) {
                                    return;
                                }
                            }
                            s.runSearch(nc, this, myPattern);
                        }
                    };
                    l.run();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            books.bookItems = l.Tree.subtrees();
                            books.filter = null;
                            books.refresh();
                        }
                    });
                }
            }, getContext());
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

    public void openBrowser(String u) {
        BrowserDialogFragment b = BrowserDialogFragment.create(u);
        b.show(getFragmentManager(), "");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_home) {
            openBrowser("http://" + host);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        final MainActivity main = (MainActivity) getActivity();
        if (host == null || host.isEmpty())
            main.homeMenu.setVisible(false);
        else
            main.homeMenu.setVisible(true);
        main.tocMenu.setVisible(false);
    }

    @Override
    public void searchClose() {
    }
}
