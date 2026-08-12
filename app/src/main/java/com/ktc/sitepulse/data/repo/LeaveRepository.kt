package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ktc.sitepulse.data.model.Leave
import com.ktc.sitepulse.domain.DateUtils
import kotlinx.coroutines.tasks.await

class LeaveRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("leaves")

    suspend fun add(leave: Leave) {
        collection.add(leave).await()
    }

    /** All leave records for a worker (fetched on demand — used by the leave-aware Absent Report). */
    suspend fun forWorker(workerId: String): List<Leave> =
        collection.whereEqualTo("workerId", workerId).get().await().documents
            .mapNotNull { it.toObjectSafe(Leave::class.java) }

    /** All leave records overlapping a date range, used by monthly Absent Report generation. */
    suspend fun all(): List<Leave> =
        collection.get().await().documents.mapNotNull { it.toObjectSafe(Leave::class.java) }

    fun isOnLeave(leaves: List<Leave>, workerId: String, dateStr: String): Boolean =
        leaves.any { it.workerId == workerId && DateUtils.isWithin(dateStr, it.fromDate, it.toDate.ifBlank { it.fromDate }) }
}
