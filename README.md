# OneOS

A minimalist, dark, **liquid‑glass** Android launcher inspired by Samsung One UI and iPadOS. Built with Kotlin + Jetpack Compose (Android 8.0+ / API 26).

## Features
- **Setup flow** – welcome, language, Wi‑Fi, name, theme, done (saved with DataStore)
- **Lock screen** – big iPadOS‑style clock (style set in Good Lock › LockStar)
- **Home screen** – clock / weather / battery widgets, app grid, glass dock, page dots, page 2 lists all your real installed apps
- **Built‑in apps** – Clock (world clock, stopwatch, timer), Calculator, Notes, Reminders, Calendar, Files, Photos, Weather (open‑meteo), Music player; Camera, Maps, Contacts, Messages, Mail, FaceTime and **Play Store** open the system apps
- **One Browser** – WebView browser with tabs, address/search bar, back/forward/reload
- **One AI** – assistant that understands commands like `open settings`, `open browser`, `turn on OneDesk`, `open YouTube`, `search cats`, `dark mode`, `lock`; optional OpenAI‑compatible API key for chat
- **OneDesk** – DeX‑style desktop: taskbar, start menu, draggable/resizable/minimize/maximize windows running the built‑in apps
- **Settings** – System Specs, Software Update (checks this repo's GitHub releases), **Good Lock** (DockStyle, Home Up icon size, Theme Park accent, Liquid Glass blur, LockStar clock), Display & Wallpaper, One AI, About

## Install
1. Download `OneOS.apk` from the [latest release](https://github.com/Woodjamesley201/OneOS/releases/latest).
2. Open it on your phone/tablet and allow "Install unknown apps" when asked.

## Set as default launcher
Press Home → choose **OneOS** → **Always**. Or: Settings › Apps › Default apps › Home app › OneOS (also available in OneOS Settings › Set as Default Home App).

## Build it yourself
Needs JDK 17 and the Android SDK.
```bash
./build-apk.sh   # -> dist/OneOS.apk
```
Every push to `main` builds the APK with GitHub Actions and publishes it as a release.

## Notes
OneOS is a launcher app, not a replacement firmware. Real blur needs Android 12+; older versions get a translucent glass look.
