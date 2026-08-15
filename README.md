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
- **Min SDK:** 26 (Android 8.0) · **Target/Compile SDK:** 34
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

## Notes

- The app requires an internet connection (Firestore, the live train-data
  API, and Google Fonts are all fetched over HTTPS at runtime).
- No changes were made to the web app's logic — schedule data, Firebase
  config, and the RailRadar live-data proxy endpoint are all untouched.
