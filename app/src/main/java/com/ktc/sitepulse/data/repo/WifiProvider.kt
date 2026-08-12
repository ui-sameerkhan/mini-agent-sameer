package com.ktc.sitepulse.data.repo

import android.content.Context
import android.net.wifi.WifiManager

/**
 * Reads the SSID of the WiFi network the phone is currently connected to, so
 * office staff can punch in/out by being on the office WiFi instead of
 * needing a GPS fix (often unreliable indoors). Reuses the location
 * permission the app already requests for GPS check-in — Android ties
 * SSID visibility to location permission/services being on, even though no
 * location fix is actually used here.
 */
class WifiProvider(context: Context) {
    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as? WifiManager

    /** Current network's SSID (unquoted), or null if not on WiFi / SSID unavailable. */
    fun currentSsid(): String? {
        val info = wifiManager?.connectionInfo ?: return null
        val raw = info.ssid ?: return null
        if (raw.isBlank() || raw == WifiManager.UNKNOWN_SSID) return null
        return raw.removeSurrounding("\"").trim().ifBlank { null }
    }
}
