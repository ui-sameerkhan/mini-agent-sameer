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

    /**
     * Office staff checking themselves in get a much wider allowance than a worker being marked
     * at a site. A tight geofence is the point for site attendance — it proves the person is at
     * the workface. Staff are salaried and mobile between office, stores and site, so holding
     * them to a few hundred metres just blocks legitimate check-ins. Overridable per site via
     * Site.staffRadius.
     */
    const val DEFAULT_STAFF_RADIUS_M = 5000

    /** Company-wide fallback for "what counts as a late arrival" — a day-shift check-in at or
     * after this local hour. A site running different hours overrides it via Site.lateAfterHour;
     * existing site docs have no such field, so they simply fall back to this. */
    const val DEFAULT_LATE_AFTER_HOUR = 8

    // Netlify site hosting send-email.js / weekly-backup.js from the original PWA repo —
    // reused as-is; email delivery hasn't shown the same silent-failure symptoms push had.
    const val NETLIFY_BASE_URL = "https://ktc-manpower.netlify.app"
    val SEND_EMAIL_URL get() = "$NETLIFY_BASE_URL/.netlify/functions/send-email"

    // Push moved off the old Netlify send-push.js (likely still on Google's legacy FCM API,
    // shut down June 2024 — see functions/README.md) to a first-party Firebase Cloud Function
    // whose logs are actually reachable. Deploy functions/ per that README, then paste the
    // printed URL here. Left as a placeholder until then — NetlifyApi.sendPush() detects it
    // and reports a clear error instead of firing requests at a URL that doesn't exist.
    const val SEND_PUSH_FUNCTION_URL = "https://REPLACE_AFTER_DEPLOY"

    const val WORKERS_PAGE_SIZE = 50
    const val FIRESTORE_BATCH_LIMIT = 450
}
