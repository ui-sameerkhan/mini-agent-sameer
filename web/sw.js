// Service worker for SitePulse — registered from index.html's <head> script at scope "/".
// Handles two independent jobs, both referenced by the page: (1) app-shell offline caching
// with Background Sync / Periodic Background Sync refresh, and (2) Firebase Cloud Messaging
// background push (the page's getFcmToken() call binds to whichever SW is active at this scope,
// so push handling has to live here rather than in a separate firebase-messaging-sw.js).
importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging-compat.js");

firebase.initializeApp({
  apiKey: "AIzaSyAJFSujuEgHQsoJWg0lh36vsWlm_DowROw",
  authDomain: "ktc-manpower.firebaseapp.com",
  projectId: "ktc-manpower",
  storageBucket: "ktc-manpower.firebasestorage.app",
  messagingSenderId: "102474530720",
  appId: "1:102474530720:web:824f13bf33148002026ec5",
});

const messaging = firebase.messaging();
messaging.onBackgroundMessage((payload) => {
  const n = payload.notification || {};
  self.registration.showNotification(n.title || "SitePulse", {
    body: n.body || "",
    icon: "icon-192.png",
    data: { url: (payload.data && payload.data.url) || "./" },
  });
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = (event.notification.data && event.notification.data.url) || "./";
  event.waitUntil(clients.openWindow(url));
});

// ---------- App-shell offline cache ----------
const CACHE_NAME = "sitepulse-shell-v1";
const SHELL_URLS = ["./", "./index.html", "./manifest.json", "./icon-192.png", "./icon-512.png"];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL_URLS)).catch(() => {}));
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((names) => Promise.all(names.filter((n) => n !== CACHE_NAME).map((n) => caches.delete(n))))
  );
  self.clients.claim();
});

async function refreshShell() {
  const cache = await caches.open(CACHE_NAME);
  await Promise.all(SHELL_URLS.map(async (url) => {
    try {
      const res = await fetch(url, { cache: "no-store" });
      if (res.ok) await cache.put(url, res);
    } catch (e) { /* offline — keep whatever's already cached */ }
  }));
}

self.addEventListener("sync", (event) => {
  if (event.tag === "sitepulse-refresh") event.waitUntil(refreshShell());
});
self.addEventListener("periodicsync", (event) => {
  if (event.tag === "sitepulse-periodic-refresh") event.waitUntil(refreshShell());
});

// Network-first for same-origin GETs, so signed-in users always get the latest app shell when
// online; falls back to the cached shell when offline. Attendance data itself is never cached
// here — that's Firestore's own offline persistence, enabled in the page script.
self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  const url = new URL(event.request.url);
  if (url.origin !== location.origin) return;
  event.respondWith(
    fetch(event.request)
      .then((res) => {
        const copy = res.clone();
        caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy)).catch(() => {});
        return res;
      })
      .catch(() => caches.match(event.request).then((cached) => cached || caches.match("./index.html")))
  );
});
