package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.Holiday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HolidayRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("holidays")

    /** Batched, idempotent — merges by date, so re-seeding after an official date changes just
     * updates the name/that one entry rather than duplicating anything. */
    suspend fun bulkAdd(holidays: List<Holiday>) {
        holidays.chunked(Constants.FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { h -> batch.set(collection.document(h.date), h, SetOptions.merge()) }
            batch.commit().await()
        }
    }

    fun live(): Flow<List<Holiday>> =
        collection.asFlow().map { docs -> docs.mapNotNull { it.toObjectSafe(Holiday::class.java, "holidays") }.sortedBy { it.date } }

    suspend fun all(): List<Holiday> =
        collection.get().await().documents.mapNotNull { it.toObjectSafe(Holiday::class.java, "holidays") }

    suspend fun add(date: String, name: String, addedBy: String, addedAt: String) {
        collection.document(date).set(Holiday(date, name, addedBy, addedAt)).await()
    }

    suspend fun delete(date: String) {
        collection.document(date).delete().await()
    }
}
