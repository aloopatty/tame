# Ludo Glass Arena — HTML5 Edition

A four-player, pass-and-play Ludo game built with plain HTML, CSS and JavaScript. It has no runtime CDN, external fonts, image hosts, login, API or network calls. The service worker caches the app shell for repeat offline play after the first successful load.

## Run locally
Open `index.html` in a modern browser. Core gameplay works without a network connection. Service-worker installation and offline reload caching require serving the folder from localhost or HTTPS.

## Publish with GitHub Pages
1. Open the repository **Settings → Pages**.
2. Under **Build and deployment**, set **Source** to **GitHub Actions**.
3. The workflow at `.github/workflows/deploy-web.yml` will publish the app after the next push to `main` (or run it manually from Actions).
4. Open the published Pages URL once while online. The service worker then caches the app shell for offline reloads.

## Files
- `index.html` — app shell and corner player HUDs
- `style.css` — responsive glass UI and board styling
- `app.js` — local game rules, dice, timers, captures and token movement
- `manifest.webmanifest` — installable web app metadata
- `sw.js` — offline app-shell cache

The Android build workflow is separate from this HTML5 edition; this web edition is not yet embedded into the existing Android APK.
