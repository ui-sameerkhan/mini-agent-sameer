# SitePulse Cloud Functions

Two functions, both first-party (inside the `ktc-manpower` Firebase project
itself, so their logs are actually reachable in the Firebase Console —
unlike the old external Netlify functions they replace).

- **`sendPush`** — replaces the old `send-push.js` (which returned HTTP 404
  when tested — the Netlify deployment is gone). See "Deploy" below.
- **`weeklyBackup`** — replaces the old `weekly-backup.js`, which sat on the
  same dead Netlify site and is very likely dead too. Runs every Sunday
  2am (Asia/Dubai), writing a full Firestore export to Cloud Storage
  automatically — no admin action needed. This is *in addition to* the
  in-app "Download Full Backup" Excel button, which stays manual/on-demand.

## One-time setup

1. Firestore's Blaze (pay-as-you-go) plan is required for Cloud Functions —
   confirm this in Firebase Console → upgrade if still on Spark. (You already
   said you're fine paying for this if needed.)
2. Install the Firebase CLI if you don't have it:
   ```
   npm install -g firebase-tools
   ```
3. Log in:
   ```
   firebase login
   ```
4. **For `weeklyBackup` only** — two extra one-time steps:
   - Create a Cloud Storage bucket named exactly `<your-project-id>-backups`
     (e.g. `ktc-manpower-backups`) in Google Cloud Console → Cloud Storage →
     Buckets → Create. Any region is fine; match your Firestore region if
     you're not sure.
   - Grant the Cloud Functions runtime service account (usually
     `<project-id>@appspot.gserviceaccount.com`) the **"Cloud Datastore
     Import Export Admin"** IAM role, in Google Cloud Console → IAM & Admin
     → IAM → find that account → Edit → Add Role.
   - Skipping this step doesn't break `sendPush` — `weeklyBackup` will just
     fail on its own schedule and log the reason (missing bucket or missing
     role) in Firebase Console → Functions → Logs.

## Deploy

From the repo root (where `firebase.json` and `.firebaserc` live):

```
cd functions
npm install
cd ..
firebase deploy --only functions
```

The deploy output prints a URL for `sendPush` that looks like:

```
https://sendpush-xxxxxxxxxx-uc.a.run.app
```

or (depending on CLI version):

```
https://us-central1-ktc-manpower.cloudfunctions.net/sendPush
```

`weeklyBackup` is schedule-triggered, not HTTP — it has no URL, it just
starts running on its own every Sunday once deployed.

## After deploying

Copy the `sendPush` URL and:

1. In the Android app: paste it into `Constants.SEND_PUSH_FUNCTION_URL` in
   `app/src/main/java/com/ktc/sitepulse/Constants.kt`, rebuild, reinstall.
2. In the web app (`index.html`): replace every
   `/.netlify/functions/send-push` with that same URL.

## Checking it worked

- Firebase Console → Functions → `sendPush` → Logs shows every push call,
  including the real error if one fails (bad token, permission issue,
  etc.) — something the old Netlify function never surfaced.
- Firebase Console → Functions → `weeklyBackup` → Logs shows each weekly
  run. A successful run logs the Cloud Storage path the export was written
  to; you can browse it in Cloud Storage → Buckets → `<project-id>-backups`
  → `scheduled/`.
