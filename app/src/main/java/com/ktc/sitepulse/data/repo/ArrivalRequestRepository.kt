package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ktc.sitepulse.data.model.ArrivalRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ArrivalRequestRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("arrivalRequests")

    /** Admin-only live subscription to pending requests. */
    fun livePending(): Flow<List<ArrivalRequest>> =
        collection.whereEqualTo("status", "pending").asFlow()
            .map { docs -> docs.mapNotNull { it.toObjectSafe(ArrivalRequest::class.java) } }

    suspend fun submit(request: ArrivalRequest): String {
        val ref = collection.add(request).await()
        return ref.id
    }

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
}
