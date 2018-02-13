package com.github.axet.bookreader.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.axet.androidlibrary.widgets.TreeListView;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.activities.FullscreenActivity;
import com.github.axet.bookreader.activities.MainActivity;
import com.github.axet.bookreader.app.MainApplication;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.widgets.FBReaderView;

import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.options.ColorProfile;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;

import java.util.List;

public class ReaderFragment extends Fragment implements MainActivity.SearchListener, SharedPreferences.OnSharedPreferenceChangeListener, FullscreenActivity.FullscreenListener {
    public static final String TAG = ReaderFragment.class.getSimpleName();

    Storage storage;
    FBReaderView view;
    AlertDialog tocdialog;
    View toolbarBottom;
    View fontsize;
    TextView fontsizetext;
    TextView fontsizepopup_text;
    SeekBar fontsizepopup_seek;
    View fontsizepopup_minus;
    View fontsizepopup_plus;

    BroadcastReceiver battery = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            view.battery = level * 100 / scale;
        }
    };

    public class TOCAdapter extends TreeListView.TreeAdapter {
        TOCTree current;

        public TOCAdapter(List<TOCTree> ll, TOCTree current) {
            this.current = current;
            loadTOC(root, ll);
            load();
        }

        void loadTOC(TreeListView.TreeNode r, List<TOCTree> tree) {
            for (TOCTree t : tree) {
                TreeListView.TreeNode n = new TreeListView.TreeNode(r, t);
                r.nodes.add(n);
                if (equals(t, current)) {
                    n.selected = true; // current selected
                    r.expanded = true; // parent expanded
                }
                if (t.hasChildren()) {
                    loadTOC(n, t.subtrees());
                    if (n.expanded) {
                        n.selected = true;
                        r.expanded = true;
                    }
                }
            }
        }

        public int getCurrent() {
            for (int i = 0; i < getCount(); i++) {
                TOCTree t = (TOCTree) getItem(i).tag;
                if (equals(t, current))
                    return i;
            }
            return -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.toc_item, null);
            }
            TreeListView.TreeNode t = getItem(position);
            TOCTree tt = (TOCTree) t.tag;
            ImageView ex = (ImageView) convertView.findViewById(R.id.expand);
            if (t.nodes.isEmpty())
                ex.setVisibility(View.INVISIBLE);
            else
                ex.setVisibility(View.VISIBLE);
            ex.setImageResource(t.expanded ? R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);
            convertView.setPadding(20 * t.level, 0, 0, 0);
            ImageView i = (ImageView) convertView.findViewById(R.id.image);
            TextView textView = (TextView) convertView.findViewById(R.id.text);
            if (t.selected) {
                textView.setTypeface(null, Typeface.BOLD);
                i.setColorFilter(null);
            } else {
                i.setColorFilter(Color.GRAY);
                textView.setTypeface(null, Typeface.NORMAL);
            }
            textView.setText(tt.getText());
            return convertView;
        }

        boolean equals(TOCTree t, TOCTree t2) {
            if (t == null || t2 == null)
                return false;
            TOCTree.Reference r1 = t.getReference();
            TOCTree.Reference r2 = t2.getReference();
            if (r1 == null || r2 == null)
                return false;
            return r1.ParagraphIndex == r2.ParagraphIndex;
        }
    }

    public ReaderFragment() {
    }

    public static ReaderFragment newInstance(Uri uri) {
        ReaderFragment fragment = new ReaderFragment();
        Bundle args = new Bundle();
        args.putParcelable("uri", uri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = new Storage(getContext());
        setHasOptionsMenu(true);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        shared.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).clearMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reader, container, false);

        toolbarBottom = v.findViewById(R.id.toolbar_bottom);
        final View fontsize_popup = v.findViewById(R.id.fontsize_popup);
        fontsizepopup_text = (TextView) fontsize_popup.findViewById(R.id.fontsize_text);
        fontsizepopup_plus = fontsize_popup.findViewById(R.id.fontsize_plus);
        fontsizepopup_minus = fontsize_popup.findViewById(R.id.fontsize_minus);
        fontsizepopup_seek = (SeekBar) fontsize_popup.findViewById(R.id.fontsize_seek);
        fontsize = v.findViewById(R.id.toolbar_fontsize);
        fontsize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fontsize_popup.getVisibility() == View.VISIBLE) {
                    fontsize_popup.setVisibility(View.GONE);
                } else {
                    fontsize_popup.setVisibility(View.VISIBLE);
                    updateFontsize();
                }
            }
        });
        fontsize_popup.setVisibility(View.GONE);
        final MainActivity main = (MainActivity) getActivity();
        view = (FBReaderView) v.findViewById(R.id.main_view);

        view.setColorProfile();

        Context context = getContext();
        context.registerReceiver(battery, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        view.setWindow(getActivity().getWindow());
        view.setActivity(getActivity());

        Uri uri = getArguments().getParcelable("uri");

        try {
            Storage.Book b = storage.load(uri);
            if (!b.isLoaded())
                storage.load(b);
            view.loadBook(b);
        } catch (RuntimeException e) {
            main.Error(e);
            main.openLibrary();
        }

        toolbarUpdate();

        return v;
    }

    void toolbarUpdate() {
        fontsize.setVisibility((view.pluginview == null || view.pluginview.reflow) ? View.VISIBLE : View.GONE);

        fontsizetext = (TextView) fontsize.findViewById(R.id.toolbar_icon_text);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (view.pluginview == null) {
            fontsizetext.setText("" + getFontsizeFB());
        } else {
            float f = getFontsizeReflow();
            fontsizetext.setText(String.format("%.1f", f));
        }
    }

    void updateFontsize() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (view.pluginview == null) {
            int f = getFontsizeFB();
            final int start = 20;
            final int end = 80;
            fontsizepopup_seek.setMax(end - start);
            fontsizepopup_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    fontsizepopup_text.setText(Integer.toString(progress + start));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int p = fontsizepopup_seek.getProgress();
                    setFontsizeFB(start + p);
                }
            });
            fontsizepopup_seek.setProgress(f - start);
            fontsizepopup_minus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int p = fontsizepopup_seek.getProgress();
                    p--;
                    if (p < 0)
                        p = 0;
                    fontsizepopup_seek.setProgress(p);
                    setFontsizeFB(start + p);
                }
            });
            fontsizepopup_plus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int p = fontsizepopup_seek.getProgress();
                    p++;
                    if (p >= end - start)
                        p = end - start;
                    fontsizepopup_seek.setProgress(p);
                    setFontsizeFB(start + p);
                }
            });
        } else {
            int f = (int) (getFontsizeReflow() * 10);
            final int start = 3;
            final int end = 15;
            final int step = 1;
            fontsizepopup_seek.setMax(end - start);
            fontsizepopup_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    fontsizepopup_text.setText(String.format("%.1f", (start + progress) / 10f));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    float p = fontsizepopup_seek.getProgress();
                    setFontsizeReflow((start + p) / 10f);
                }
            });
            fontsizepopup_seek.setProgress(f - start);
            fontsizepopup_minus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int p = fontsizepopup_seek.getProgress();
                    p -= step;
                    if (p < 0)
                        p = 0;
                    fontsizepopup_seek.setProgress(p);
                    setFontsizeReflow((start + p) / 10f);
                }
            });
            fontsizepopup_plus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int p = fontsizepopup_seek.getProgress();
                    p += step;
                    if (p >= end - start)
                        p = end - start;
                    fontsizepopup_seek.setProgress(p);
                    setFontsizeReflow((start + p) / 10f);
                }
            });
        }
    }

    public int getFontsizeFB() {
        return view.app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption.getValue();
    }

    void setFontsizeFB(int p) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor edit = shared.edit();
        edit.putInt(MainApplication.PREFERENCE_FONTSIZE_FBREADER, p);
        edit.apply();
        ZLIntegerRangeOption option = view.app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption;
        option.setValue(p);
        view.app.clearTextCaches();
        view.app.getViewWidget().repaint();
        toolbarUpdate();
    }

    float getFontsizeReflow() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        return shared.getFloat(MainApplication.PREFERENCE_FONTSIZE_REFLOW, MainApplication.PREFERENCE_FONTSIZE_REFLOW_DEFAULT);
    }

    void setFontsizeReflow(float p) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = shared.edit();
        editor.putFloat(MainApplication.PREFERENCE_FONTSIZE_REFLOW, p);
        editor.apply();
        view.pluginview.reflower.k2.setFontSize(p);
        view.reset();
        toolbarUpdate();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onPause() {
        super.onPause();
        savePosition();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        savePosition();
    }

    void savePosition() {
        if (view.book == null)
            return;
        view.book.info.position = view.getPosition();
        storage.save(view.book);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Context context = getContext();
        context.unregisterReceiver(battery);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        shared.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_toc) {
            showTOC();
            return true;
        }
        if (id == R.id.action_reflow) {
            view.pluginview.reflow = !view.pluginview.reflow;
            view.reset();
            toolbarUpdate();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem homeMenu = menu.findItem(R.id.action_home);
        MenuItem tocMenu = menu.findItem(R.id.action_toc);
        MenuItem searchMenu = menu.findItem(R.id.action_search);
        homeMenu.setVisible(false);
        tocMenu.setVisible(view.app.Model.TOCTree != null && view.app.Model.TOCTree.hasChildren());
        searchMenu.setVisible(view.pluginview == null); // pdf and djvu do not support search
        MenuItem reflow = menu.findItem(R.id.action_reflow);
        reflow.setVisible(view.pluginview != null);
    }

    void showTOC() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final TOCTree current = view.app.getCurrentTOCElement();
        final TOCAdapter a = new TOCAdapter(view.app.Model.TOCTree.subtrees(), current);
        final TreeListView tree = new TreeListView(getContext());
        tree.setAdapter(a);
        builder.setView(tree);
        tree.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                TOCTree n = (TOCTree) a.getItem(position).tag;
                if (n.hasChildren())
                    return;
                view.gotoPosition(n.getReference());
                tocdialog.dismiss();
            }
        });
        builder.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        tocdialog = builder.create();
        tocdialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                int i = a.getCurrent();
                tree.setSelection(i - 1);
            }
        });
        tocdialog.show();
    }

    @Override
    public void search(String s) {
        view.app.runAction(ActionCode.SEARCH, s);
    }

    @Override
    public void searchClose() {
        view.app.hideActivePopup();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(MainApplication.PREFERENCE_THEME)) {
            if (sharedPreferences.getString(key, "").equals(getString(R.string.Theme_Dark))) {
                view.setColorProfile(ColorProfile.NIGHT);
            } else {
                view.setColorProfile(ColorProfile.DAY);
            }
        }
    }

    @Override
    public String getHint() {
        return getString(R.string.search_book);
    }

    @Override
    public void onFullscreenChanged(boolean f) {
        if (f) {
            toolbarBottom.setVisibility(View.GONE);
        } else {
            toolbarBottom.setVisibility(View.VISIBLE);
        }
    }
}
