package com.github.axet.bookreader.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.axet.androidlibrary.widgets.PopupWindowCompat;
import com.github.axet.androidlibrary.widgets.ScreenlockPreference;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.androidlibrary.widgets.TreeListView;
import com.github.axet.androidlibrary.widgets.TreeRecyclerView;
import com.github.axet.bookreader.BuildConfig;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.activities.FullscreenActivity;
import com.github.axet.bookreader.activities.MainActivity;
import com.github.axet.bookreader.app.MainApplication;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.widgets.FBReaderView;
import com.github.axet.bookreader.widgets.ToolbarButtonView;
import com.github.axet.bookreader.widgets.ToolbarFontSizeView;

import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.options.ColorProfile;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.util.ZLTTFInfoDetector;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class ReaderFragment extends Fragment implements MainActivity.SearchListener, SharedPreferences.OnSharedPreferenceChangeListener, FullscreenActivity.FullscreenListener {
    public static final String TAG = ReaderFragment.class.getSimpleName();

    Storage storage;
    FBReaderView view;
    AlertDialog tocdialog;
    FontAdapter fonts;
    ListView fontsList;
    View fontsize_popup;
    TextView fontsizepopup_text;
    SeekBar fontsizepopup_seek;
    View fontsizepopup_minus;
    View fontsizepopup_plus;
    boolean showRTL;
    PopupWindow popupWindow;
    Handler handler = new Handler();
    Runnable time = new Runnable() {
        @Override
        public void run() {
            updateTime();
        }
    };

    BroadcastReceiver battery = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            onReceiveBattery(intent);
        }
    };

    Runnable invalidateOptionsMenu = new Runnable() {
        @Override
        public void run() {
            ActivityCompat.invalidateOptionsMenu(getActivity());
        }
    };

    public static class FontView {
        public String name;
        public Typeface font;
        public File file;

        public FontView(String name, File f) {
            this.name = name;
            this.file = f;
            this.font = Typeface.createFromFile(file);
        }

        public FontView(String name) {
            this.name = name;
            this.font = Typeface.create(name, Typeface.NORMAL);
        }
    }

    public static class FontAdapter extends BaseAdapter {
        Context context;
        public ArrayList<FontView> ff = new ArrayList<>();
        public int selected;

        public FontAdapter(Context context) {
            this.context = context;
        }

        public void addBasics() {
            add("sans-serif"); // "normal"
            add("serif");
            add("monospace");
        }

        public void loadTTF() {
            addBasics();
            HashMap<String, String> hh = enumerateFonts();
            for (String k : hh.keySet()) {
                String v = hh.get(k);
                ff.add(new FontView(v, new File(k)));
            }
        }

        public void select(String f) {
            for (int i = 0; i < ff.size(); i++) {
                if (ff.get(i).name.equals(f)) {
                    selected = i;
                }
            }
            notifyDataSetChanged();
        }

        public void select(int i) {
            selected = i;
            notifyDataSetChanged();
        }

        public void add(String f) {
            ff.add(new FontView(f));
        }

        @Override
        public int getCount() {
            return ff.size();
        }

        @Override
        public Object getItem(int position) {
            return ff.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (view == null) {
                final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(android.R.layout.select_dialog_singlechoice, parent, false);
            }

            if (view != null) {
                CheckedTextView tv = (CheckedTextView) view.findViewById(android.R.id.text1);
                tv.setChecked(selected == position);
                tv.setTypeface(ff.get(position).font);
                tv.setText(ff.get(position).name);
            }

            return view;
        }
    }

    public static class TOCHolder extends TreeRecyclerView.TreeHolder {
        ImageView i;
        TextView textView;

        public TOCHolder(View itemView) {
            super(itemView);
            i = (ImageView) itemView.findViewById(R.id.image);
            textView = (TextView) itemView.findViewById(R.id.text);
        }
    }

    public class TOCAdapter extends TreeRecyclerView.TreeAdapter<TOCHolder> {
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
            for (int i = 0; i < getItemCount(); i++) {
                TOCTree t = (TOCTree) getItem(i).tag;
                if (equals(t, current))
                    return i;
            }
            return -1;
        }

        @Override
        public TOCHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View convertView = inflater.inflate(R.layout.toc_item, null);
            return new TOCHolder(convertView);
        }

        @Override
        public void onBindViewHolder(final TOCHolder h, int position) {
            TreeListView.TreeNode t = getItem(h.getAdapterPosition(this));
            TOCTree tt = (TOCTree) t.tag;
            ImageView ex = (ImageView) h.itemView.findViewById(R.id.expand);
            if (t.nodes.isEmpty())
                ex.setVisibility(View.INVISIBLE);
            else
                ex.setVisibility(View.VISIBLE);
            ex.setImageResource(t.expanded ? R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);
            h.itemView.setPadding(20 * t.level, 0, 0, 0);
            if (t.selected) {
                h.textView.setTypeface(null, Typeface.BOLD);
                h.i.setColorFilter(null);
            } else {
                h.i.setColorFilter(Color.GRAY);
                h.textView.setTypeface(null, Typeface.NORMAL);
            }
            h.textView.setText(tt.getText());
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TOCTree n = (TOCTree) getItem(h.getAdapterPosition(TOCAdapter.this)).tag;
                    if (n.hasChildren())
                        return;
                    view.gotoPosition(n.getReference());
                    tocdialog.dismiss();
                }
            });
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

    // http://www.ulduzsoft.com/2012/01/enumerating-the-fonts-on-android-platform/
    public static class TTFAnalyzer {
        // This function parses the TTF file and returns the font name specified in the file
        public String getTtfFontName(File fontFilename) {
            try {
                // Parses the TTF file format.
                // See http://developer.apple.com/fonts/ttrefman/rm06/Chap6.html
                m_file = new RandomAccessFile(fontFilename, "r");

                // Read the version first
                int version = readDword();

                // The version must be either 'true' (0x74727565) or 0x00010000
                if (version != 0x74727565 && version != 0x00010000 && version != 0x4f54544f)
                    return null;

                // The TTF file consist of several sections called "tables", and we need to know how many of them are there.
                int numTables = readWord();

                // Skip the rest in the header
                readWord(); // skip searchRange
                readWord(); // skip entrySelector
                readWord(); // skip rangeShift

                // Now we can read the tables
                for (int i = 0; i < numTables; i++) {
                    // Read the table entry
                    int tag = readDword();
                    readDword(); // skip checksum
                    int offset = readDword();
                    int length = readDword();

                    // Now here' the trick. 'name' field actually contains the textual string name.
                    // So the 'name' string in characters equals to 0x6E616D65
                    if (tag == 0x6E616D65) {
                        // Here's the name section. Read it completely into the allocated buffer
                        byte[] table = new byte[length];

                        m_file.seek(offset);
                        read(table);

                        // This is also a table. See http://developer.apple.com/fonts/ttrefman/rm06/Chap6name.html
                        // According to Table 36, the total number of table records is stored in the second word, at the offset 2.
                        // Getting the count and string offset - remembering it's big endian.
                        int count = getWord(table, 2);
                        int string_offset = getWord(table, 4);

                        // Record starts from offset 6
                        for (int record = 0; record < count; record++) {
                            // Table 37 tells us that each record is 6 words -> 12 bytes, and that the nameID is 4th word so its offset is 6.
                            // We also need to account for the first 6 bytes of the header above (Table 36), so...
                            int nameid_offset = record * 12 + 6;
                            int platformID = getWord(table, nameid_offset);
                            int nameid_value = getWord(table, nameid_offset + 6);

                            // Table 42 lists the valid name Identifiers. We're interested in 4 but not in Unicode encoding (for simplicity).
                            // The encoding is stored as PlatformID and we're interested in Mac encoding
                            if (nameid_value == 4 && platformID == 1) {
                                // We need the string offset and length, which are the word 6 and 5 respectively
                                int name_length = getWord(table, nameid_offset + 8);
                                int name_offset = getWord(table, nameid_offset + 10);

                                // The real name string offset is calculated by adding the string_offset
                                name_offset = name_offset + string_offset;

                                // Make sure it is inside the array
                                if (name_offset >= 0 && name_offset + name_length < table.length)
                                    return new String(table, name_offset, name_length);
                            }
                        }
                    }
                }

                return null;
            } catch (FileNotFoundException e) {
                // Permissions?
                return null;
            } catch (IOException e) {
                // Most likely a corrupted font file
                return null;
            }
        }

        // Font file; must be seekable
        private RandomAccessFile m_file = null;

        // Helper I/O functions
        private int readByte() throws IOException {
            return m_file.read() & 0xFF;
        }

        private int readWord() throws IOException {
            int b1 = readByte();
            int b2 = readByte();

            return b1 << 8 | b2;
        }

        private int readDword() throws IOException {
            int b1 = readByte();
            int b2 = readByte();
            int b3 = readByte();
            int b4 = readByte();

            return b1 << 24 | b2 << 16 | b3 << 8 | b4;
        }

        private void read(byte[] array) throws IOException {
            if (m_file.read(array) != array.length)
                throw new IOException();
        }

        // Helper
        private int getWord(byte[] array, int offset) {
            int b1 = array[offset] & 0xFF;
            int b2 = array[offset + 1] & 0xFF;

            return b1 << 8 | b2;
        }
    }

    public ReaderFragment() {
    }

    static public HashMap<String, String> enumerateFonts() {
        String[] fontdirs = {"/system/fonts", "/system/font", "/data/fonts"};
        HashMap<String, String> fonts = new HashMap<>();
        TTFAnalyzer a = new TTFAnalyzer();

        for (String fontdir : fontdirs) {
            File dir = new File(fontdir);

            if (!dir.exists())
                continue;

            File[] files = dir.listFiles();

            if (files == null)
                continue;

            for (File file : files) {
                String n = a.getTtfFontName(file);
                if (n != null)
                    fonts.put(file.getAbsolutePath(), n);
            }
        }

        return fonts.isEmpty() ? null : fonts;
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

    void onReceiveBattery(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        view.battery = level * 100 / scale;
        view.widget.repaint();
    }

    void updateTime() {
        long s60 = 60 * 1000;
        long secs = System.currentTimeMillis() % s60;
        handler.removeCallbacks(time);
        handler.postDelayed(time, s60 - secs);
        view.widget.repaint();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_reader, container, false);

        fontsize_popup = inflater.inflate(R.layout.font_popup, new FrameLayout(getContext()), false);
        fontsizepopup_text = (TextView) fontsize_popup.findViewById(R.id.fontsize_text);
        fontsizepopup_plus = fontsize_popup.findViewById(R.id.fontsize_plus);
        fontsizepopup_minus = fontsize_popup.findViewById(R.id.fontsize_minus);
        fontsizepopup_seek = (SeekBar) fontsize_popup.findViewById(R.id.fontsize_seek);
        fonts = new FontAdapter(getContext());
        fontsList = (ListView) fontsize_popup.findViewById(R.id.fonts_list);
        fontsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                fonts.select(position);
                setFontFB(fonts.ff.get(position).name);
            }
        });
        final MainActivity main = (MainActivity) getActivity();
        view = (FBReaderView) v.findViewById(R.id.main_view);

        view.pageTurningListener = new FBReaderView.PageTurningListener() {
            @Override
            public void onScrollingFinished(ZLViewEnums.PageIndex index) {
                if (popupWindow != null)
                    popupWindow.dismiss();
                updateToolbar();
            }
        };

        view.setColorProfile();

        Context context = getContext();
        onReceiveBattery(context.registerReceiver(battery, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));

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

        if (view.pluginview == null) {
            fontsList.setVisibility(View.VISIBLE);
            fontsList.setAdapter(fonts);
            List<File> files = new ArrayList<>();
            for (String f : enumerateFonts().keySet()) {
                files.add(new File(f));
            }
            AndroidFontUtil.ourFileSet = new TreeSet<>();
            AndroidFontUtil.ourFontFileMap = new ZLTTFInfoDetector().collectFonts(files);
            fonts.addBasics();
            for (String s : AndroidFontUtil.ourFontFileMap.keySet()) {
                File[] ff = AndroidFontUtil.ourFontFileMap.get(s);
                for (File f : ff) {
                    if (f != null) {
                        fonts.ff.add(new FontView(s, f));
                        break; // regular first
                    }
                }
            }
        } else {
            fontsList.setVisibility(View.GONE);
        }

        updateToolbar();

        updateTime();

        handler.post(new Runnable() {
            @Override
            public void run() {
                updateToolbar(); // update toolbar after page been drawen to detect RTL
            }
        });

        return v;
    }

    void updateToolbar() {
        invalidateOptionsMenu.run();
    }

    void updateFontsize() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (view.pluginview == null) {
            int f = getFontsizeFB();
            final int start = 15;
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
        updateToolbar();
    }

    void setFontFB(String f) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor edit = shared.edit();
        edit.putString(MainApplication.PREFERENCE_FONTFAMILY_FBREADER, f);
        edit.apply();
        view.app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption.setValue(f);
        view.app.clearTextCaches();
        view.app.getViewWidget().repaint();
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
        updateToolbar();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onPause() {
        super.onPause();
        savePosition();
        ScreenlockPreference.onPause(getActivity(), MainApplication.PREFERENCE_SCREENLOCK);
    }

    @Override
    public void onResume() {
        super.onResume();
        ScreenlockPreference.onResume(getActivity(), MainApplication.PREFERENCE_SCREENLOCK);
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
        handler.removeCallbacksAndMessages(null);
        Context context = getContext();
        context.unregisterReceiver(battery);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        shared.unregisterOnSharedPreferenceChangeListener(this);
        handler.removeCallbacks(time);
        ScreenlockPreference.onUserInteractionRemove();
        if (popupWindow != null)
            popupWindow.dismiss();
    }

    @Override
    public void onUserInteraction() {
        ScreenlockPreference.onUserInteraction(getActivity(), MainApplication.PREFERENCE_SCREENLOCK);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (popupWindow != null)
            popupWindow.dismiss();
        int id = item.getItemId();
        if (id == R.id.action_toc) {
            showTOC();
            return true;
        }
        if (id == R.id.action_reflow) {
            view.pluginview.reflow = !view.pluginview.reflow;
            view.reset();
            updateToolbar();
        }
        if (id == R.id.action_debug) {
            view.pluginview.reflowDebug = !view.pluginview.reflowDebug;
            if (view.pluginview.reflowDebug)
                view.pluginview.reflow = true;
            view.reset();
            updateToolbar();
        }
        if (id == R.id.action_fontsize) {
            popupWindow = new PopupWindow(fontsize_popup);
            fonts.select(view.app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption.getValue());
            fontsList.setSelection(fonts.selected);
            updateFontsize();
            PopupWindowCompat.showAsTooltip(popupWindow, MenuItemCompat.getActionView(item), Gravity.BOTTOM,
                    MainApplication.getTheme(getContext(),
                            ThemeUtils.getColor(getContext(), R.color.button_material_light),
                            ThemeUtils.getColor(getContext(), R.color.button_material_dark)),
                    ThemeUtils.dp2px(getContext(), 300));
        }
        if (id == R.id.action_rtl) {
            view.app.BookTextView.rtlMode = !view.app.BookTextView.rtlMode;
            view.reset();
            updateToolbar();
        }
        return super.onOptionsItemSelected(item);
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
        MenuItem debug = menu.findItem(R.id.action_debug);
        final MenuItem fontsize = menu.findItem(R.id.action_fontsize);
        final MenuItem rtl = menu.findItem(R.id.action_rtl);
        MenuItem grid = menu.findItem(R.id.action_grid);

        grid.setVisible(false);
        homeMenu.setVisible(false);
        tocMenu.setVisible(view.app.Model.TOCTree != null && view.app.Model.TOCTree.hasChildren());
        searchMenu.setVisible(view.pluginview == null); // pdf and djvu do not support search
        reflow.setVisible(view.pluginview != null);

        if (BuildConfig.DEBUG && view.pluginview != null) {
            debug.setVisible(true);
        } else {
            debug.setVisible(false);
        }

        fontsize.setVisible((view.pluginview == null || view.pluginview.reflow) ? true : false);
        if (view.pluginview == null) {
            ((ToolbarButtonView) MenuItemCompat.getActionView(fontsize)).text.setText("" + getFontsizeFB());
        } else {
            ((ToolbarButtonView) MenuItemCompat.getActionView(fontsize)).text.setText(String.format("%.1f", getFontsizeReflow()));
        }
        MenuItemCompat.getActionView(fontsize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(fontsize);
            }
        });

        showRTL |= !view.app.BookTextView.rtlMode && view.app.BookTextView.rtlDetected;
        if (showRTL) {
            rtl.setVisible(true);
        } else {
            rtl.setVisible(false);
        }
        MenuItemCompat.getActionView(rtl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(rtl);
            }
        });
        rtl.setTitle(view.app.BookTextView.rtlMode ? "RTL" : "LTR");
        ((ToolbarButtonView) MenuItemCompat.getActionView(rtl)).text.setText(view.app.BookTextView.rtlMode ? "RTL" : "LTR");
    }

    void showTOC() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final TOCTree current = view.app.getCurrentTOCElement();
        final TOCAdapter a = new TOCAdapter(view.app.Model.TOCTree.subtrees(), current);
        final TreeRecyclerView tree = new TreeRecyclerView(getContext());
        tree.setAdapter(a);
        builder.setView(tree);
        builder.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        tocdialog = builder.create();
        tocdialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                final int i = a.getCurrent() - 1;
                if (i > 0)
                    tree.setSelection(i);
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
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            view.app.runAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD);
            return true;
        }
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            view.app.runAction(ActionCode.VOLUME_KEY_SCROLL_BACK);
            return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            return true;
        }
        return false;
    }
}
