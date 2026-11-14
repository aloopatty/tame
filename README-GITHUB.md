# Ludo Glass Arena — Offline Android Game

Native Android pass-and-play Ludo for 2–4 people sharing one phone. The board and dice are drawn locally on Android Canvas. No WebView, hosted page, login, server or network permission.

## Get the APK from GitHub Actions
1. Create a GitHub repository.
2. Extract this ZIP and push the **contents of `LudoGlassArena`** to the repository root. `.github/` and `android/` must be at the top level.
3. Open **Actions** and select **Build offline Android APK**.
4. Open the completed run and download the artifact **Ludo-Glass-Arena-offline-APK**.
5. Extract `app-debug.apk` and install it on Android.

The workflow runs on pushes to `main` or `master`; you can also trigger it manually from Actions.

## Controls
- Choose 2, 3 or 4 players at the top. Names are optional and not requested.
- Tap the active player's die to roll; tap a highlighted token to move it.
- Six releases a token from base. Sixes and captures grant another turn; three consecutive sixes forfeit the turn.
- Safe squares protect tokens. Bring all four tokens home to win.

## Local build
Requires JDK 17, Android SDK platform 35, build-tools 35.0.0 and Gradle 8.9.

```sh
cd android
gradle --no-daemon assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`.
