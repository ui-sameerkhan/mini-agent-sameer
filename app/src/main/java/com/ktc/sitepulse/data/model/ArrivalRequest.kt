package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/** Firestore: arrivalRequests/{autoId} — supervisor-reported new arrivals, admin-approved. */
data class ArrivalRequest(
    @DocumentId @get:Exclude val docId: String = "",
    val site: String = "",
    val workerId: String = "",
    val name: String = "",
    val designation: String = "",
    val requestedDate: String = "",
    val requestedBy: String = "",
    val status: String = "pending", // pending | approved | rejected
    val ts: String = "",
    val approvedAt: String? = null,
    val approvedBy: String? = null,
    val rejectedAt: String? = null,
    val rejectedBy: String? = null,
)
