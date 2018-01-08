package com.github.axet.bookreader.activities;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.PopupMenuCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.axet.androidlibrary.services.FileProvider;
import com.github.axet.androidlibrary.widgets.AboutPreferenceCompat;
import com.github.axet.androidlibrary.widgets.HeaderGridView;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.widgets.FBReaderView;
import com.github.johnpersano.supertoasts.SuperActivityToast;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.OnClickWrapper;

import org.geometerplus.android.fbreader.NavigationPopup;
import org.geometerplus.android.fbreader.PopupPanel;
import org.geometerplus.android.fbreader.SelectionPopup;
import org.geometerplus.android.fbreader.TextSearchPopup;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.bookmark.EditBookmarkActivity;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.android.util.UIMessageUtil;
import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.DictionaryHighlighting;
import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.util.TextSnippet;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.ZLTextView;

import java.util.ArrayList;

import group.pals.android.lib.ui.filechooser.utils.MimeTypes;

public class MainActivity extends FullscreenActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    FBReaderView view;
    Toolbar toolbar;
    Storage storage;
    HeaderGridView grid;
    BooksAdapter books;

    private BroadcastReceiver myBatteryInfoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            view.battery = level * 100 / scale;
        }
    };

    public class BooksAdapter implements ListAdapter {
        ArrayList<Storage.StoredBook> list;
        DataSetObserver listener;

        public BooksAdapter() {
            refresh();
        }

        public void refresh() {
            list = storage.list();
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
            LayoutInflater inflater = getLayoutInflater();
            View book = inflater.inflate(R.layout.book_view, null, false);
            ImageView image = (ImageView) book.findViewById(R.id.imageView);
            TextView text = (TextView) book.findViewById(R.id.textView);

            Storage.StoredBook b = list.get(position);

            if (!b.isLoaded())
                storage.load(b);

            if (b.bm != null) {
                Bitmap bmp = BitmapFactory.decodeStream(b.bm.inputStream());
                image.setImageBitmap(bmp);
            }

            String a = b.book.authorsString(" ");
            String t = b.book.getTitle();
            String m = "";
            if (a == null && m == null)
                m = b.md5;
            else if (a == null)
                m = t;
            else if (t == null)
                m = a;
            else
                m = a + " - " + t;
            text.setText(m);

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View navigationHeader = navigationView.getHeaderView(0);

        storage = new Storage(this);

        registerReceiver(myBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        view = (FBReaderView) findViewById(R.id.main_view);
        grid = (HeaderGridView) findViewById(R.id.grid);

        openLibrary();

        TextView ver = (TextView) navigationHeader.findViewById(R.id.textView);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = "v" + pInfo.versionName;
            ver.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            ver.setVisibility(View.GONE);
        }

        final FBReaderApp app = view.app;

        view.setWindow(getWindow());

        app.addAction(ActionCode.SHOW_MENU, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                toggle();
            }
        });
        app.addAction(ActionCode.SHOW_NAVIGATION, new FBAction(app) {
            @Override
            public boolean isVisible() {
                final ZLTextView view = (ZLTextView) Reader.getCurrentView();
                final ZLTextModel textModel = view.getModel();
                return textModel != null && textModel.getParagraphsNumber() != 0;
            }

            @Override
            protected void run(Object... params) {
                ((NavigationPopup) app.getPopupById(NavigationPopup.ID)).runNavigation();
            }
        });
        app.addAction(ActionCode.SELECTION_SHOW_PANEL, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final ZLTextView view = app.getTextView();
                ((SelectionPopup) app.getPopupById(SelectionPopup.ID))
                        .move(view.getSelectionStartY(), view.getSelectionEndY());
                app.showPopup(SelectionPopup.ID);
            }
        });
        app.addAction(ActionCode.SELECTION_HIDE_PANEL, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final FBReaderApp.PopupPanel popup = app.getActivePopup();
                if (popup != null && popup.getId() == SelectionPopup.ID) {
                    app.hideActivePopup();
                }
            }
        });
        app.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final FBView fbview = Reader.getTextView();
                final TextSnippet snippet = fbview.getSelectedSnippet();
                if (snippet == null) {
                    return;
                }

                final String text = snippet.getText();
                fbview.clearSelection();

                final ClipboardManager clipboard =
                        (ClipboardManager) getApplication().getSystemService(Application.CLIPBOARD_SERVICE);
                clipboard.setText(text);
                UIMessageUtil.showMessageText(
                        MainActivity.this,
                        ZLResource.resource("selection").getResource("textInBuffer").getValue().replace("%s", clipboard.getText())
                );
            }
        });
        app.addAction(ActionCode.SELECTION_SHARE, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final FBView fbview = Reader.getTextView();
                final TextSnippet snippet = fbview.getSelectedSnippet();
                if (snippet == null) {
                    return;
                }

                final String text = snippet.getText();
                final String title = Reader.getCurrentBook().getTitle();
                fbview.clearSelection();

                final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        ZLResource.resource("selection").getResource("quoteFrom").getValue().replace("%s", title)
                );
                intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(intent, null));
            }
        });
        app.addAction(ActionCode.SELECTION_TRANSLATE, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final FBView fbview = Reader.getTextView();
                final DictionaryHighlighting dictionaryHilite = DictionaryHighlighting.get(fbview);
                final TextSnippet snippet = fbview.getSelectedSnippet();

                if (dictionaryHilite == null || snippet == null) {
                    return;
                }

                DictionaryUtil.openTextInDictionary(
                        MainActivity.this,
                        snippet.getText(),
                        fbview.getCountOfSelectedWords() == 1,
                        fbview.getSelectionStartY(),
                        fbview.getSelectionEndY(),
                        new Runnable() {
                            public void run() {
                                fbview.addHighlighting(dictionaryHilite);
                                Reader.getViewWidget().repaint();
                            }
                        }
                );
                fbview.clearSelection();
            }
        });
        app.addAction(ActionCode.SELECTION_BOOKMARK, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final Bookmark bookmark;
                if (params.length != 0) {
                    bookmark = (Bookmark) params[0];
                } else {
                    bookmark = Reader.addSelectionBookmark();
                }
                if (bookmark == null) {
                    return;
                }

                final SuperActivityToast toast =
                        new SuperActivityToast(MainActivity.this, SuperToast.Type.BUTTON);
                toast.setText(bookmark.getText());
                toast.setDuration(SuperToast.Duration.EXTRA_LONG);
                toast.setButtonIcon(
                        android.R.drawable.ic_menu_edit,
                        ZLResource.resource("dialog").getResource("button").getResource("edit").getValue()
                );
                toast.setOnClickWrapper(new OnClickWrapper("bkmk", new SuperToast.OnClickListener() {
                    @Override
                    public void onClick(View view, Parcelable token) {
                        final Intent intent =
                                new Intent(getApplicationContext(), EditBookmarkActivity.class);
                        FBReaderIntents.putBookmarkExtra(intent, bookmark);
                        OrientationUtil.startActivity(MainActivity.this, intent);
                    }
                }));
                Toast.makeText(MainActivity.this, toast.getText(), toast.getDuration()).show();
            }
        });

        ((PopupPanel) app.getPopupById(TextSearchPopup.ID)).setPanelInfo(this, view);
        ((NavigationPopup) app.getPopupById(NavigationPopup.ID)).setPanelInfo(this, view);
        ((PopupPanel) app.getPopupById(SelectionPopup.ID)).setPanelInfo(this, view);

        loadIntent(getIntent());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (view.book != null) {
                openLibrary();
                return;
            }
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            AboutPreferenceCompat.buildDialog(this, R.raw.about).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_library) {
            openLibrary();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        loadIntent(intent);
    }

    void loadIntent(Intent intent) {
        if (intent == null)
            return;
        String a = intent.getAction();
        if (a == null)
            return;
        Uri u = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (u == null)
            u = intent.getData();
        if (u == null)
            return;
        Storage.StoredBook fbook = storage.load(u);
        loadBook(fbook);
    }

    void loadBook(Storage.StoredBook book) {
        grid.setVisibility(View.GONE);
        view.setVisibility(View.VISIBLE);
        toolbar.setTitle(book.book.getTitle());
        navigationView.getMenu().findItem(R.id.nav_library).setChecked(false);
        view.loadBook(book);
    }

    void closeBook() {
        Storage.StoredBook book = view.book;
        if (book == null)
            return;
        book.info.position = view.getPosition();
        view.closeBook();
        storage.save(book);
    }

    void openLibrary() {
        closeBook();
        toolbar.setTitle(R.string.app_name);
        grid.setVisibility(View.VISIBLE);
        view.setVisibility(View.GONE);
        navigationView.getMenu().findItem(R.id.nav_library).setChecked(true);
        books = new BooksAdapter();
        grid.setAdapter(books);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Storage.StoredBook b = books.getItem(position);
                loadBook(b);
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Storage.StoredBook b = books.getItem(position);
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                popup.inflate(R.menu.book_menu);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.action_open) {
                            String ext = Storage.getExt(b.file);
                            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                            String name = Storage.getNameNoExt(b.file);
                            Uri uri = FileProvider.getUriForFile(MainActivity.this, type, name, b.file);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, type);
                            FileProvider.grantPermissions(MainActivity.this, intent, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivity(intent);
                        }
                        if (item.getItemId() == R.id.action_share) {
                            String ext = Storage.getExt(b.file);
                            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                            String name = Storage.getNameNoExt(b.file);
                            Uri uri = FileProvider.getUriForFile(MainActivity.this, type, name, b.file);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.putExtra(Intent.EXTRA_EMAIL, "");
                            intent.putExtra(Intent.EXTRA_SUBJECT, b.book.getTitle());
                            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.shared_via, getString(R.string.app_name)));
                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                            FileProvider.grantPermissions(MainActivity.this, intent, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivity(intent);
                        }
                        if (item.getItemId() == R.id.action_delete) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
                                    b.file.delete();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myBatteryInfoReceiver);
        closeBook();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
