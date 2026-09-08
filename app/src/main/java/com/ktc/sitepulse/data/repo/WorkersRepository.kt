package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.Worker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WorkersRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("workers")

    /**
     * Live-subscribed list of every worker, sorted by sno client-side (matches the web
     * app's ordering intent, but without Firestore's orderBy() silently excluding any
     * document that happens to be missing the "sno" field from the results).
     */
    fun liveWorkers(): Flow<List<Worker>> =
        collection.asFlow().map { docs ->
            docs.mapNotNull { it.toObjectSafe(Worker::class.java, "workers") }.sortedBy { it.sno }
        }

    /**
     * Only the workers assigned to [siteCodes]. Required, not merely cheaper: security rules
     * reject an unconstrained collection query from a site-scoped user outright, so such a user
     * must ask per site or receive nothing at all. See mergePerSite().
     */
    fun liveWorkersForSites(siteCodes: List<String>): Flow<List<Worker>> =
        mergePerSite(siteCodes) { code ->
            collection.whereEqualTo("site", code).asFlow().map { docs ->
                docs.mapNotNull { it.toObjectSafe(Worker::class.java, "workers") }
            }
        }.map { it.sortedBy { w -> w.sno } }

    suspend fun findById(id: String): Worker? =
        collection.document(id).get().await().toObjectSafe(Worker::class.java, "workers")

    suspend fun isEmpty(): Boolean = collection.limit(1).get().await().isEmpty

    suspend fun nextSno(workers: List<Worker>): Long = (workers.maxOfOrNull { it.sno } ?: 0) + 1

    suspend fun saveWorker(worker: Worker) {
        collection.document(worker.id).set(worker, SetOptions.merge()).await()
    }

    suspend fun deleteWorker(id: String) {
        collection.document(id).delete().await()
    }

    suspend fun setStatus(id: String, status: String, leftDate: String?) {
        collection.document(id).set(mapOf("status" to status, "leftDate" to leftDate), SetOptions.merge()).await()
    }

    /** Batched upsert used by Excel/Outsource/Roster uploads and the one-time 999-worker seed. */
    suspend fun batchUpsert(workers: List<Worker>, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }) {
        var done = 0
        workers.chunked(Constants.FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { w -> batch.set(collection.document(w.id), w, SetOptions.merge()) }
            batch.commit().await()
            done += chunk.size
            onProgress(done, workers.size)
        }
    }

    suspend fun deleteAll(ids: List<String>, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }) {
        var done = 0
        ids.chunked(Constants.FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { id -> batch.delete(collection.document(id)) }
            batch.commit().await()
            done += chunk.size
            onProgress(done, ids.size)
        }
    }
}
