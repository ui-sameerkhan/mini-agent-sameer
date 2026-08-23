const { onRequest } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const { v1: firestoreV1 } = require("@google-cloud/firestore");

admin.initializeApp();
const firestoreAdminClient = new firestoreV1.FirestoreAdminClient();

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

/**
 * Weekly automated backup — replaces the old Netlify weekly-backup.js, which sat on the same
 * dead Netlify site as the confirmed-404 send-push.js and is very likely dead too. Uses
 * Firestore's own native "managed export" to write a full snapshot of every collection to
 * Cloud Storage, on a schedule, with zero admin action needed — separate from, and in addition
 * to, the in-app "Download Full Backup" Excel export, which stays manual/on-demand for a
 * human-readable copy.
 *
 * One-time setup required before this works (see functions/README.md):
 *  1. A Cloud Storage bucket named "<project-id>-backups" must exist to receive the export.
 *  2. The Cloud Functions runtime service account needs the "Cloud Datastore Import Export
 *     Admin" IAM role, granted in Google Cloud Console → IAM.
 */
exports.weeklyBackup = onSchedule(
  { schedule: "every sunday 02:00", timeZone: "Asia/Dubai", region: "us-central1" },
  async (event) => {
    const projectId = process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT || process.env.PROJECT_ID;
    const bucket = `gs://${projectId}-backups`;
    const timestamp = new Date().toISOString().slice(0, 10);
    try {
      const [operation] = await firestoreAdminClient.exportDocuments({
        name: firestoreAdminClient.databasePath(projectId, "(default)"),
        outputUriPrefix: `${bucket}/scheduled/${timestamp}`,
        collectionIds: [], // empty = every collection
      });
      logger.info("weeklyBackup export started", { operation: operation.name, bucket, timestamp });
    } catch (e) {
      // Most common cause: the bucket above doesn't exist yet, or the service account is
      // missing the Import/Export Admin role — both one-time setup steps, see README.
      logger.error("weeklyBackup failed", { message: e.message, code: e.code });
      throw e;
    }
  }
);
