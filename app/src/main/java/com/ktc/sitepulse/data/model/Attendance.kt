package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/** Plain {lat,lng} map, as stored on inGps/outGps (rounded to 6 decimals). */
data class GpsPoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)

/**
 * Firestore: attendance/{date_workerId} — one doc per worker per calendar date
 * (UTC date, see DateUtils.todayStrUtc). Written with setDoc(merge=true) so IN
 * and OUT can land in the same doc.
 */
data class Attendance(
    @DocumentId @get:Exclude val docId: String = "",
    val workerId: String = "",
    val date: String = "",
    val siteCode: String = "",
    val siteName: String = "",
    val lastAction: String = "",
    val markedBy: String = "",
    val shift: String? = null,
    @get:PropertyName("in") @set:PropertyName("in") var checkIn: String? = null,
    val inGps: GpsPoint? = null,
    val inDist: Long? = null,
    var out: String? = null,
    val outGps: GpsPoint? = null,
    val outDist: Long? = null,
) {
    @get:Exclude
    val hasIn: Boolean get() = !checkIn.isNullOrBlank()

    @get:Exclude
    val hasOut: Boolean get() = !out.isNullOrBlank()
}
