package com.github.axet.bookreader.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.axet.bookreader.R;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.app.TTFManager;

import org.geometerplus.zlibrary.core.util.ZLTTFInfoDetector;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class FontsPopup extends PopupWindow {
    public FontAdapter fonts;
    public View fontsFrame;
    public RecyclerView fontsList;
    TextView fontsText;
    public View fontsize_popup;
    public TextView fontsizepopup_text;
    public SeekBar fontsizepopup_seek;
    public View fontsizepopup_minus;
    public View fontsizepopup_plus;
    public CheckBox ignore_embedded_fonts;
    TTFManager ttf;

    public static class TTCFile extends File {
        public int index;

        public TTCFile(@NonNull String pathname) {
            super(pathname);
        }

        public TTCFile(File f, int i) {
            super(f.getAbsolutePath());
            index = i;
        }
    }

    public static class FontView {
        public String name;
        public Typeface font;
        public File file;
        public int index; // ttc index

        public FontView(String name, File f) {
            this.name = name;
            this.file = f;
            this.font = Typeface.createFromFile(file);
        }

        @TargetApi(26)
        public FontView(String name, File f, int index) {
            this.name = name;
            this.file = f;
            this.index = index;
            this.font = new Typeface.Builder(file).setTtcIndex(index).build();
        }

        public FontView(String name) {
            this.name = name;
            this.font = Typeface.create(name, Typeface.NORMAL);
        }
    }

    public static class FontHolder extends RecyclerView.ViewHolder {
        public CheckedTextView tv;

        public FontHolder(View itemView) {
            super(itemView);
            tv = (CheckedTextView) itemView.findViewById(android.R.id.text1);
        }
    }

    public static class FontAdapter extends RecyclerView.Adapter<FontHolder> {
        Context context;
        public ArrayList<FontView> ff = new ArrayList<>();
        public int selected;
        public AdapterView.OnItemClickListener clickListener;
        public static ArrayList<String> DEFAULT = new ArrayList<>(Arrays.asList("sans-serif", "serif", "monospace"));

        public FontAdapter(Context context) {
            this.context = context;
        }

        public void addBasics() {
            for (String s : DEFAULT)
                add(s);
        }

        public void loadTTF(HashMap<File, String> hh) {
            addBasics();
            for (File k : hh.keySet()) {
                String v = hh.get(k);
                ff.add(new FontView(v, k));
            }
        }

        public void sort() {
            Collections.sort(ff, new Comparator<FontView>() {
                @Override
                public int compare(FontView o1, FontView o2) {
                    if (DEFAULT.contains(o1.name) && DEFAULT.contains(o2.name)) {
                        int i1 = DEFAULT.indexOf(o1.name);
                        int i2 = DEFAULT.indexOf(o2.name);
                        return Integer.compare(i1, i2);
                    }
                    for (String s : DEFAULT) {
                        if (o1.name.equals(s))
                            return -1;
                        if (o2.name.equals(s))
                            return 1;
                    }
                    int r = o1.name.compareTo(o2.name);
                    if (r != 0)
                        return r;
                    r = o1.file.compareTo(o2.file);
                    if (r != 0)
                        return r;
                    return Integer.compare(o1.index, o2.index);
                }
            });
            notifyDataSetChanged();
        }

        public void select(String f) {
            for (int i = 0; i < ff.size(); i++) {
                if (ff.get(i).name.equals(f))
                    selected = i;
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
        public int getItemCount() {
            return ff.size();
        }

        @Override
        public FontHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(android.R.layout.select_dialog_singlechoice, parent, false);
            return new FontHolder(view);
        }

        @Override
        public void onBindViewHolder(final FontHolder holder, int position) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onItemClick(null, null, holder.getAdapterPosition(), -1);
                }
            });
            holder.tv.setChecked(selected == position);
            holder.tv.setTypeface(ff.get(position).font);
            holder.tv.setText(ff.get(position).name);
        }
    }

    public FontsPopup(TTFManager ttf) {
        Context context = ttf.context;
        this.ttf = ttf;
        fontsize_popup = LayoutInflater.from(context).inflate(R.layout.font_popup, new FrameLayout(context), false);
        fontsizepopup_text = (TextView) fontsize_popup.findViewById(R.id.fontsize_text);
        fontsizepopup_plus = fontsize_popup.findViewById(R.id.fontsize_plus);
        fontsizepopup_minus = fontsize_popup.findViewById(R.id.fontsize_minus);
        fontsizepopup_seek = (SeekBar) fontsize_popup.findViewById(R.id.fontsize_seek);
        fonts = new FontAdapter(context);
        fonts.clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                fonts.select(position);
                setFont(fonts.ff.get(position).name);
            }
        };
        fontsFrame = fontsize_popup.findViewById(R.id.fonts_frame);
        fontsText = (TextView) fontsize_popup.findViewById(R.id.fonts_text);
        fontsText.setText(context.getString(R.string.add_more_fonts_to, TTFManager.USER_FONTS.toString()));
        if (Build.VERSION.SDK_INT >= 30) {
            fontsText.setVisibility(View.GONE); // unless device rooted you can't add files to side apps even using adb
        } else if (Build.VERSION.SDK_INT >= 21) {
            if (!Storage.permitted(context, Storage.PERMISSIONS_RO))
                fontsText.setText(context.getString(R.string.add_more_fonts_to, ttf.appFonts.toString()));
        }
        fontsList = (RecyclerView) fontsize_popup.findViewById(R.id.fonts_list);
        fontsList.setLayoutManager(new LinearLayoutManager(context));

        ignore_embedded_fonts = (CheckBox) fontsize_popup.findViewById(R.id.ignore_embedded_fonts);
        ignore_embedded_fonts.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        setIgnoreEmbeddedFonts(isChecked);
                    }
                }
        );
        setContentView(fontsize_popup);
    }

    public void setFont(String str) {
    }

    public void setFontsize(int f) {
    }

    public void setIgnoreEmbeddedFonts(boolean f) {
    }

    public void updateFontsize(int f) {
    }

    public void loadFonts() {
        fontsFrame.setVisibility(View.VISIBLE);
        fontsList.setAdapter(fonts);
        fonts.addBasics();
        for (String s : AndroidFontUtil.ourFontFileMap.keySet()) {
            File[] ff = AndroidFontUtil.ourFontFileMap.get(s);
            for (File f : ff) {
                if (f instanceof TTCFile) {
                    fonts.ff.add(new FontView(s, f, ((TTCFile) f).index));
                    break; // regular first
                } else if (f != null) {
                    fonts.ff.add(new FontView(s, f));
                    break; // regular first
                }
            }
        }
        fonts.sort();
    }

    public void updateFontsize(final int start, final int end, int f) {
        fontsizepopup_seek.setMax(end - start);
        fontsizepopup_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateFontsize(progress + start);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int p = fontsizepopup_seek.getProgress();
                setFontsize(start + p);
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
                setFontsize(start + p);
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
                setFontsize(start + p);
            }
        });
    }
}
