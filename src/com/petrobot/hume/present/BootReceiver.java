package com.petrobot.hume.present;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * After boot: starts the admin server if Presentation is enabled, and opens the presentation
 * full-screen when "Start on boot" is on (port of BootReceiver.kt). The launcher's Home screen
 * also checks this on first start, since a HOME app may come up before this broadcast.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        boolean boot = Intent.ACTION_BOOT_COMPLETED.equals(a);
        boolean wake = "android.intent.action.DREAMING_STOPPED".equals(a)
                || Intent.ACTION_SCREEN_ON.equals(a)
                || "android.intent.action.QUICKBOOT_POWERON".equals(a)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(a);
        if (!boot && !wake && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) return;
        if (wake) {
            try {
                Intent home = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .setClassName("com.petrobot.hume", "com.petrobot.hume.MainActivity")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                c.startActivity(home);
            } catch (Exception ignored) { }
            return;
        }
        Presenter.ensureRunning(c);
        if (Intent.ACTION_BOOT_COMPLETED.equals(a)) {
            Presenter.openOnBootOnce(c);
            // Launch Hume immediately on boot so it appears before Google TV grabs the home slot.
            // Google TV is a system app with higher home priority, so this is the only reliable
            // way to ensure Hume is the first thing the user sees after the TV powers on.
            try {
                Intent home = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .setClassName("com.petrobot.hume", "com.petrobot.hume.MainActivity")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                c.startActivity(home);
            } catch (Exception ignored) { }
        }
    }
}
