package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ktc.sitepulse.data.model.BiometricCheckLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BiometricCheckRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("biometricChecks")

    /** Records a completed run. Keyed by day and scope, so re-checking a day overwrites it. */
    suspend fun save(log: BiometricCheckLog) {
        collection.document(BiometricCheckLog.idFor(log.date, log.scope)).set(log).await()
    }

    /**
     * Whether a given day has been verified for this scope.
     *
     * Fetched by document id rather than by query on purpose: a direct get is one read, needs no
     * index, and — since the rules cannot scope a list of these to a caller — avoids relying on a
     * list query at all. See the note on /biometricChecks in firestore.rules.
     */
    suspend fun get(date: String, scope: String): BiometricCheckLog? =
        collection.document(BiometricCheckLog.idFor(date, scope)).get().await()
            .toObjectSafe(BiometricCheckLog::class.java, "biometricChecks")

    /** Live view of one day's check, so the screen updates the moment a run finishes. */
    fun live(date: String, scope: String): Flow<BiometricCheckLog?> =
        collection.document(BiometricCheckLog.idFor(date, scope)).asFlow()
            .map { it?.toObjectSafe(BiometricCheckLog::class.java, "biometricChecks") }

    /**
     * The last [days] days of checks for this scope, newest first — the "is this actually being
     * done every day?" view. Fetched by id per day rather than as a range query, for the same
     * reason as [get], and because a fortnight of ids is a trivial number of reads.
     */
    suspend fun recent(dates: List<String>, scope: String): List<BiometricCheckLog> =
        dates.mapNotNull { get(it, scope) }
}
