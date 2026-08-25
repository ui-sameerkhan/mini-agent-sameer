# SitePulse — Web App

A rebuilt browser companion to the SitePulse Android app, talking to the exact same
`ktc-manpower` Firebase project and Firestore data. Pure static site — no build step, no
npm install. Deploy the contents of this `web/` folder as-is.

## One-time setup (do this before it will work)

### 1. Register a Web app in Firebase

This Firebase project only has the Android app registered so far.

1. Go to Firebase Console → **ktc-manpower** → Project Settings → **Your apps** → **Add app** → Web (`</>`).
2. Name it e.g. "SitePulse Web". You can skip Firebase Hosting — this deploys to Netlify instead.
3. Copy the `firebaseConfig` object it shows you.
4. Open `web/js/firebase-init.js` and paste the real `apiKey` and `appId` values in.

### 2. (Optional) Enable push notifications

1. Firebase Console → Project Settings → **Cloud Messaging** → Web configuration → **Generate key pair**.
2. Copy the "Key pair" (VAPID key) and paste it into `VAPID_KEY` in `web/js/push.js`.
3. Also copy the same `apiKey`/`appId` from step 1 into `web/sw.js` (service workers can't
   import the app's other files, so the config is duplicated there).

Without this step the rest of the app works fine — the Enable Notifications button just
shows a clear "not configured yet" message instead of silently failing.

### 3. Deploy to Netlify

Easiest path if you don't have a PC: use Netlify's web dashboard (works fine from a phone browser).

1. On [app.netlify.com](https://app.netlify.com), **Add new site → Import an existing project**.
2. Connect it to the `ui-sameerkhan/mini-agent-sameer` GitHub repo, branch `claude/android-app-gkcpt4`
   (or whichever branch this ends up on after merging).
3. Set **Base directory** to `web` and **Publish directory** to `web` (no build command needed).
4. Deploy. Every future push to that branch redeploys automatically.

### 4. Firestore security rules

No changes needed — `firestore.rules` at the repo root already has an `adminEmails()` allowlist
this web app and the Android app both rely on; nothing web-specific to add.

## Known platform differences from the Android app

Two Android features have no browser equivalent and are intentionally not faked here:

- **Office WiFi punch-in.** Browsers cannot read the name of the currently connected WiFi
  network (`navigator` has no such API, by design, for privacy) — so WiFi-based check-in
  only works from the Android app. Admins can still configure a site's WiFi SSID from the
  web Sites screen; it just won't be usable to check in from a browser.
- **Fake-GPS / mock-location detection.** Android can detect when a device is reporting a
  mock location (`Location.isFromMockProvider()`). The browser Geolocation API has no
  equivalent flag, so this specific safeguard doesn't exist on web check-ins.

Everything else — GPS geofenced check-in/out, QR scan/generate (individual + bulk badge
PDF), admin manual correction, bulk attendance/leave marking, the company holiday calendar,
per-site shift hours, designation standardization, Excel reporting, full backup/restore, and
push notifications — has full feature parity with the Android app, reading and writing the
same Firestore collections.

## Local preview

Any static file server works, e.g.:

```
cd web
python3 -m http.server 8080
```

Then open `http://localhost:8080`. (Camera/GPS features need HTTPS or `localhost` — both work
for local testing; a real deployment gets HTTPS automatically from Netlify.)
