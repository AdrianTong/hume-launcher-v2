package com.petrobot.hume;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.SystemClock;
import android.view.KeyEvent;

/**
 * Navigation sounds. Two styles: Haven uses a soft SoundPool tick on d-pad presses; Hume uses
 * Android TV's own system sounds (per-direction focus sound + key click on select/back) at 60%
 * of the normal sound-effect level. Settings -> Home -> Navigation sounds.
 */
final class Clicks {
    static final float VOLUME = 0.6f;
    static final String STYLE_HAVEN = "haven";
    static final String STYLE_HUME = "hume";
    private static AudioManager audio;
    private static SoundPool tickPool;
    // volatile: tickId and tickLoaded are written from the SoundPool callback thread, read from main.
    private static volatile int tickId = -1;
    private static volatile boolean tickLoaded;
    private static long lastTickAt;

    private Clicks() {
    }

    /** Initialises AudioManager and the tick SoundPool in one call. Safe to call multiple times. */
    static void initTick(Context c) {
        if (audio == null) audio = c.getApplicationContext().getSystemService(AudioManager.class);
        if (tickPool != null) return;
        tickPool = new SoundPool.Builder().setMaxStreams(2).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        tickPool.setOnLoadCompleteListener((pool, id, status) -> {
            if (status != 0) android.util.Log.e("Clicks", "tick sample load error, status=" + status);
            tickLoaded = status == 0;
        });
        int loaded = tickPool.load(c.getApplicationContext(), R.raw.tick, 1);
        if (loaded == 0) android.util.Log.e("Clicks", "SoundPool.load failed for R.raw.tick");
        else tickId = loaded;
    }

    /** Haven-style tick, debounced to 80ms. No-op if the sample hasn't loaded yet. */
    static void tick(Context c) {
        if (tickPool == null) initTick(c);
        if (!tickLoaded || tickId == -1) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastTickAt < 80) return;
        lastTickAt = now;
        tickPool.play(tickId, 0.35f, 0.35f, 0, 0, 1f);
    }

    static boolean enabled(Context c) {
        return c.getSharedPreferences("sounds", Context.MODE_PRIVATE).getBoolean("nav_sounds", true);
    }

    static void setEnabled(Context c, boolean on) {
        c.getSharedPreferences("sounds", Context.MODE_PRIVATE).edit().putBoolean("nav_sounds", on).apply();
    }

    static String style(Context c) {
        return c.getSharedPreferences("sounds", Context.MODE_PRIVATE).getString("nav_style", STYLE_HAVEN);
    }

    static void setStyle(Context c, String style) {
        c.getSharedPreferences("sounds", Context.MODE_PRIVATE).edit().putString("nav_style", style).apply();
    }

    static void init(Context c) {
        initTick(c);  // ensures both AudioManager and SoundPool are ready
    }

    private static void play(Context c, int effect) {
        init(c);
        if (audio != null && enabled(c)) audio.playSoundEffect(effect, VOLUME);
    }

    /** Focus moved: Android's focus sound for that direction. */
    static void move(Context c, int keyCode) {
        int fx;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: fx = AudioManager.FX_FOCUS_NAVIGATION_UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: fx = AudioManager.FX_FOCUS_NAVIGATION_DOWN; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: fx = AudioManager.FX_FOCUS_NAVIGATION_LEFT; break;
            default: fx = AudioManager.FX_FOCUS_NAVIGATION_RIGHT; break;
        }
        play(c, fx);
    }

    /** Something was chosen. */
    static void select(Context c) {
        play(c, AudioManager.FX_KEY_CLICK);
    }

    /** Back. */
    static void back(Context c) {
        play(c, AudioManager.FX_KEY_CLICK);
    }
}
