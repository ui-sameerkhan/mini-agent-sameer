package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Firestore: leaves/{autoId}. Two ways a record here gets created:
 *  - Admin directly marks a worker on leave (Roster tab) — status is "approved"
 *    immediately, excludes the worker from the Absent Report right away.
 *  - Office staff self-submit a leave application — status starts "pending"
 *    and only counts toward the Absent Report once an admin approves it.
 */
data class Leave(
    @DocumentId @get:Exclude val docId: String = "",
    val workerId: String = "",
    val site: String = "",
    val fromDate: String = "",
    val toDate: String = "",
    val reason: String? = null,
    /** "Annual" | "Sick" | "Unpaid" | "Other" — only "Annual" counts against a worker's yearly balance. */
    val leaveType: String = "Annual",
    val markedBy: String = "",
    val ts: String = "",
    val status: String = "approved", // "approved" | "pending" | "rejected"
    /** Set only for self-submitted applications — the office staff account that applied. */
    val requestedBy: String? = null,
    val approvedBy: String? = null,
    val approvedAt: String? = null,
    val rejectedBy: String? = null,
    val rejectedAt: String? = null,
) {
    @get:Exclude
    val isPending: Boolean get() = status == "pending"
}
