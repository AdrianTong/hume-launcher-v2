package com.petrobot.hume;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Google TV sends the Home key directly to its own launcher, ignoring the default home app.
 * This service catches Home from the remote and opens Hume, and also redirects to Hume on
 * boot (before the user has interacted with Hume at all in this session).
 *
 * Boot redirect: Google TV is a system app that wins the home resolver race. We detect its
 * HomeActivity appearing while Hume hasn't been shown yet (firstBootLaunch=true) and redirect
 * immediately. Once Hume shows once, firstBootLaunch=false so the GTV shortcut tile works.
 *
 * Packages in PASSTHROUGH are allowed to handle Home themselves after firstBootLaunch is false.
 */
public class HomeKeyService extends AccessibilityService {

    private static final String GOOGLE_TV_HOME = "com.google.android.apps.tv.launcherx";

    /** Packages where Home should pass through instead of redirecting to Hume. */
    private static final String[] PASSTHROUGH = {
        GOOGLE_TV_HOME,
    };

    /** True until Hume has been shown at least once this session. Cleared in MainActivity.onResume(). */
    static boolean firstBootLaunch = true;

    /** Package of the window most recently brought to the foreground (via accessibility events). */
    private String currentPkg = "";

    @Override public boolean onKeyEvent(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (e.getAction() == KeyEvent.ACTION_UP && e.getRepeatCount() == 0) {
            if (MainActivity.resumed) {
                MainActivity inst = MainActivity.instance;
                if (inst != null) inst.handleHomeKey();
            } else if (!isPassthrough(currentPkg)) {
                openHume();
            }
        }
        return true;
    }

    private boolean isPassthrough(String pkg) {
        if (firstBootLaunch) return false;  // never passthrough before Hume has loaded once
        for (String p : PASSTHROUGH) if (p.equals(pkg)) return true;
        return false;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent ev) {
        if (ev.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = ev.getPackageName();
            if (pkg != null) currentPkg = pkg.toString();
            // On boot: if GTV's home appears before Hume has ever shown, redirect to Hume.
            // After Hume shows once (firstBootLaunch=false), GTV is allowed (shortcut tile).
            if (firstBootLaunch
                    && GOOGLE_TV_HOME.equals(currentPkg)
                    && String.valueOf(ev.getClassName()).endsWith("HomeActivity")
                    && !MainActivity.resumed) {
                openHume();
            }
        }
    }

    @Override public void onInterrupt() { }

    private void openHume() {
        try {
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setClass(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (Exception e) {
            android.util.Log.e("HomeKeyService", "failed to open MainActivity: " + e);
        }
    }
}
