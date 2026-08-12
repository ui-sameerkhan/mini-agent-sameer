package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    /** All leave records for a worker (fetched on demand — used by the leave-aware Absent Report). */
    suspend fun forWorker(workerId: String): List<Leave> =
        collection.whereEqualTo("workerId", workerId).get().await().documents
            .mapNotNull { it.toObjectSafe(Leave::class.java, "leaves") }

    /** All leave records overlapping a date range, used by monthly Absent Report generation. */
    suspend fun all(): List<Leave> =
        collection.get().await().documents.mapNotNull { it.toObjectSafe(Leave::class.java, "leaves") }

    /** Admin-only live subscription to self-submitted leave applications awaiting a decision. */
    fun livePendingRequests(): Flow<List<Leave>> =
        collection.whereEqualTo("status", "pending").asFlow()
            .map { docs -> docs.mapNotNull { it.toObjectSafe(Leave::class.java, "leaves") } }

    /** An office staff member's own submitted leave applications (fetched on demand). */
    suspend fun forRequester(email: String): List<Leave> =
        collection.whereEqualTo("requestedBy", email).get().await().documents
            .mapNotNull { it.toObjectSafe(Leave::class.java, "leaves") }

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

    /** Only "approved" leave excludes a worker from the Absent Report — a pending request doesn't yet. */
    fun isOnLeave(leaves: List<Leave>, workerId: String, dateStr: String): Boolean =
        leaves.any {
            it.workerId == workerId && it.status == "approved" &&
                DateUtils.isWithin(dateStr, it.fromDate, it.toDate.ifBlank { it.fromDate })
        }
}
