# SitePulse — Web App

This is the **real original SitePulse PWA** — the one that used to be live at
`ktc-manpower.netlify.app` — restored from a saved copy of `index.html`, with the same new
features added this round to the Android app now layered in here too. Single self-contained
`index.html` file (Firebase Web SDK v10 loaded from CDN, no build step, no npm install) plus a
handful of sidecar files it references (`sw.js`, `manifest.json`, icons). Deploy the contents
of this `web/` folder as-is.

It already talks to the real `ktc-manpower` Firebase project with a real, working Web app
config embedded — unlike a from-scratch rebuild, there is **no Firebase Web-app-registration
step needed**.

## What's new in this pass

Added directly into the original file, following its existing vanilla-JS conventions
(`window.fn = ...`, the `$()` DOM helper, static `modal-bg` dialogs):

- **Per-site shift hours** — Sites now have optional Night/Day start-hour overrides; `mark()`
  uses a site's own hours instead of the hardcoded company-wide 18:00/05:00 split.
- **Designation dropdown** — the worker form's Designation field is now a suggest-as-you-type
  `<datalist>` seeded with common trades, for consistent Trade-Wise Summary reporting.
- **Annual leave balance** — workers have a configurable `annualLeaveDays` allowance; office
  staff see a live "X of Y days used this year" balance on their leave page.
- **Leave type** — every leave record (admin-marked or self-applied) is now categorized
  Annual / Sick / Unpaid / Other, shown throughout the roster and leave-history views.
- **Bulk leave marking** — mark a whole group of employees on leave for a date range in one action.
- **Company holiday calendar** — admin-managed list of holiday dates, excluded from the Absent
  Report (`HOLIDAY` status / `Days Holiday` column) instead of counting workers as absent. One-tap
  seed button loads the official UAE MOHRE public holiday calendar for 2026–2027.
- **Admin manual attendance correction** — fix a missed or wrong record for any past date,
  single-worker or bulk (multiple employees, one backdated day at once). Every correction is
  tagged `corrected` / `correctedBy` / `correctedAt` and shows up flagged in the Excel export.
- **QR codes** — scan a badge to fill the Worker ID field (Check-In and Report New Arrival),
  generate a QR code for any single employee, or bulk-generate one printable PDF of badges for
  the whole worker list. Runs entirely on-device (ZXing-style libraries via `qrcode`/`jsQR`),
  no extra Firestore reads.
- **Push fixed to point at the new Cloud Function.** The old calls to
  `/.netlify/functions/send-push` (dead — Google's underlying legacy FCM API was retired in
  2024) now go through a `SEND_PUSH_FUNCTION_URL` constant near the top of the script, left as
  a placeholder until the `functions/` Cloud Function in this repo is deployed — same as the
  Android app's `Constants.SEND_PUSH_FUNCTION_URL`. Paste the deployed URL into both once ready.

## One-time setup

### 1. (Optional, once) Deploy the push notification Cloud Function

Push works client-side already (a real VAPID key is embedded), but the server-side send call
needs `functions/sendPush` (see `functions/README.md` in the repo root) deployed once. Until
then, `notifyAdmin()`/`sendAnnouncement()`'s push fan-out silently no-ops — everything else
works normally, and the in-app announcement banner still shows regardless.

### 2. Deploy to Netlify

Easiest path if you don't have a PC: use Netlify's web dashboard (works fine from a phone browser).

1. On [app.netlify.com](https://app.netlify.com), **Add new site → Import an existing project**.
2. Connect it to the `ui-sameerkhan/mini-agent-sameer` GitHub repo, branch `claude/android-app-gkcpt4`
   (or whichever branch this ends up on after merging).
3. Set **Base directory** to `web` and **Publish directory** to `web` (no build command needed).
4. Deploy. Every future push to that branch redeploys automatically.

### 3. Firestore security rules

No changes needed — `firestore.rules` at the repo root already covers every collection this
app reads/writes (including `holidays`, added for the Android app's own holiday-calendar feature).

## Known platform differences from the Android app

Two Android features have no browser equivalent and are intentionally not faked here (neither
was in the original PWA to begin with, for the same reason):

- **Office WiFi punch-in.** Browsers cannot read the name of the currently connected WiFi
  network (no such API exists, by design, for privacy) — this only works from the Android app.
- **Fake-GPS / mock-location detection.** Android can detect a device reporting a mock
  location (`Location.isFromMockProvider()`); the browser Geolocation API has no equivalent,
  so this safeguard doesn't exist on web check-ins.

Everything else has full feature parity with the Android app, reading and writing the exact
same Firestore collections.

## Icons

`favicon.ico`, `icon-192.png`, and `icon-512.png` were regenerated this round to match the
app's own green/amber theme (`#082E22` / `#D4A34A`) — the original binary assets weren't part
of the saved `index.html` copy. Swap them out if you have the real originals.

## Local preview

Any static file server works, e.g.:

```
cd web
python3 -m http.server 8080
```

Then open `http://localhost:8080`. (Camera/GPS features need HTTPS or `localhost` — both work
for local testing; a real deployment gets HTTPS automatically from Netlify.)
