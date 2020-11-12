package com.github.axet.bookreader.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.format.DateFormat;
import android.util.AndroidRuntimeException;
import android.util.Log;

import com.github.axet.androidlibrary.app.AlarmManager;
import com.github.axet.androidlibrary.preferences.OptimizationPreferenceCompat;
import com.github.axet.androidlibrary.sound.Sound;
import com.github.axet.androidlibrary.widgets.Toast;
import com.github.axet.bookreader.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class TTS extends Sound {
    public static final String TAG = TTS.class.getSimpleName();

    public static final int DELAYED_DELAY = 5 * AlarmManager.SEC1;
    public static final String TTS_INIT = TTS.class.getCanonicalName() + ".TTS_INIT";

    public static int SOUND_TYPE = AudioManager.STREAM_NOTIFICATION;
    public static int SOUND_USAGE = AudioAttributes.USAGE_NOTIFICATION;
    public static int SOUND_CONTENTTYPE = AudioAttributes.CONTENT_TYPE_SONIFICATION;

    public TextToSpeech tts;
    Runnable delayed; // delayedSpeach. tts may not be initalized, on init done, run delayed.run()
    int restart; // restart tts once if failed. on apk upgrade tts always failed.
    public Set<Runnable> dones = new HashSet<>(); // valid done list, in case sound was canceled during play done will not be present
    Runnable onInit; // once
    Set<Runnable> exits = new HashSet<>(); // run when all done

    public TTS(Context context) {
        super(context);
        ttsCreate();
    }

    void ttsCreate() {
        Log.d(TAG, "tts create");
        handler.removeCallbacks(onInit);
        onInit = new Runnable() {
            @Override
            public void run() {
                onInit();
            }
        };
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(final int status) {
                if (status != TextToSpeech.SUCCESS)
                    return;
                handler.post(onInit);
            }
        });
    }

    public void onInit() {
        if (Build.VERSION.SDK_INT >= 21) {
            tts.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(SOUND_USAGE)
                    .setContentType(SOUND_CONTENTTYPE)
                    .build());
        }

        handler.removeCallbacks(onInit);
        onInit = null;

        done(delayed);
        handler.removeCallbacks(delayed);
        delayed = null;
    }

    public void close() {
        closeTTS();
    }

    public void closeTTS() {
        Log.d(TAG, "closeTTS()");
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        handler.removeCallbacks(onInit);
        onInit = null;
        dones.remove(delayed);
        handler.removeCallbacks(delayed);
        delayed = null;
    }

    public void playSpeech(final String speak, final Runnable done) {
        dones.add(done);

        dones.remove(delayed);
        handler.removeCallbacks(delayed);
        delayed = null;

        if (tts == null)
            ttsCreate();

        // clear delayed(), sound just played
        final Runnable clear = new Runnable() {
            @Override
            public void run() {
                dones.remove(delayed);
                handler.removeCallbacks(delayed);
                delayed = null;
                done(done);
            }
        };

        if (Build.VERSION.SDK_INT < 15) {
            tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String s) {
                    handler.post(clear);
                }
            });
        } else {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) { // tts start speaking
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            dones.remove(delayed);
                            handler.removeCallbacks(delayed);
                            delayed = null;
                        }
                    });
                }

                public void onRangeStart(final String utteranceId, final int start, final int end, final int frame) { // API26
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TTS.this.onRangeStart(utteranceId, start, end, frame);
                        }
                    });
                }

                @Override
                public void onDone(String utteranceId) {
                    handler.post(clear);
                }

                @Override
                public void onError(String utteranceId) {
                    handler.post(clear);
                }
            });
        }

        // TTS may say failed, but play sounds successfully. we need regardless or failed do not
        // play speech twice if clear.run() was called.
        if (!playSpeech(speak)) {
            Log.d(TAG, "Waiting for TTS");
            Toast.makeText(context, "WaitTTS", Toast.LENGTH_SHORT).show();
            dones.remove(delayed);
            handler.removeCallbacks(delayed);
            delayed = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "delayed run()");
                    if (!playSpeech(speak)) {
                        closeTTS();
                        if (restart >= 1) {
                            Log.d(TAG, "Failed TTS again, skipping");
                            Toast.makeText(context, "FailedTTS", Toast.LENGTH_SHORT).show();
                            clear.run();
                        } else {
                            Log.d(TAG, "Failed TTS again, restarting");
                            restart++;
                            Toast.makeText(context, "FailedTTSRestar", Toast.LENGTH_SHORT).show();
                            dones.remove(delayed);
                            handler.removeCallbacks(delayed);
                            delayed = new Runnable() {
                                @Override
                                public void run() {
                                    playSpeech(speak, done);
                                }
                            };
                            dones.add(delayed);
                            handler.postDelayed(delayed, DELAYED_DELAY);
                        }
                    }
                }
            };
            dones.add(delayed);
            handler.postDelayed(delayed, DELAYED_DELAY);
        }
    }

    public Locale getUserLocale() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        String lang = shared.getString(BookApplication.PREFERENCE_LANGUAGE, ""); // take user lang preferences

        Locale locale;

        if (lang.isEmpty()) // use system locale (system language)
            locale = Locale.getDefault();
        else
            locale = new Locale(lang);

        return locale;
    }

    public Locale getTTSLocale() {
        Locale locale = getUserLocale();

        if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED) {
            String lang = locale.getLanguage();
            locale = new Locale(lang);
        }

        if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED) { // user selection not supported.
            locale = null;
            if (Build.VERSION.SDK_INT >= 21) {
                Voice v = tts.getDefaultVoice();
                if (v != null)
                    locale = v.getLocale();
            }
            if (locale == null) {
                if (Build.VERSION.SDK_INT >= 18)
                    locale = tts.getDefaultLanguage();
                else
                    locale = tts.getLanguage();
            }
            if (locale == null)
                locale = Locale.getDefault();
            if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED) {
                String lang = locale.getLanguage(); // default tts voice not supported. use 'lang' "ru" of "ru_RU"
                locale = new Locale(lang);
            }
            if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED) {
                locale = Locale.getDefault(); // default 'lang' tts voice not supported. use 'system default lang'
                String lang = locale.getLanguage();
                locale = new Locale(lang);
            }
            if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED)
                locale = new Locale("en"); // 'system default lang' tts voice not supported. use 'en'
            if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED)
                return null; // 'en' not supported? do not speak
        }

        if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_MISSING_DATA) {
            try {
                Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (OptimizationPreferenceCompat.isCallable(context, intent)) {
                    context.startActivity(intent);
                }
            } catch (AndroidRuntimeException e) {
                Log.d(TAG, "Unable to load TTS", e);
                try {
                    Intent intent = new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (OptimizationPreferenceCompat.isCallable(context, intent))
                        context.startActivity(intent);
                } catch (AndroidRuntimeException e1) {
                    Log.d(TAG, "Unable to load TTS", e1);
                }
            }
            return null;
        }

        return locale;
    }

    public boolean playSpeech(String speak) {
        if (onInit != null)
            return false;

        Locale locale = getTTSLocale();

        if (locale == null)
            return false;

        return playSpeech(locale, speak);
    }

    public boolean playSpeech(Locale locale, String speak) {
        tts.setLanguage(locale);
        if (Build.VERSION.SDK_INT >= 21) {
            Bundle params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, SOUND_TYPE);
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, getVolume());
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DONE");
            if (tts.speak(speak, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString()) != TextToSpeech.SUCCESS)
                return false;
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(SOUND_TYPE));
            params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, Float.toString(getVolume()));
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DONE");
            if (tts.speak(speak, TextToSpeech.QUEUE_FLUSH, params) != TextToSpeech.SUCCESS)
                return false;
        }
        restart = 0;
        return true;
    }

    public float getVolume() {
        return 1f;
    }

    public void done(Runnable done) {
        if (done != null && dones.contains(done)) {
            dones.remove(done);
            done.run();
        } else {
            dones.remove(done);
        }
        if (dones.isEmpty()) {
            for (Runnable r : exits)
                r.run();
            exits.clear();
        }
    }

    public void after(Runnable done) {
        if (dones.isEmpty())
            done.run();
        else
            exits.add(done);
    }

    public void onRangeStart(String utteranceId, int start, int end, int frame) {
    }
}
