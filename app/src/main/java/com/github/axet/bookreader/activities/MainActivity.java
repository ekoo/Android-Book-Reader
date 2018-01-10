package com.github.axet.bookreader.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.axet.androidlibrary.widgets.AboutPreferenceCompat;
import com.github.axet.androidlibrary.widgets.OpenChoicer;
import com.github.axet.androidlibrary.widgets.OpenFileDialog;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.fragments.LibraryFragment;
import com.github.axet.bookreader.fragments.NetworkLibraryFragment;
import com.github.axet.bookreader.fragments.ReaderFragment;

import org.geometerplus.android.fbreader.network.Util;
import org.geometerplus.android.fbreader.network.auth.AndroidNetworkContext;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.network.INetworkLink;
import org.geometerplus.fbreader.network.NetworkLibrary;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends FullscreenActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    public static final int RESULT_FILE = 1;

    public static final String[] PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

    public Toolbar toolbar;
    Storage storage;
    OpenChoicer choicer;
    SubMenu networkMenu;
    Map<String, MenuItem> networkMenuMap = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        storage = new Storage(this);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View navigationHeader = navigationView.getHeaderView(0);

        openLibrary();

        TextView ver = (TextView) navigationHeader.findViewById(R.id.nav_version);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = "v" + pInfo.versionName;
            ver.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            ver.setVisibility(View.GONE);
        }

        Menu m = navigationView.getMenu();
        networkMenu = m.addSubMenu("Network Library");

        final AndroidNetworkContext nc = new AndroidNetworkContext() {
            @Override
            protected Context getContext() {
                return MainActivity.this;
            }

            @Override
            protected Map<String, String> authenticateWeb(URI uri, String realm, String authUrl, String completeUrl, String verificationUrl) {
                return null;
            }
        };
        UIUtil.wait("loadingNetworkLibrary", new Runnable() { // Util.initLibrary(this, nc, null);
            public void run() {
                final NetworkLibrary library = Util.networkLibrary(MainActivity.this);

                if (!library.isInitialized()) {
                    try {
                        library.initialize(nc);
                    } catch (ZLNetworkException e) {
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadMenu();
                    }
                });
            }
        }, this);

        loadIntent(getIntent());
    }

    void reloadMenu() {
        networkMenu.clear();
        NetworkLibrary lib = NetworkLibrary.Instance(new Storage.Info(MainActivity.this));
        List<String> ids = lib.activeIds();
        for (int i = 0; i < ids.size(); i++) {
            final INetworkLink link = lib.getLinkByUrl(ids.get(i));
            MenuItem m = networkMenu.add(link.getTitle());
            Intent intent = new Intent();
            intent.putExtra("url", ids.get(i));
            m.setIntent(intent);
            m.setIcon(R.drawable.ic_drag_handle_black_24dp);
            m.setCheckable(true);
            networkMenuMap.put(ids.get(i), m);
        }
        MenuItem m = networkMenu.add("Configure Catalogs");
        m.setIcon(R.drawable.ic_settings_black_24dp);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment f = getSupportFragmentManager().findFragmentByTag(ReaderFragment.TAG);
            if (f != null && f.isVisible()) {
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

        if (id == R.id.action_file) {
            choicer = new OpenChoicer(OpenFileDialog.DIALOG_TYPE.FILE_DIALOG, true) {
                @Override
                public void onResult(Uri uri) {
                    loadBook(uri);
                }
            };
            choicer.setStorageAccessFramework(this, RESULT_FILE);
            choicer.setPermissionsDialog(this, PERMISSIONS, RESULT_FILE);
            choicer.show(null);
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

        Intent i = item.getIntent();
        if (i != null) {
            openLibrary(i.getStringExtra("url"));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void openLibrary(String n) {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.main_content, NetworkLibraryFragment.newInstance(n), NetworkLibraryFragment.TAG).commit();
        MenuItem m = networkMenuMap.get(n);
        m.setChecked(true);
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
        loadBook(u);
    }

    public void loadBook(final Uri u) {
        int dp10 = ThemeUtils.dp2px(this, 10);

        ProgressBar v = new ProgressBar(this);
        v.setIndeterminate(true);
        v.setPadding(dp10, dp10, dp10, dp10);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Loading book");
        builder.setView(v);
        builder.setCancelable(false);
        final AlertDialog d = builder.create();
        d.show();

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    final Storage.Book fbook = storage.load(u);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBook(fbook);
                        }
                    });
                } catch (RuntimeException e) {
                    Post(e);
                } finally {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            d.cancel();
                        }
                    });
                }
            }
        };
        thread.start();
    }

    public void loadBook(Storage.Book book) {
        Uri uri = Uri.fromFile(book.file);
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.main_content, ReaderFragment.newInstance(uri), ReaderFragment.TAG).commit();
        navigationView.getMenu().findItem(R.id.nav_library).setChecked(false);
    }

    public void openLibrary() {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.main_content, new LibraryFragment(), LibraryFragment.TAG).commit();
        navigationView.getMenu().findItem(R.id.nav_library).setChecked(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RESULT_FILE:
                choicer.onRequestPermissionsResult(permissions, grantResults);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_FILE:
                choicer.onActivityResult(resultCode, data);
                break;
        }
    }

    public static String toString(Throwable e) {
        while (e.getCause() != null)
            e = e.getCause();
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty())
            msg = e.getClass().getSimpleName();
        return msg;
    }

    public void Post(final Throwable e) {
        Log.d(TAG, "Error", e);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Error(MainActivity.toString(e));
            }
        });
    }

    public void Error(Throwable e) {
        Log.d(TAG, "Error", e);
        Error(toString(e));
    }

    public void Error(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }
}
