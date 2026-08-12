package com.ktc.sitepulse

/**
 * Mirrors the constants hard-coded in the original SitePulse PWA (index.html).
 * Kept identical so this app talks to the exact same Firebase project / data.
 */
object Constants {
    val ADMIN_EMAILS = setOf("admin@ktc-manpower.com")
    val NOTIFY_EMAILS = listOf("admin@ktc-manpower.com", "sameer.khan.ktc@outlook.com")

    /** Accounts on this email domain are recognized as office staff (self-service leave, own attendance history). */
    const val OFFICE_STAFF_EMAIL_DOMAIN = "ktcco.net"

    const val DEFAULT_GEOFENCE_RADIUS_M = 500

    // Netlify site hosting send-push.js / send-email.js / weekly-backup.js from the
    // original PWA repo — reused as-is so new-arrival notifications keep working.
    const val NETLIFY_BASE_URL = "https://ktc-manpower.netlify.app"
    val SEND_PUSH_URL get() = "$NETLIFY_BASE_URL/.netlify/functions/send-push"
    val SEND_EMAIL_URL get() = "$NETLIFY_BASE_URL/.netlify/functions/send-email"

    const val WORKERS_PAGE_SIZE = 50
    const val FIRESTORE_BATCH_LIMIT = 450
}
