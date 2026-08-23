package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.Leave
import com.ktc.sitepulse.domain.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LeaveRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("leaves")

    suspend fun add(leave: Leave) {
        collection.add(leave).await()
    }

    /** Admin marking a whole crew on leave at once (rain day, public holiday, site shutdown) —
     * one new "approved" leave doc per worker, in a single batch. */
    suspend fun bulkAdd(leaves: List<Leave>) {
        leaves.chunked(Constants.FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { l -> batch.set(collection.document(), l) }
            batch.commit().await()
        }
    }

    /**
     * Firestore's @DocumentId annotation is supposed to auto-populate Leave.docId from the
     * snapshot on toObject(), but that mapping proved unreliable in practice — approve/reject/
     * delete were failing with a blank ID even on freshly-created requests. Setting it explicitly
     * from the snapshot's real ID here removes any dependence on that annotation working at all.
     */
    private fun DocumentSnapshot.toLeave(): Leave? =
        toObjectSafe(Leave::class.java, "leaves")?.copy(docId = id)

    /** All leave records for a worker (fetched on demand — used by the leave-aware Absent Report). */
    suspend fun forWorker(workerId: String): List<Leave> =
        collection.whereEqualTo("workerId", workerId).get().await().documents.mapNotNull { it.toLeave() }

    /** All leave records overlapping a date range, used by monthly Absent Report generation. */
    suspend fun all(): List<Leave> =
        collection.get().await().documents.mapNotNull { it.toLeave() }

    /** Admin-only live subscription to self-submitted leave applications awaiting a decision. */
    fun livePendingRequests(): Flow<List<Leave>> =
        collection.whereEqualTo("status", "pending").asFlow()
            .map { docs -> docs.mapNotNull { it.toLeave() } }

    /** An office staff member's own submitted leave applications (fetched on demand). */
    suspend fun forRequester(email: String): List<Leave> =
        collection.whereEqualTo("requestedBy", email).get().await().documents.mapNotNull { it.toLeave() }

    suspend fun approve(id: String, approvedBy: String, approvedAt: String) {
        collection.document(id).set(
            mapOf("status" to "approved", "approvedBy" to approvedBy, "approvedAt" to approvedAt),
            SetOptions.merge()
        ).await()
    }

    suspend fun reject(id: String, rejectedBy: String, rejectedAt: String) {
        collection.document(id).set(
            mapOf("status" to "rejected", "rejectedBy" to rejectedBy, "rejectedAt" to rejectedAt),
            SetOptions.merge()
        ).await()
    }

    suspend fun delete(id: String) {
        collection.document(id).delete().await()
    }

    /**
     * Batched upsert used by the full-backup restore flow. A record with a Doc ID (from a
     * previously-downloaded backup) overwrites that exact document via merge; one without an
     * ID (e.g. a row someone added by hand) becomes a brand-new leave — this keeps re-uploading
     * the same backup file idempotent instead of duplicating every leave on every restore.
     */
    suspend fun restoreAll(leaves: List<Leave>) {
        leaves.chunked(Constants.FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { l ->
                val ref = if (l.docId.isBlank()) collection.document() else collection.document(l.docId)
                batch.set(ref, l, SetOptions.merge())
            }
            batch.commit().await()
        }
    }

    /** Only "approved" leave excludes a worker from the Absent Report — a pending request doesn't yet. */
    fun isOnLeave(leaves: List<Leave>, workerId: String, dateStr: String): Boolean =
        leaves.any {
            it.workerId == workerId && it.status == "approved" &&
                DateUtils.isWithin(dateStr, it.fromDate, it.toDate.ifBlank { it.fromDate })
        }
}
