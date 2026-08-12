# SitePulse — Android (native Kotlin)

A 100% native Android port of the **SitePulse** GPS workforce-attendance PWA
(KTC International Contracting LLC) — no WebView, no PWA wrapper. Built with
Kotlin + Jetpack Compose, talking to the **same Firebase project** the web
app uses so both can run against identical data.

## Feature parity with the original web app

- Firebase email/password auth, with admin vs supervisor role derived from
  `Constants.ADMIN_EMAILS` (identical logic to the original).
- GPS geofenced Check-In / Check-Out, including: duplicate-checkin
  prevention, night-shift midnight crossover (UTC date + local-hour shift
  classification, matching the original exactly), supervisor
  check-out-ownership restriction, and blocked-attempt logging when a worker
  is outside every site radius.
- Admin Dashboard: live KPIs, today's attendance by project, blocked
  attempts.
- Sites CRUD with GPS capture for geofence center + radius.
- Workers CRUD, bulk Excel upload (master list / outsourced manpower /
  roster-by-project-code), with the same flexible column-header matching and
  overwrite/reassignment confirmation flow as the web app.
- Roster: admin (pending-arrival approvals, roster upload, mark leave,
  roster-by-project list) and supervisor (report-new-arrival) views.
- Multi-sheet Excel attendance report export (per-project sheets, summary,
  trade-wise summary, leave-aware absent report in day/month modes),
  generated with Apache POI and shared via Android's share sheet.
- FCM push notifications on new-arrival requests, reusing the original
  `send-push.js` / `send-email.js` Netlify functions.
- Firestore offline persistence (writes made offline queue and sync
  automatically), mirroring the web app's `persistentLocalCache` behavior.

See `app/src/main/java/com/ktc/sitepulse/` — organized as `data/` (Firestore
models + repositories), `domain/` (geofencing, date rules, the `mark()`
business logic, Excel import/export), and `ui/` (Compose screens, one
`SitePulseViewModel` shared across screens).

## Required setup before running

This app ships with a **placeholder** `app/google-services.json` so the
project compiles out of the box. It will **not** authenticate against the
real `ktc-manpower` Firebase project until you:

1. In the [Firebase console](https://console.firebase.google.com/), open the
   `ktc-manpower` project → Project settings → Add app → Android.
2. Register package name `com.ktc.sitepulse` (add your debug/release SHA-1
   fingerprints too, needed for some Firebase features).
3. Download the real `google-services.json` and replace
   `app/google-services.json` with it.
4. Set `Constants.NETLIFY_BASE_URL` in
   `app/src/main/java/com/ktc/sitepulse/Constants.kt` to the deployed
   Netlify site URL that hosts `send-push.js` / `send-email.js` (from the
   original PWA repo). Until set, new-arrival push/email notifications are a
   silent no-op — everything else works normally.

## Build

```
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (minified)
```

Or open the project root in Android Studio (Kotlin + Jetpack Compose,
`minSdk 26`, `compileSdk`/`targetSdk 35`).

## Known deltas from the original PWA

- `saveSite()` matches the original behavior exactly: since a site's
  Firestore doc ID is its project code, editing a site's code creates a
  *new* site doc rather than renaming the old one — delete the old code
  explicitly if you need a rename.
- The web app's Service Worker / background-sync logic has no native
  equivalent needed: Firestore's Android SDK offline persistence provides
  the same "write while offline, sync on reconnect" behavior on its own.
- Weekly CSV backup (`weekly-backup.js`) is a server-side/ops concern and is
  unchanged — it isn't part of the mobile client in either version.
