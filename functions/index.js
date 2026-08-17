const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

// Browser origins allowed to call this over CORS — the Android/iOS app doesn't need CORS
// (it's a server-to-server style request, not a browser fetch), only the web app does.
const ALLOWED_ORIGINS = new Set([
  "https://ktc-manpower.web.app",
  "https://ktc-manpower.firebaseapp.com",
  "https://ktc-manpower.netlify.app",
]);

function applyCors(req, res) {
  const origin = req.get("origin");
  if (origin && ALLOWED_ORIGINS.has(origin)) {
    res.set("Access-Control-Allow-Origin", origin);
  }
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");
}

/**
 * Replaces the old Netlify send-push.js. Body: { token, title, body, url }.
 * Sends one FCM message to one device token and reports back exactly what
 * happened — the previous function's failures were invisible from the app,
 * which made "notification never arrives" undiagnosable. This one returns a
 * real HTTP status and error body instead of swallowing anything.
 */
exports.sendPush = onRequest({ region: "us-central1", cors: false }, async (req, res) => {
  applyCors(req, res);
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  if (req.method !== "POST") {
    res.status(405).json({ ok: false, error: "POST only" });
    return;
  }

  const { token, title, body, url } = req.body || {};
  if (!token || !title) {
    res.status(400).json({ ok: false, error: "token and title are required" });
    return;
  }

  try {
    const messageId = await admin.messaging().send({
      token,
      notification: { title, body: body || "" },
      data: { url: url || "./" },
      android: { priority: "high" },
      webpush: { fcmOptions: { link: url || "./" } },
    });
    logger.info("sendPush ok", { messageId });
    res.status(200).json({ ok: true, messageId });
  } catch (e) {
    // Logged server-side too (Firebase Console → Functions → Logs) — unlike the old Netlify
    // function, this is a first-party Firebase project, so those logs are actually reachable.
    logger.error("sendPush failed", { code: e.code, message: e.message });
    res.status(500).json({ ok: false, error: e.message, code: e.code });
  }
});
