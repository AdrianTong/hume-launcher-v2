#!/bin/bash
# Build and deploy Hume to an ADB-connected TV.
# Usage: VERSION_CODE=155 VERSION_NAME=1.5.18 bash deploy.sh [adb-serial]
set -euo pipefail
cd "$(dirname "$0")"

SERIAL="${1:-}"
ADB="adb${SERIAL:+ -s $SERIAL}"

# Build
JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null || echo '')}"
ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export JAVA_HOME ANDROID_HOME
bash build.sh

# Install (non-incremental to avoid stale caches)
$ADB install -r --no-incremental build/tvos-home.apk

# Permissions that Android resets on reinstall
$ADB shell pm grant com.petrobot.hume android.permission.READ_TV_LISTINGS

# TCL blocks third-party services from auto-starting without this permission.
# Without it the accessibility service toggle flips back off immediately after enabling.
$ADB shell appops set com.petrobot.hume AUTO_START allow

# Re-enable the HomeKeyService accessibility service — Android disables it on every reinstall.
$ADB shell settings put secure \
    enabled_accessibility_services com.petrobot.hume/com.petrobot.hume.HomeKeyService
$ADB shell settings put secure accessibility_enabled 1

echo "Deployed. HomeKeyService re-enabled."
