package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Firestore: transferRequests/{autoId} — a worker who turned up at a site other than the one
 * their roster says, being moved across.
 *
 * This exists because attendance and the roster were drifting apart. The roster is shared, so
 * the receiving site could always *mark* a transferred worker — attendance was never blocked —
 * but nobody on site could correct the roster entry itself, so the worker kept counting against
 * the site they had left. Every report stayed wrong until head office reassigned them by hand.
 *
 * The flow is deliberately site-local: the foreman or supervisor who is actually looking at the
 * man raises the request, and the receiving site's timekeeper approves it. Head office is not in
 * the loop, because head office cannot see who is standing at the gate.
 *
 * A request only ever pulls a worker TOWARDS the requester's own site — see the create rule in
 * firestore.rules. Nobody can push a worker onto a site they don't hold.
 */
data class TransferRequest(
    @DocumentId @get:Exclude val docId: String = "",
    val workerId: String = "",
    val workerName: String = "",
    val designation: String = "",
    /** The site the roster currently shows. Blank when the worker was never aligned to one. */
    val fromSite: String = "",
    /** The site the worker actually turned up at, and the site they will be moved to. */
    val toSite: String = "",
    val reason: String = "",
    val requestedBy: String = "",
    val requestedByRole: String = "",
    val requestedDate: String = "",
    val status: String = "pending", // pending | approved | rejected
    val ts: String = "",
    val approvedAt: String? = null,
    val approvedBy: String? = null,
    val rejectedAt: String? = null,
    val rejectedBy: String? = null,
) {
    @get:Exclude
    val isPending: Boolean get() = status == "pending"

    /** How the move reads in a list: "SITE-A → SITE-B", or just the destination if never aligned. */
    @get:Exclude
    val route: String get() = if (fromSite.isBlank()) "Unassigned → $toSite" else "$fromSite → $toSite"
}
