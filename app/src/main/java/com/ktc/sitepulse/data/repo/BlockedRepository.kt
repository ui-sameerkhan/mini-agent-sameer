package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.Blocked
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class BlockedRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("blocked")

    // See LeaveRepository.toLeave() — @DocumentId's automatic population proved unreliable, so
    // the real snapshot ID is set explicitly instead of trusting the annotation.
    private fun DocumentSnapshot.toBlocked(): Blocked? =
        toObjectSafe(Blocked::class.java, "blocked")?.copy(docId = id)

    /** Admin-only live subscription, scoped to the last 14 days to bound reads (matches web app). */
    fun liveLast14Days(): Flow<List<Blocked>> {
        val cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(14).format(DateTimeFormatter.ISO_LOCAL_DATE)
        return collection.whereGreaterThanOrEqualTo("date", cutoff).asFlow()
            .map { docs -> docs.mapNotNull { it.toBlocked() } }
    }

    /**
     * Blocked attempts for the given sites only — required for site-scoped users, whose
     * unconstrained queries the rules reject. Scoped by siteCode equality alone and windowed to
     * the last 14 days client-side: combining an equality clause with a range clause on a
     * different field would need a hand-deployed composite index, and this collection is small.
     */
    fun liveLast14DaysForSites(siteCodes: List<String>): Flow<List<Blocked>> {
        val cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(14).format(DateTimeFormatter.ISO_LOCAL_DATE)
        return mergePerSite(siteCodes) { code ->
            collection.whereEqualTo("siteCode", code).asFlow()
                .map { docs -> docs.mapNotNull { it.toBlocked() }.filter { it.date >= cutoff } }
        }
    }

    suspend fun log(blocked: Blocked) {
        runCatching { collection.add(blocked).await() } // best-effort, matches web app's swallow-errors behavior
    }

    /** Every blocked-attempt record ever logged — used only by the admin-only full data backup export. */
    suspend fun all(): List<Blocked> =
        collection.get().await().documents.mapNotNull { it.toBlocked() }

    /** Batched upsert for the full-backup restore flow — see LeaveRepository.restoreAll for the Doc ID logic. */
    suspend fun restoreAll(records: List<Blocked>) {
        records.chunked(Constants.FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { b ->
                val ref = if (b.docId.isBlank()) collection.document() else collection.document(b.docId)
                batch.set(ref, b, SetOptions.merge())
            }
            batch.commit().await()
        }
    }
}
