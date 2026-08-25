// Firebase Cloud Messaging background handler. Runs when the app tab isn't focused/open.
// firebaseConfig below must be kept in sync with web/js/firebase-init.js — service workers
// can't import ES modules from other app files, so the values are duplicated here.
importScripts("https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-compat.js");

firebase.initializeApp({
  apiKey: "REPLACE_WITH_WEB_API_KEY",
  authDomain: "ktc-manpower.firebaseapp.com",
  projectId: "ktc-manpower",
  storageBucket: "ktc-manpower.firebasestorage.app",
  messagingSenderId: "102474530720",
  appId: "REPLACE_WITH_WEB_APP_ID",
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const title = payload.notification?.title || "SitePulse";
  const body = payload.notification?.body || "";
  self.registration.showNotification(title, { body, icon: "/favicon.ico" });
});
