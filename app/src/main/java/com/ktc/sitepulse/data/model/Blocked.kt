package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/** Firestore: blocked/{autoId} — logged whenever a mark() attempt is outside every geofence. */
data class Blocked(
    @DocumentId @get:Exclude val docId: String = "",
    val workerId: String = "",
    val name: String = "",
    val date: String = "",
    val time: String = "",
    val gps: BlockedGps? = null,
    val nearestSite: String = "",
    val distance: Long = 0,
    val action: String = "",
)

data class BlockedGps(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val acc: Double? = null,
)
