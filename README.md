# Hume Launcher v2

A tvOS-inspired home screen for Android TV and Google TV, built on top of [Hume Launcher](https://github.com/petrobot-admin/hume-launcher) by Petrobot. Designed and customized for TCL Google TV (QM6K series) but works on any Android TV device.

---

## Screenshots

### Home — Dock View
![Dock](screenshots/dock.png)

### Hero Mode — Full-screen Wallpaper
![Hero](screenshots/hero.png)

### Control Center
![Control Center](screenshots/control-center.png)

### Weather
![Weather](screenshots/weather.png)

### Continue Watching + App Grid
![Continue Watching](screenshots/continue-watching.png)

### App Grid
![App Grid](screenshots/app-grid.png)

---

## Features

- **50 bundled 4K landscape wallpapers** — shuffled on launch, auto-advances every 30 seconds. Location and photographer credited in hero mode.
- **tvOS-style dock** — frosted glass, app icons with glare animation on focus. Light/dark glare automatically selected based on icon background.
- **Continue Watching row** — populated from the system TV provider (Netflix, Prime Video, etc.) with app badge and thumbnail.
- **Live weather widget** — current conditions, hourly and 5-day forecast via Open-Meteo.
- **Control Center** — Power Off, Restart, Wi-Fi, Volume, Audio Output, and shortcuts to Hume Settings, TV Settings, and Accessibility.
- **Home button override** — AccessibilityService intercepts the Home key on Google TV so it always opens Hume instead of the stock launcher.
- **State restoration** — returns to the exact app you were focused on when pressing Home from another app.
- **Liquid Glass UI** — adaptive frosted glass on the dock, weather card, search, and time/control chip.

---

## Tested On

**TCL 65" QM6K Series** — QLED Mini LED 4K HDR Smart Google TV (65QM6K), 144Hz  
Android 14 (API 34)

This launcher should work on any Android TV or Google TV device running Android 8.0+, but all development and testing was done on the above hardware.

---

## Requirements

- Android TV or Google TV device
- ADB enabled (Developer Options → Network Debugging)
- macOS/Linux build machine with:
  - JDK 17+ (`brew install openjdk@17`)
  - Android SDK build-tools 34.0.0 and platform android-34:
    ```bash
    brew install android-commandlinetools
    sdkmanager "build-tools;34.0.0" "platforms;android-34"
    ```

---

## Installation

### 1. Clone

```bash
git clone https://github.com/AdrianTong/hume-launcher-v2.git
cd hume-launcher-v2
```

### 2. Connect your TV via ADB

Enable **Developer Options → Network Debugging** on your TV, then pair:

```bash
adb pair <tv-ip>:<pair-port>
adb connect <tv-ip>:<connect-port>
adb devices  # confirm device appears
```

### 3. Build and deploy

```bash
VERSION_CODE=1 VERSION_NAME=1.0 bash deploy.sh <adb-serial>
```

`deploy.sh` builds the APK, installs it, grants `READ_TV_LISTINGS` for Continue Watching, grants the TCL auto-start permission, and re-enables the HomeKeyService accessibility service automatically.

> Find your ADB serial with `adb devices`.

### 4. Set as default home (optional but recommended)

```bash
adb -s <serial> shell cmd package set-home-activity com.petrobot.hume/.MainActivity
```

### 5. Enable Home button override (optional)

On the TV: **Hume Settings → Permissions → Home button override → Open Accessibility Settings**, then enable **Hume Launcher – Home Button**.

This makes the remote's Home button always open Hume instead of the stock Google TV launcher. The `deploy.sh` script re-enables this automatically on each reinstall.

---

## Changes from Original Hume Launcher

This is a heavily modified fork. Below is a summary of what was added, changed, or removed compared to [petrobot-admin/hume-launcher](https://github.com/petrobot-admin/hume-launcher).

### Wallpapers
- **Added:** 50 bundled 4K landscape photos from Unsplash, each with location and photographer credit. The original used procedurally generated abstract gradient backgrounds.
- **Added:** Rotating wallpaper carousel in hero mode — shuffled on launch, auto-advances every 30 seconds.
- **Added:** Location + photographer credit overlay shown in hero mode, dims when navigating to the status bar, hides when the dock returns.
- **Added:** First photo pre-decoded on the same background thread as the shuffle so the hero is never blank on launch.

### Continue Watching
- **Added:** A "Continue Watching" row inserted between the dock and the app grid, sourced from the system `TvContract.WatchNextPrograms` provider (the same data Google TV and Haven use). Requires `READ_TV_LISTINGS` permission granted via ADB.
- **Added:** Each card shows a thumbnail, title, and translucent app badge (Netflix, Prime Video, etc.) in the bottom-right corner.
- **Added:** Row is horizontally scrollable when more than 3 items are present.

### Control Center
The original CC had Do Not Disturb, Sleep Timer, Memory, and Search shortcuts. This version replaces them with more useful TV controls:

| Original | v2 |
|---|---|
| Power Off | Power Off |
| Volume | Restart *(new)* |
| Wi-Fi | Wi-Fi |
| Do Not Disturb | Volume |
| Audio Output | Audio Output *(full width)* |
| AirPlay | — *(removed)* |
| Bluetooth | — *(removed)* |
| Sleep Timer | — *(removed)* |
| Inputs | — *(removed)* |
| Search | — *(removed)* |
| Hume Settings | Hume Settings |
| TV Settings | TV Settings |
| — | Accessibility *(new)* |

- **Changed:** Inputs, Bluetooth, AirPlay, Do Not Disturb, Sleep Timer, and Search removed.
- **Added:** Restart button (uses `DevicePolicyManager.reboot()`, requires device admin).
- **Added:** Accessibility shortcut.
- **Changed:** Audio Output tile spans full width.

### Status Bar
- **Changed:** Time and Wi-Fi status combined into a single pill chip (right of the search button) that opens Control Center on press. The original had a separate CC button.
- **Removed:** Avatar/profile button, refresh button (hidden).
- **Changed:** App labels in the grid are smaller (10sp, 82% width) and only visible when the tile is focused.

### Focus Glare Animation
- **Changed:** The tile focus glare automatically selects dark or white based on the icon's background luminance — white glare on dark icons, dark glare on light-background icons (Netflix, YouTube, ESPN, etc.). Banner images are sampled separately from icon colors.
- **Added:** The same glare animation is applied to Continue Watching thumbnails.

### Navigation & State
- **Added:** `HomeKeyService` AccessibilityService intercepts the Home key so it always opens Hume on Google TV, which otherwise routes Home to its own launcher regardless of the default home setting.
- **Added:** Foreground app tracking so the Home key passes through to the Google TV launcher when the user intentionally launches it, rather than immediately redirecting back.
- **Added:** State restoration — when returning from any app via Home, focus and scroll position are restored to exactly where you were.
- **Fixed:** Hero wallpaper stays expanded when navigating from the shelf to the status bar (weather, search, time chip). In the original, any focus change away from the shelf would collapse the dock.
- **Fixed:** Pressing Home while the hero is expanded (from the status bar) collapses it and brings the dock back into view rather than silently consuming the key.
- **Added:** Grid UP/DOWN navigation uses a custom EASE interpolator at 320ms, matching the feel of the hero animations.

### Launch Animations
- **Removed:** App launch zoom animation (card expanding to fill screen). Apps open instantly.

### Google TV Shortcut
- **Added:** A "Google TV" tile in the app grid that launches the stock Google TV launcher directly, for cases where you want to access Google TV's For You recommendations or app store.

### Deploy Script
- **Added:** `deploy.sh` — single command to build, install, grant all required permissions (`READ_TV_LISTINGS`, TCL `AUTO_START`), and re-enable the accessibility service. The original had no install automation.

---

## Known Issues

### Home Button & Accessibility Service

Google TV intercepts the Home key at the system level and routes it to its own launcher, bypassing Android's standard default-home mechanism. Hume works around this using an `AccessibilityService` (`HomeKeyService`) that captures `KEYCODE_HOME` globally and redirects it to Hume.

**TCL-specific:** TCL's `TclAppBoot` system blocks third-party services from auto-starting without explicit permission. If you enable the accessibility service and it immediately flips back off, run:

```bash
adb -s <serial> shell appops set com.petrobot.hume AUTO_START allow
```

`deploy.sh` does this automatically, but if you enable the service manually through TV Settings it may need to be re-granted.

**On reinstall:** Android automatically disables accessibility services when an app is reinstalled as a security measure. `deploy.sh` re-enables it after every install. If you install manually (without `deploy.sh`), re-enable it in TV Settings → Accessibility.

**Google TV shortcut tile:** A "Google TV" tile is included in the app grid that launches the stock Google TV launcher directly. While in the Google TV launcher, the Home button passes through normally (does not redirect to Hume) so you can navigate Google TV freely. Pressing Home from any other app still returns to Hume.

---

## Updating

Re-run `deploy.sh` with an incremented `VERSION_CODE`:

```bash
VERSION_CODE=2 VERSION_NAME=1.1 bash deploy.sh <adb-serial>
```

---

## Credits

- Original [Hume Launcher](https://github.com/petrobot-admin/hume-launcher) by Petrobot — Apache 2.0
- Wallpapers from [Unsplash](https://unsplash.com) — see `src/com/petrobot/hume/Nature.java` for individual credits
- Weather data from [Open-Meteo](https://open-meteo.com)

Licensed under the [Apache License 2.0](LICENSE).
