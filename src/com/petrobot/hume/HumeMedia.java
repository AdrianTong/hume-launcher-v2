package com.petrobot.hume;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Trending Netflix titles for the continue watching carousel. Netflix doesn't publish to Android's
 * Watch Next channel, so we ask JustWatch's public GraphQL API (no key required; unofficial, may
 * change) what's trending on Netflix in the TV's country. Results cached 6 hours.
 */
final class HumeMedia {

    private static final String JW_URL = "https://apis.justwatch.com/graphql";
    private static final String JW_IMG = "https://images.justwatch.com";
    private static final long FRESH_MS = 6 * 3600_000L;
    private static final LruCache<String, Bitmap> POSTERS = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };

    private HumeMedia() { }

    /** The TV's country from its locale setting; US if unknown. */
    static String country() {
        String c = Locale.getDefault().getCountry();
        return c != null && c.matches("[A-Z]{2}") ? c : "US";
    }

    /**
     * Trending Netflix titles for the TV's country (shows and movies alternating). India gets a
     * mix of Hindi and English titles; everywhere else gets the local trending list.
     */
    static List<Suggestions.Item> netflix(Context c) {
        String cc = country();
        return cached(c, "jwNetflix_" + cc, () -> {
            List<List<Suggestions.Item>> lists = new ArrayList<>();
            if (cc.equals("IN")) {
                lists.add(query(cc, "SHOW", "IN", 15));
                lists.add(query(cc, "MOVIE", "IN", 15));
                lists.add(query(cc, "SHOW", "US\",\"GB", 10));
            } else {
                lists.add(query(cc, "SHOW", null, 15));
                lists.add(query(cc, "MOVIE", null, 15));
            }
            List<Suggestions.Item> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (int i = 0; out.size() < 12; i++) {
                boolean any = false;
                for (List<Suggestions.Item> l : lists) {
                    if (i < l.size()) {
                        any = true;
                        if (seen.add(l.get(i).title) && out.size() < 12) out.add(l.get(i));
                    }
                }
                if (!any) break;
            }
            return out;
        });
    }

    private interface Fetch { List<Suggestions.Item> run() throws Exception; }

    private static List<Suggestions.Item> cached(Context c, String name, Fetch fetch) {
        SharedPreferences p = c.getSharedPreferences("hume_media", Context.MODE_PRIVATE);
        long at = p.getLong(name + "At", 0);
        if (System.currentTimeMillis() - at < FRESH_MS) {
            List<Suggestions.Item> keep = fromPrefs(p.getString(name, "[]"));
            if (!keep.isEmpty()) return keep;
        }
        try {
            List<Suggestions.Item> fresh = fetch.run();
            if (!fresh.isEmpty()) {
                p.edit().putString(name, toPrefs(fresh)).putLong(name + "At", System.currentTimeMillis()).apply();
                return fresh;
            }
        } catch (Exception e) {
            android.util.Log.w("HumeMedia", "fetch failed for " + name + ": " + e);
        }
        return fromPrefs(p.getString(name, "[]"));
    }

    private static List<Suggestions.Item> query(String cc, String type, String made, int count) throws Exception {
        String filter = "objectTypes: [" + type + "], packages: [\"nfx\"]"
                + (made != null ? ", productionCountries: [\"" + made + "\"]" : "");
        String q = "{ popularTitles(country: " + cc + ", first: " + count + ", sortBy: TRENDING, filter: {" + filter
                + "}) { edges { node { content(country: " + cc + ", language: \"en\") { title originalTitle posterUrl }"
                + " offers(country: " + cc + ", platform: WEB) { monetizationType package { shortName } } } } } }";
        String body = post(JW_URL, new JSONObject().put("query", q).toString());
        JSONArray edges = new JSONObject(body).getJSONObject("data").getJSONObject("popularTitles").getJSONArray("edges");
        List<Suggestions.Item> out = new ArrayList<>();
        boolean indian = "IN".equals(made);
        for (int i = 0; i < edges.length(); i++) {
            JSONObject node = edges.getJSONObject(i).getJSONObject("node");
            JSONObject content = node.getJSONObject("content");
            String title = content.optString("title", "");
            String poster = content.optString("posterUrl", "");
            if (title.isEmpty() || poster.isEmpty()) continue;
            if (indian && !hindiOrLatin(content.optString("originalTitle", title))) continue;
            if (!hasNetflix(node.optJSONArray("offers"))) continue;
            Suggestions.Item s = new Suggestions.Item();
            s.type = "netflix";
            s.title = title;
            s.overview = "Netflix";
            s.backdrop = JW_IMG + poster.replace("{profile}", "s332").replace("{format}", "jpg");
            s.pkg = "com.netflix.ninja";
            s.key = "netflix|" + title;
            s.progress = -1f;
            s.link = "";
            out.add(s);
        }
        return out;
    }

    private static boolean hindiOrLatin(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 0x0980 && ch <= 0x0DFF) return false;
        }
        return true;
    }

    private static boolean hasNetflix(JSONArray offers) {
        if (offers == null) return false;
        for (int i = 0; i < offers.length(); i++) {
            JSONObject o = offers.optJSONObject(i);
            if (o == null) continue;
            String type = o.optString("monetizationType");
            if (!"FLATRATE".equals(type) && !"FREE".equals(type) && !"ADS".equals(type)) continue;
            JSONObject p = o.optJSONObject("package");
            if (p != null && "nfx".equals(p.optString("shortName"))) return true;
        }
        return false;
    }

    /** Google TV's global search for a title — opens the title's page across installed apps. */
    static Intent searchIntent(String title) {
        return new Intent("android.search.action.GLOBAL_SEARCH")
                .putExtra(SearchManager.QUERY, title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /** Poster bitmap from memory, disk cache, or network. Must be called off the main thread. */
    static Bitmap poster(Context c, String url) {
        if (url == null || url.isEmpty()) return null;
        Bitmap b = POSTERS.get(url);
        if (b != null) return b;
        if (url.startsWith("content://") || url.startsWith("android.resource://")) {
            try (InputStream in = c.getContentResolver().openInputStream(Uri.parse(url))) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                o.inSampleSize = 2;
                b = BitmapFactory.decodeStream(in, null, o);
                if (b != null) POSTERS.put(url, b);
                return b;
            } catch (Exception | OutOfMemoryError e) {
                android.util.Log.w("HumeMedia", "poster content URI load failed: " + e);
                return null;
            }
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) return null;
        File dir = new File(c.getFilesDir(), "nf_posters");
        File f = new File(dir, Integer.toHexString(url.hashCode()) + ".jpg");
        try {
            if (!f.exists()) {
                byte[] data = getBytes(url);
                if (data == null) return null;
                if (!dir.exists()) dir.mkdirs();
                try (FileOutputStream out = new FileOutputStream(f)) { out.write(data); }
                prune(dir);
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            b = BitmapFactory.decodeFile(f.getPath(), o);
            if (b != null) POSTERS.put(url, b);
            return b;
        } catch (Exception | OutOfMemoryError e) {
            if (f.exists()) f.delete();  // remove any partial write
            android.util.Log.w("HumeMedia", "poster network load failed for " + url + ": " + e);
            return null;
        }
    }

    private static void prune(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length <= 80) return;
        Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (int i = 0; i < files.length - 80; i++) files[i].delete();
    }

    private static String toPrefs(List<Suggestions.Item> items) {
        JSONArray a = new JSONArray();
        try { for (Suggestions.Item i : items) a.put(new JSONObject().put("t", i.title).put("b", i.backdrop)); }
        catch (Exception e) { android.util.Log.w("HumeMedia", "toPrefs serialisation failed: " + e); }
        return a.toString();
    }

    private static List<Suggestions.Item> fromPrefs(String json) {
        List<Suggestions.Item> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Suggestions.Item s = new Suggestions.Item();
                s.type = "netflix";
                s.title = o.getString("t");
                s.overview = "Netflix";
                s.backdrop = o.optString("b", "");
                s.pkg = "com.netflix.ninja";
                s.key = "netflix|" + s.title;
                s.progress = -1f;
                s.link = "";
                out.add(s);
            }
        } catch (Exception e) { android.util.Log.w("HumeMedia", "fromPrefs parse failed: " + e); }
        return out;
    }

    private static String post(String url, String json) throws Exception {
        byte[] b = request(url, json);
        if (b == null) throw new Exception("no data");
        return new String(b, StandardCharsets.UTF_8);
    }

    private static byte[] getBytes(String url) { return request(url, null); }

    private static byte[] request(String url, String json) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", "Hume-TV-launcher");
            if (json != null) {
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                try (java.io.OutputStream o = c.getOutputStream()) { o.write(json.getBytes(StandardCharsets.UTF_8)); }
            }
            int code = c.getResponseCode();
            if (code != 200) {
                android.util.Log.w("HumeMedia", "HTTP " + code + " from " + url);
                return null;
            }
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[16384];
                int n, total = 0;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > 4 * 1024 * 1024) return null;
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        } catch (Exception e) {
            android.util.Log.w("HumeMedia", "request to " + url + " failed: " + e);
            return null;
        } finally { if (c != null) c.disconnect(); }
    }
}
