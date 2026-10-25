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
 * Packages in PASSTHROUGH are allowed to handle Home themselves (e.g. Google TV launcher
 * so the user can navigate within it after using the GTV shortcut tile).
 */
public class HomeKeyService extends AccessibilityService {

    private static final String GOOGLE_TV_HOME = "com.google.android.apps.tv.launcherx";

    /** Packages where Home should pass through instead of redirecting to Hume. */
    private static final String[] PASSTHROUGH = {
        GOOGLE_TV_HOME,
    };

    /** Package of the window most recently brought to the foreground (via accessibility events). */
    private String currentPkg = "";

    @Override public boolean onKeyEvent(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (e.getAction() == KeyEvent.ACTION_UP && e.getRepeatCount() == 0) {
            if (MainActivity.resumed) {
                // Already in Hume: handle in-app (close overlays, go to top if nothing open).
                MainActivity inst = MainActivity.instance;
                if (inst != null) inst.handleHomeKey();
            } else if (!isPassthrough(currentPkg)) {
                openHume();
            }
        }
        return true;
    }

    private boolean isPassthrough(String pkg) {
        for (String p : PASSTHROUGH) if (p.equals(pkg)) return true;
        return false;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent ev) {
        if (ev.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = ev.getPackageName();
            if (pkg != null) currentPkg = pkg.toString();
        }
    }

    @Override public void onInterrupt() { }

    private void openHume() {
        try {
            // FLAG_ACTIVITY_SINGLE_TOP: reuse the existing MainActivity instance (calls
            // onNewIntent → onResume) rather than clearing/resetting the task, which would
            // lose the focused tile and scroll position.
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setClass(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (Exception e) {
            android.util.Log.e("HomeKeyService", "failed to open MainActivity: " + e);
        }
    }
}
