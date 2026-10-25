package com.petrobot.hume;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 4K nature backdrops bundled as APK assets (assets/wallpapers/w01.jpg … w50.jpg).
 * Shuffled each session so the carousel always starts at a random photo.
 * Each photo carries a location label and photographer credit shown in hero mode.
 */
final class Nature {

    static final class Photo {
        final String asset;   // e.g. "wallpapers/w01.jpg"
        final String location;
        final String photographer;

        Photo(String asset, String location, String photographer) {
            this.asset = asset;
            this.location = location;
            this.photographer = photographer;
        }
    }

    private static final Photo[] PHOTOS = {
        new Photo("wallpapers/w01.jpg", "Zermatt, Switzerland", "Samuel Ferrara"),
        new Photo("wallpapers/w02.jpg", "Iceland Highlands", "Tobias Tullius"),
        new Photo("wallpapers/w03.jpg", "Olympic National Forest, Washington", "Sergei Akulich"),
        new Photo("wallpapers/w04.jpg", "Antelope Canyon, Arizona", "John Fowler"),
        new Photo("wallpapers/w05.jpg", "Tuscany, Italy", "Elijah Hiett"),
        new Photo("wallpapers/w06.jpg", "Lofoten Islands, Norway", "Simon Matzinger"),
        new Photo("wallpapers/w07.jpg", "Milky Way over the Alps", "Nathan Anderson"),
        new Photo("wallpapers/w08.jpg", "Redwood National Park, California", "Jeremy Bishop"),
        new Photo("wallpapers/w09.jpg", "Amazon Rainforest, Brazil", "Dave Hoefler"),
        new Photo("wallpapers/w10.jpg", "Great Smoky Mountains, Tennessee", "Lukasz Szmigiel"),
        new Photo("wallpapers/w11.jpg", "Yosemite Valley, California", "Arto Marttinen"),
        new Photo("wallpapers/w12.jpg", "Maldives", "Sean Oulashin"),
        new Photo("wallpapers/w13.jpg", "Dolomites, Italy", "Kalen Emsley"),
        new Photo("wallpapers/w14.jpg", "Patagonia, Argentina", "Luca Bravo"),
        new Photo("wallpapers/w15.jpg", "Rocky Mountains, Colorado", "Stephan Valentin"),
        new Photo("wallpapers/w16.jpg", "Black Forest, Germany", "Johannes Plenio"),
        new Photo("wallpapers/w17.jpg", "Hoh Rain Forest, Washington", "Wei Liang"),
        new Photo("wallpapers/w18.jpg", "Northern Lights, Iceland", "Jonatan Pie"),
        new Photo("wallpapers/w19.jpg", "Atacama Desert, Chile", "Aperture Vintage"),
        new Photo("wallpapers/w20.jpg", "Joshua Tree National Park", "Kristopher Roller"),
        new Photo("wallpapers/w21.jpg", "Lake Bled, Slovenia", "v2osk"),
        new Photo("wallpapers/w22.jpg", "Tongass National Forest, Alaska", "Johannes Plenio"),
        new Photo("wallpapers/w23.jpg", "Big Sur, California", "Sam Goodgame"),
        new Photo("wallpapers/w24.jpg", "Amalfi Coast, Italy", "Azzedine Rouichi"),
        new Photo("wallpapers/w25.jpg", "Bora Bora, French Polynesia", "Jeremy Bishop"),
        new Photo("wallpapers/w26.jpg", "New Zealand Alps", "Samuel Scrimshaw"),
        new Photo("wallpapers/w27.jpg", "Skógafoss, Iceland", "Adam Jang"),
        new Photo("wallpapers/w28.jpg", "Anna Ruby Falls, Georgia", "Hunter Newton"),
        new Photo("wallpapers/w29.jpg", "White Sands, New Mexico", "Elvis Bekmanis"),
        new Photo("wallpapers/w30.jpg", "Kluane National Park, Yukon", "Kalen Emsley"),
        new Photo("wallpapers/w31.jpg", "Misty Mountain Ridges", "Dmytro Koplyk"),
        new Photo("wallpapers/w32.jpg", "Lake Matheson, New Zealand", "Tanja Zöllner"),
        new Photo("wallpapers/w33.jpg", "Glencoe, Scotland", "Ryan Parker"),
        new Photo("wallpapers/w34.jpg", "Drôme, France", "Léonard Cotte"),
        new Photo("wallpapers/w35.jpg", "Mirissa Beach, Sri Lanka", "Zoshua Colah"),
        new Photo("wallpapers/w36.jpg", "Pacific Coast, California", "Uran Wang"),
        new Photo("wallpapers/w37.jpg", "Vestrahorn, Iceland", "Job Savelsberg"),
        new Photo("wallpapers/w38.jpg", "Grand Canyon, Arizona", "Megan Clark"),
        new Photo("wallpapers/w39.jpg", "Lake Wanaka, New Zealand", "Ken Cheung"),
        new Photo("wallpapers/w40.jpg", "Bjuröklubb, Sweden", "Niklas Jonasson"),
        new Photo("wallpapers/w41.jpg", "Queenstown, New Zealand", "Andreas Sjövall"),
        new Photo("wallpapers/w42.jpg", "Columbia River, Oregon", "Karsten Würth"),
        new Photo("wallpapers/w43.jpg", "Lake Tekapo, New Zealand", "Richard Lin"),
        new Photo("wallpapers/w44.jpg", "Passo Giau, Dolomites", "Luca Bravo"),
        new Photo("wallpapers/w45.jpg", "Piatra Craiului, Romania", "Urban Vintage"),
        new Photo("wallpapers/w46.jpg", "Alpstein, Switzerland", "Niklas Tidbury"),
        new Photo("wallpapers/w47.jpg", "Lake Atitlán, Guatemala", "Mark Harpur"),
        new Photo("wallpapers/w48.jpg", "Silver Falls, Oregon", "Jarred Decker"),
        new Photo("wallpapers/w49.jpg", "Bunderspitz, Switzerland", "Sam Sommer"),
        new Photo("wallpapers/w50.jpg", "Torres del Paine, Chile", "Marc Thunis"),
    };

    private Nature() { }

    static List<Suggestions.Item> items() {
        List<Photo> shuffled = new ArrayList<>();
        Collections.addAll(shuffled, PHOTOS);
        Collections.shuffle(shuffled);
        List<Suggestions.Item> out = new ArrayList<>();
        for (Photo p : shuffled) {
            Suggestions.Item s = new Suggestions.Item();
            s.type = "nature";
            s.title = p.location;
            s.overview = p.photographer;
            s.backdrop = "asset:" + p.asset;  // asset:// scheme for asset loading
            s.pkg = "";
            s.key = "nature|" + p.asset;
            s.progress = -1f;
            s.link = "";
            out.add(s);
        }
        return out;
    }

    /**
     * Loads a nature photo from APK assets into a Bitmap, downsampled to fit the screen.
     * Must be called off the main thread.
     */
    static Bitmap load(Context c, String assetPath, int targetW) {
        try (InputStream in = c.getAssets().open(assetPath)) {
            // First pass: read dimensions only.
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            // We need a second stream since InputStream isn't seekable.
            try (InputStream in2 = c.getAssets().open(assetPath)) {
                BitmapFactory.decodeStream(in2, null, bounds);
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (bounds.outWidth / (opts.inSampleSize * 2) >= targetW) opts.inSampleSize *= 2;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeStream(in, null, opts);
        } catch (IOException e) {
            android.util.Log.w("Nature", "failed to load asset " + assetPath + ": " + e);
            return null;
        }
    }
}
