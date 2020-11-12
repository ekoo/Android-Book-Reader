package com.github.axet.bookreader.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.github.axet.bookreader.R;
import com.github.axet.bookreader.app.TTS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TTSPreferenceCompat extends ListPreference {
    CharSequence defSummary;
    ArrayList<CharSequence> text;
    ArrayList<CharSequence> value;

    @TargetApi(11)
    public static HashSet<Locale> getInputLanguages(Context context) {
        HashSet<Locale> list = new HashSet<>();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> ims = imm.getEnabledInputMethodList();
        for (InputMethodInfo method : ims) {
            List<InputMethodSubtype> submethods = imm.getEnabledInputMethodSubtypeList(method, true);
            for (InputMethodSubtype submethod : submethods) {
                if (submethod.getMode().equals("keyboard")) {
                    String currentLocale = submethod.getLocale();
                    Locale locale = new Locale(currentLocale);
                    list.add(locale);
                }
            }
        }
        return list;
    }

    public TTSPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create();
    }

    public TTSPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        create();
    }

    public TTSPreferenceCompat(Context context) {
        super(context);
        create();
    }

    public void create() {
        defSummary = getSummary();
        TTS tts = new TTS(getContext());
        text = new ArrayList<>();
        value = new ArrayList<>();
        text.add(getContext().getString(R.string.tts_default));
        value.add("");
        Set<Locale> ll = null;
        if (Build.VERSION.SDK_INT >= 21)
            ll = tts.tts.getAvailableLanguages();
        if (ll == null) {
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                LocaleList lll = LocaleList.getDefault();
                if (lll != null) {
                    ll = new HashSet<>();
                    for (int i = 0; i < lll.size(); i++)
                        ll.add(lll.get(i));
                }
            } else {
                ll = getInputLanguages(getContext());
            }
        }
        if (ll == null) {
            ll = new HashSet<>();
            ll.add(Locale.US);
        }
        for (Locale l : ll) {
            text.add(l.toString());
            value.add(l.toString());
        }
        setEntries(text.toArray(new CharSequence[0]));
        setEntryValues(value.toArray(new CharSequence[0]));
        setDefaultValue("");
        tts.close();
    }

    @Override
    public void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
        setSummary(getEntry());
    }

    @Override
    public void onClick() {
        super.onClick();
    }

    @Override
    protected void notifyChanged() {
        super.notifyChanged();
        setSummary(getEntry());
    }
}
