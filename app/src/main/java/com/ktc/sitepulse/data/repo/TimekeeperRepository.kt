package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ktc.sitepulse.data.model.Timekeeper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TimekeeperRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("timekeepers")

    fun live(): Flow<List<Timekeeper>> =
        collection.asFlow().map { docs -> docs.mapNotNull { it.toObjectSafe(Timekeeper::class.java, "timekeepers") }.sortedBy { it.email } }

    /** Cheap existence check used once per login to resolve a non-admin account's role. */
    suspend fun isTimekeeper(email: String): Boolean {
        if (email.isBlank()) return false
        return collection.document(email.lowercase()).get().await().exists()
    }

    suspend fun add(email: String, addedBy: String, addedAt: String) {
        val lower = email.trim().lowercase()
        collection.document(lower).set(Timekeeper(email = lower, addedBy = addedBy, addedAt = addedAt)).await()
    }

    suspend fun delete(email: String) {
        collection.document(email.lowercase()).delete().await()
    }
}
