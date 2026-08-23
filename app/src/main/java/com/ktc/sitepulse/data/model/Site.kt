package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/** Firestore: sites/{code} — doc id == code (uppercased project code). */
data class Site(
    @DocumentId @get:Exclude val docId: String = "",
    val code: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radius: Long = 500,
    /**
     * Optional office WiFi network name (SSID). When set, being connected to
     * this network counts as being "at" this site — no GPS fix needed, so
     * office staff can punch in/out indoors where GPS is often unreliable.
     */
    val wifiSsid: String? = null,
    /** Hour-of-day (0-23) boundaries for this site's Day/Night shift split — null means
     * "use the company default" (18 / 5, see DateUtils.shiftFor). Only needs setting for a
     * site that genuinely runs different hours than the rest of the company. */
    val nightStartHour: Long? = null,
    val dayStartHour: Long? = null,
)
