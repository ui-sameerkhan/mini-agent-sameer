// Web push via Firebase Cloud Messaging. Needs a one-time setup step beyond firebase-init.js:
// Firebase Console → Project Settings → Cloud Messaging → Web configuration → Generate key pair
// (a "VAPID key"), then paste it into VAPID_KEY below. Until that's done, Enable Notifications
// shows a clear message instead of silently failing.
import { app } from "./firebase-init.js";
import { registerPushToken } from "./data.js";
import { toast } from "./ui.js";

const VAPID_KEY = "REPLACE_WITH_VAPID_KEY";

export async function enablePush(email) {
  if (VAPID_KEY.startsWith("REPLACE_")) {
    toast("Push notifications aren't configured yet (missing VAPID key) — see web/js/push.js", "error");
    return;
  }
  if (!("serviceWorker" in navigator) || !("Notification" in window)) {
    toast("This browser doesn't support push notifications.", "error");
    return;
  }
  try {
    const permission = await Notification.requestPermission();
    if (permission !== "granted") {
      toast("Notification permission was not granted.", "error");
      return;
    }
    const { getMessaging, getToken } = await import("https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging.js");
    const registration = await navigator.serviceWorker.register("/sw.js");
    const messaging = getMessaging(app);
    const token = await getToken(messaging, { vapidKey: VAPID_KEY, serviceWorkerRegistration: registration });
    if (token) {
      await registerPushToken(email, token);
      toast("Notifications enabled", "success");
    }
  } catch (e) {
    toast(`Could not enable notifications: ${e.message}`, "error");
  }
}
