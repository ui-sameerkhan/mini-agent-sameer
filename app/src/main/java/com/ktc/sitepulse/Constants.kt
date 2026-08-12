package com.ktc.sitepulse

/**
 * Mirrors the constants hard-coded in the original SitePulse PWA (index.html).
 * Kept identical so this app talks to the exact same Firebase project / data.
 */
object Constants {
    val ADMIN_EMAILS = setOf("admin@ktc-manpower.com")
    val NOTIFY_EMAILS = listOf("admin@ktc-manpower.com", "sameer.khan.ktc@outlook.com")

    const val DEFAULT_GEOFENCE_RADIUS_M = 500

    // TODO: set this to the actual deployed Netlify site URL for the send-push.js /
    // send-email.js / weekly-backup.js functions bundled in the original PWA repo
    // (Site settings -> Domain management, in the Netlify dashboard for that site).
    // Push/email notifications on new-arrival requests are no-ops until this is set.
    const val NETLIFY_BASE_URL = "" // e.g. "https://sitepulse-ktc.netlify.app"
    val SEND_PUSH_URL get() = "$NETLIFY_BASE_URL/.netlify/functions/send-push"
    val SEND_EMAIL_URL get() = "$NETLIFY_BASE_URL/.netlify/functions/send-email"

    const val WORKERS_PAGE_SIZE = 50
    const val FIRESTORE_BATCH_LIMIT = 450
}
