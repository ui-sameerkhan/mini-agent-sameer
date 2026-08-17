# SitePulse push function

Replaces the old external Netlify `send-push.js` (which likely still used
Google's legacy FCM API — shut down June 2024, which explains the silent
failures). This runs inside the `ktc-manpower` Firebase project itself, so
its logs are actually reachable in the Firebase Console.

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

## Deploy

From the repo root (where `firebase.json` and `.firebaserc` live):

```
cd functions
npm install
cd ..
firebase deploy --only functions
```

The deploy output prints a URL that looks like:

```
https://sendpush-xxxxxxxxxx-uc.a.run.app
```

or (depending on CLI version):

```
https://us-central1-ktc-manpower.cloudfunctions.net/sendPush
```

## After deploying

Copy that URL and:

1. In the Android app: paste it into `Constants.SEND_PUSH_FUNCTION_URL` in
   `app/src/main/java/com/ktc/sitepulse/Constants.kt`, rebuild, reinstall.
2. In the web app (`index.html`): replace every
   `/.netlify/functions/send-push` with that same URL.

## Checking it worked

Firebase Console → Functions → `sendPush` → Logs will show every call,
including the real error if one fails (bad token, permission issue, etc.) —
something the old Netlify function never surfaced.
