# Ludo Glass Arena — Offline Android

A self-contained native Android pass-and-play Ludo game. The game board, rules, dice and UI are drawn locally in Android; no WebView, website, network service, login, or internet permission is used.

## Build locally

Requirements: JDK 17, Android SDK platform 35, build-tools 35.0.0, Gradle 8.9.

```sh
gradle --no-daemon clean assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Build on GitHub

Push this project to a repository's `main` or `master` branch. The included workflow `.github/workflows/build-android.yml` builds the APK automatically. Open the repository's **Actions** tab, select the latest **Build offline Android APK** run, and download the `Ludo-Glass-Arena-offline-APK` artifact.

The APK is debug-signed for direct installation. For 2–4 players on one device, choose the player count at the top and tap the active player's die. A six releases a token; tap a highlighted token to move it. A six or capture grants another turn.
