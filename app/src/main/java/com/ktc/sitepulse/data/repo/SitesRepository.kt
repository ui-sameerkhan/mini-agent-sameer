package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.Site
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SitesRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("sites")

    fun liveSites(): Flow<List<Site>> =
        collection.asFlow().map { docs -> docs.mapNotNull { it.toObjectSafe(Site::class.java, "sites") } }

    /**
     * Plain (non-merge) overwrite, matching the original saveSite(): the doc id is the
     * project code, so changing the code on "edit" creates a new site doc rather than
     * renaming — callers must delete the old code explicitly if renaming is intended.
     */
    suspend fun saveSite(site: Site) {
        collection.document(site.code).set(site).await()
    }

    suspend fun deleteSite(code: String) {
        collection.document(code).delete().await()
    }

    /** Batched upsert used by the full-backup restore flow. */
    suspend fun batchUpsert(sites: List<Site>) {
        sites.chunked(Constants.FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { s -> batch.set(collection.document(s.code), s) }
            batch.commit().await()
        }
    }
}
