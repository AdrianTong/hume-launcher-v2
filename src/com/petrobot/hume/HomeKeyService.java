package com.petrobot.hume;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Google TV sends the Home key directly to its own launcher, ignoring the default home app.
 * This service catches Home from the remote and opens Hume, and also opens Hume whenever
 * Google TV's home screen appears some other way. Enable it via Settings -> Permissions ->
 * Home button override -> Open Accessibility Settings.
 *
 * TCL remotes mark ordinary Home presses as long presses, so every press redirects to Hume.
 */
public class HomeKeyService extends AccessibilityService {

    private static final String GOOGLE_TV_HOME = "com.google.android.apps.tv.launcherx";

    @Override public boolean onKeyEvent(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        // Only act on ACTION_UP; return true on both to consume the full press cycle.
        // Note: onKeyEvent intercepts Home from every app globally (packageNames in the XML
        // only filters accessibility events, not key events). This is intentional for Google TV.
        if (e.getAction() == KeyEvent.ACTION_UP && e.getRepeatCount() == 0) openHume();
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent ev) {
        if (ev.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && GOOGLE_TV_HOME.contentEquals(String.valueOf(ev.getPackageName()))
                && String.valueOf(ev.getClassName()).endsWith(".HomeActivity")) {
            openHume();
        }
    }

    @Override public void onInterrupt() { }

    private void openHume() {
        try {
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setClass(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } catch (Exception e) {
            android.util.Log.e("HomeKeyService", "failed to open MainActivity: " + e);
        }
    }
}
