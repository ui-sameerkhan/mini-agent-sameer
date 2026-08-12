package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ktc.sitepulse.data.model.Blocked
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class BlockedRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("blocked")

    /** Admin-only live subscription, scoped to the last 14 days to bound reads (matches web app). */
    fun liveLast14Days(): Flow<List<Blocked>> {
        val cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(14).format(DateTimeFormatter.ISO_LOCAL_DATE)
        return collection.whereGreaterThanOrEqualTo("date", cutoff).asFlow()
            .map { docs -> docs.mapNotNull { it.toObjectSafe(Blocked::class.java) } }
    }

    suspend fun log(blocked: Blocked) {
        runCatching { collection.add(blocked).await() } // best-effort, matches web app's swallow-errors behavior
    }
}
