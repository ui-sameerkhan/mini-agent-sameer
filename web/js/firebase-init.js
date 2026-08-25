// Firebase Web SDK (v10, modular, loaded straight from Google's CDN — no bundler/build step,
// so this whole folder can be deployed to Netlify as-is).
//
// IMPORTANT — one-time setup: this project (ktc-manpower) only has an Android app registered
// so far. To get real values below:
//   1. Firebase Console → ktc-manpower → Project Settings → Your apps → Add app → Web (</>)
//   2. Name it e.g. "SitePulse Web", skip Firebase Hosting (we're using Netlify).
//   3. Copy the firebaseConfig object it shows you and paste the values in below.
// Until you do that, the app will show a clear "not configured" screen instead of failing silently.
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js";
import {
  getAuth, onAuthStateChanged, signInWithEmailAndPassword, sendPasswordResetEmail, signOut,
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import {
  getFirestore, enableIndexedDbPersistence,
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";

export const firebaseConfig = {
  apiKey: "REPLACE_WITH_WEB_API_KEY",
  authDomain: "ktc-manpower.firebaseapp.com",
  projectId: "ktc-manpower",
  storageBucket: "ktc-manpower.firebasestorage.app",
  messagingSenderId: "102474530720",
  appId: "REPLACE_WITH_WEB_APP_ID",
};

export const isConfigured = !firebaseConfig.apiKey.startsWith("REPLACE_") && !firebaseConfig.appId.startsWith("REPLACE_");

let app = null, auth = null, db = null;
if (isConfigured) {
  app = initializeApp(firebaseConfig);
  auth = getAuth(app);
  db = getFirestore(app);
  // Lets check-ins/out queue locally and sync automatically when connectivity returns —
  // mirrors the Android app's offline-ready check-in behavior. Fails harmlessly if the app
  // is open in more than one browser tab at once (persistence can only be owned by one tab).
  enableIndexedDbPersistence(db).catch(() => {});
}

export { app, auth, db, onAuthStateChanged, signInWithEmailAndPassword, sendPasswordResetEmail, signOut };
