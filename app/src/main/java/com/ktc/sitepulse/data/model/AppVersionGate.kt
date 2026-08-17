package com.ktc.sitepulse.data.model

/**
 * Firestore: settings/appVersion — remote force-update threshold. Any installed app whose
 * BuildConfig.VERSION_CODE is below minVersionCode gets blocked with an update screen the
 * moment this doc changes (live-listened), so an admin can shut off an older APK on demand
 * without touching the phones it's on.
 */
data class AppVersionGate(
    val minVersionCode: Long = 0,
    val updateUrl: String = "",
    val message: String = "",
)
