# RailGate Live — Android App

A native Android wrapper for **RailGate Live**, a live railway-crossing status
tracker for Barabanki Junction (BBK). The app shows whether the gate is open
or closed, upcoming train closures, a crowd-sourced "report gate status"
feature with GPS verification, and a points/leaderboard system backed by
Firebase.

The original app is a single self-contained `index.html` (vanilla JS +
Firebase Firestore + a live train-data API). This project packages it as an
installable Android app using a `WebView`, rather than rewriting the UI/logic
natively — the web app's behavior is preserved exactly.

## How it's packaged

- `app/src/main/assets/index.html` — the original web app, unmodified.
- `MainActivity` loads it into a `WebView` using
  [`WebViewAssetLoader`](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content),
  serving it from the virtual `https://appassets.androidx.webkit.net/assets/`
  origin. This (rather than a raw `file://` URL) is what Google recommends for
  local WebView content — it avoids `file://` CORS quirks so the Firebase JS
  SDK's ES-module imports and Firestore calls work normally.
- JavaScript, DOM storage (`localStorage`, used for remembering the
  reporter's name/points), and Geolocation are enabled on the `WebView`.
- The **"Report Gate Status"** feature calls `navigator.geolocation`, which
  Android intercepts and turns into a runtime permission request
  (`ACCESS_FINE_LOCATION`) the first time it's used — same as it would in a
  mobile browser.
- The system back button navigates the WebView's history before exiting the
  app.

## Project structure

```
app/
  src/main/
    java/com/railgatelive/app/MainActivity.kt   # WebView host
    assets/index.html                           # the web app
    res/                                         # icon, theme, strings
    AndroidManifest.xml
build.gradle.kts / settings.gradle.kts           # Gradle config (Kotlin DSL)
gradlew / gradlew.bat                            # Gradle wrapper (8.7)
```

- **Application ID:** `com.railgatelive.app`
- **Min SDK:** 26 (Android 8.0) · **Target/Compile SDK:** 35 (current Play Store requirement)
- **Language:** Kotlin

## Building

Requires the Android SDK (command-line tools or Android Studio) and a
`local.properties` file pointing at it — Android Studio creates this
automatically on first open:

```properties
sdk.dir=/path/to/Android/sdk
```

Then, from the repo root:

```bash
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. This has
been verified to build successfully and pass lint in this environment.

To install on a connected device/emulator: `./gradlew :app:installDebug`.

## Building a signed release (for Google Play)

Play Store requires a signed **Android App Bundle** (`.aab`), not a debug
APK. To build one:

1. Copy `keystore.properties.example` to `keystore.properties` and fill in
   your signing key details (see below for how to generate one). This file
   is gitignored — never commit it or the `.jks` keystore file.
2. Run:
   ```bash
   ./gradlew :app:bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab` — this is the
   file you upload to Play Console.

If you don't have a release keystore yet, generate one:

```bash
keytool -genkeypair -v -keystore railgatelive-release.jks \
  -alias railgatelive -keyalg RSA -keysize 2048 -validity 10950
```

**Keep that keystore file and its passwords safe** — losing them means you
can no longer publish updates to the app under the same listing (unless
you've enrolled in Play App Signing, which Google recommends and defaults
to for new apps — it lets you reset a lost upload key).

## Publishing to Google Play — checklist

Building the app is only part of publishing it. The rest requires your own
Google Play Console account (one-time $25 registration fee) since it can't
be done on your behalf:

1. **Register** at [play.google.com/console](https://play.google.com/console)
   if you haven't already.
2. **Create the app** in Play Console — name, default language, app/game,
   free/paid.
3. **Upload `app-release.aab`** under Production (or Internal testing first,
   recommended) — opt into **Play App Signing** when prompted.
4. **Store listing** — short/full description, screenshots, feature graphic,
   app icon, category. Draft copy and generated assets for all of these are
   included in this project's companion deliverables (see chat).
5. **Privacy policy URL** — required because the app requests location and
   stores a user-provided name. Publish one (a draft is provided) and paste
   its public URL into Play Console's "App content" → "Privacy policy" field.
6. **Data safety form** — declare what's collected (location, display name,
   report/points activity) and how it's used; reference notes are provided.
7. **Content rating questionnaire** — answer Play's standardized
   questionnaire; this app should qualify for the lowest rating tier.
8. **Target audience & ads** — declare age range and that the app has no ads.
9. **Submit for review.** Google's review typically takes a few hours to a
   few days for a first submission.

## Notes

- The app requires an internet connection (Firestore, the live train-data
  API, and Google Fonts are all fetched over HTTPS at runtime).
- No changes were made to the web app's logic — schedule data, Firebase
  config, and the RailRadar live-data proxy endpoint are all untouched.
