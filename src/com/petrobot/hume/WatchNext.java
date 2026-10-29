package com.petrobot.hume;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Other apps' "Continue watching" (Watch Next) and channel recommendations from the system TV
 * provider — the same data Google TV and Haven show. Needs READ_TV_LISTINGS (grant via adb:
 * adb shell pm grant com.petrobot.hume android.permission.READ_TV_LISTINGS). Queries may not
 * pass a selection or sort, so filtering happens here.
 */
final class WatchNext {

    static final class Item {
        final String title, subtitle, image, pkg;
        final Intent intent;
        final float progress;  // 0..1, or -1 if unknown

        Item(String title, String subtitle, String image, String pkg, Intent intent, float progress) {
            this.title = title;
            this.subtitle = subtitle;
            this.image = image;
            this.pkg = pkg;
            this.intent = intent;
            this.progress = progress;
        }
    }

    private static final String TAG = "HumeWatchNext";
    private static final Set<String> EXCLUDED = new HashSet<>(java.util.Arrays.asList("com.spotify.tv.android"));
    private static final String[] COLS = {"package_name", "title", "episode_title", "poster_art_uri",
            "thumbnail_uri", "intent_uri", "last_engagement_time_utc_millis", "browsable",
            "last_playback_position_millis", "duration_millis"};

    private WatchNext() { }

    /** Continue watching (the Watch Next channel), most recently watched first. */
    static List<Item> load(Context c, int max) {
        List<Item> out = read(c, TvContract.WatchNextPrograms.CONTENT_URI, max, true);
        Log.i(TAG, "watch next rows: " + out.size());
        return out;
    }

    /**
     * Recommendations apps publish to their home-screen channels. Titles in {@code skip} are
     * excluded (already in Continue Watching). Takes a few from each app so one doesn't fill the row.
     */
    static List<Item> recommendations(Context c, int max, Set<String> skip) {
        List<Item> all = read(c, TvContract.PreviewPrograms.CONTENT_URI, 200, false);
        java.util.LinkedHashMap<String, List<Item>> byApp = new java.util.LinkedHashMap<>();
        for (Item i : all) {
            if (skip.contains(i.title) || EXCLUDED.contains(i.pkg)) continue;
            byApp.computeIfAbsent(i.pkg, k -> new ArrayList<>()).add(i);
        }
        List<Item> out = new ArrayList<>();
        for (int round = 0; out.size() < max; round++) {
            boolean any = false;
            for (List<Item> l : byApp.values()) {
                if (round < l.size() && out.size() < max) { out.add(l.get(round)); any = true; }
            }
            if (!any) break;
        }
        return out;
    }

    private static List<Item> read(Context c, Uri uri, int max, boolean sortByEngagement) {
        List<Object[]> rows = new ArrayList<>();
        try (Cursor cur = c.getContentResolver().query(uri, null, null, null, null)) {
            if (cur == null) return new ArrayList<>();
            int[] ix = new int[COLS.length];
            for (int i = 0; i < COLS.length; i++) ix[i] = cur.getColumnIndex(COLS[i]);
            while (cur.moveToNext()) {
                Object[] r = new Object[COLS.length];
                for (int i = 0; i < COLS.length; i++) {
                    if (ix[i] < 0) continue;
                    r[i] = (i == 6 || i == 7 || i == 8 || i == 9) ? (Object) cur.getLong(ix[i]) : cur.getString(ix[i]);
                }
                rows.add(r);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "READ_TV_LISTINGS permission denied — grant with: " +
                    "adb shell pm grant com.petrobot.hume android.permission.READ_TV_LISTINGS", e);
            return new ArrayList<>();
        } catch (Exception e) {
            Log.w(TAG, "TvContract query failed", e);
            return new ArrayList<>();
        }
        if (sortByEngagement) rows.sort((a, b) -> Long.compare(num(b[6]), num(a[6])));
        List<Item> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Object[] r : rows) {
            if (out.size() >= max) break;
            String pkg = (String) r[0], title = (String) r[1], intentUri = (String) r[5];
            if (title == null || intentUri == null || r[7] != null && num(r[7]) == 0) continue;
            if (!seen.add(pkg + "|" + title)) continue;
            Intent intent;
            try {
                intent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            } catch (Exception e) {
                Log.w(TAG, "bad intent URI from " + pkg + ": " + e);
                continue;
            }
            String poster = r[3] != null ? (String) r[3] : (String) r[4];
            long pos = r[8] instanceof Long ? (Long) r[8] : -1;
            long dur = r[9] instanceof Long ? (Long) r[9] : 0;
            float prog = dur > 0 && pos >= 0 ? Math.min(1f, pos / (float) dur) : -1f;
            out.add(new Item(title, r[2] != null ? (String) r[2] : "", poster, pkg, intent, prog));
        }
        return out;
    }

    private static long num(Object o) { return o instanceof Long ? (Long) o : 0; }

    /** Convert a Watch Next item to a Suggestions.Item for the Hume carousel. */
    static Suggestions.Item toSuggestion(Item wn) {
        Suggestions.Item s = new Suggestions.Item();
        s.type = "watchnext";
        s.title = wn.title;
        s.overview = wn.subtitle;
        s.backdrop = wn.image != null ? wn.image : "";
        s.pkg = wn.pkg != null ? wn.pkg : "";
        s.key = s.pkg + "|" + s.title;
        s.progress = wn.progress;
        s.link = wn.intent != null ? wn.intent.toUri(Intent.URI_INTENT_SCHEME) : "";
        return s;
    }
}
