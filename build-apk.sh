#!/usr/bin/env bash
# Builds a debug APK of OneOS and copies it to ./dist/OneOS.apk
# Requires: JDK 17 and Android SDK (ANDROID_HOME or local.properties sdk.dir)
set -euo pipefail
cd "$(dirname "$0")"
chmod +x ./gradlew
./gradlew --no-daemon assembleDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/OneOS.apk
echo "APK ready: $(pwd)/dist/OneOS.apk"
