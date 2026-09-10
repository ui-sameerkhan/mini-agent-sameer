package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.TransferRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TransferRequestRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("transferRequests")

    // See LeaveRepository.toLeave() — @DocumentId's automatic population proved unreliable for
    // approve/reject, so the real snapshot ID is set explicitly instead of trusting the annotation.
    private fun DocumentSnapshot.toTransfer(): TransferRequest? =
        toObjectSafe(TransferRequest::class.java, "transferRequests")?.copy(docId = id)

    /** Every pending transfer, company-wide — only for users who hold all sites. */
    fun livePending(): Flow<List<TransferRequest>> =
        collection.whereEqualTo("status", "pending").asFlow()
            .map { docs -> docs.mapNotNull { it.toTransfer() } }

    /**
     * Pending transfers a site-scoped user may act on: the ones coming INTO a site they hold.
     *
     * Queried on toSite alone rather than "toSite or fromSite" because Firestore rules are not
     * filters — an unconstrained or partly-constrained query from a scoped account is refused
     * outright, not trimmed. Outgoing transfers are surfaced separately by [liveOutgoingForSites]
     * so each query stays a single equality the rules can satisfy for every document it returns.
     */
    fun livePendingForSites(siteCodes: List<String>): Flow<List<TransferRequest>> =
        mergePerSite(siteCodes) { code ->
            collection.whereEqualTo("status", "pending").whereEqualTo("toSite", code).asFlow()
                .map { docs -> docs.mapNotNull { it.toTransfer() } }
        }

    /**
     * Pending transfers taking a worker AWAY from a site the user holds. The losing site can't
     * approve or block the move — the man is already standing at the other gate — but it must be
     * able to see that its headcount is about to change.
     */
    fun liveOutgoingForSites(siteCodes: List<String>): Flow<List<TransferRequest>> =
        mergePerSite(siteCodes) { code ->
            collection.whereEqualTo("status", "pending").whereEqualTo("fromSite", code).asFlow()
                .map { docs -> docs.mapNotNull { it.toTransfer() } }
        }

    suspend fun submit(request: TransferRequest): String =
        collection.add(request).await().id

    /**
     * A pending request for this worker into this site, if one already exists. Two foremen
     * noticing the same man on the same morning should not produce two cards for the timekeeper.
     */
    suspend fun findPending(workerId: String, toSite: String): TransferRequest? =
        collection.whereEqualTo("workerId", workerId)
            .whereEqualTo("toSite", toSite)
            .whereEqualTo("status", "pending")
            .get().await().documents.firstNotNullOfOrNull { it.toTransfer() }

    suspend fun approve(reqId: String, approvedBy: String, approvedAt: String) {
        collection.document(reqId).set(
            mapOf("status" to "approved", "approvedAt" to approvedAt, "approvedBy" to approvedBy),
            SetOptions.merge()
        ).await()
    }

    suspend fun reject(reqId: String, rejectedBy: String, rejectedAt: String) {
        collection.document(reqId).set(
            mapOf("status" to "rejected", "rejectedAt" to rejectedAt, "rejectedBy" to rejectedBy),
            SetOptions.merge()
        ).await()
    }

    /** Every transfer ever raised — used only by the admin-only full data backup export. */
    suspend fun all(): List<TransferRequest> =
        collection.get().await().documents.mapNotNull { it.toTransfer() }

    /** Batched upsert for the full-backup restore flow — see LeaveRepository.restoreAll. */
    suspend fun restoreAll(records: List<TransferRequest>) {
        records.chunked(Constants.FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { t ->
                val ref = if (t.docId.isBlank()) collection.document() else collection.document(t.docId)
                batch.set(ref, t, SetOptions.merge())
            }
            batch.commit().await()
        }
    }
}
