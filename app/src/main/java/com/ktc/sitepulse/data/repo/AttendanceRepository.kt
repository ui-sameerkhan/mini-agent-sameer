package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.domain.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AttendanceRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("attendance")

    private fun docId(date: String, workerId: String) = "${date}_$workerId"

    /** Live-subscribed today's (UTC) attendance — the only date kept streamed, to bound Firestore reads. */
    fun liveToday(): Flow<List<Attendance>> {
        val today = DateUtils.todayStrUtc()
        return collection.whereEqualTo("date", today).asFlow()
            .map { docs -> docs.mapNotNull { it.toObjectSafe(Attendance::class.java, "attendance") } }
    }

    /** Today's attendance for the given sites only — see WorkersRepository.liveWorkersForSites. */
    fun liveTodayForSites(siteCodes: List<String>): Flow<List<Attendance>> {
        val today = DateUtils.todayStrUtc()
        return mergePerSite(siteCodes) { code ->
            collection.whereEqualTo("date", today).whereEqualTo("siteCode", code).asFlow()
                .map { docs -> docs.mapNotNull { it.toObjectSafe(Attendance::class.java, "attendance") } }
        }
    }

    suspend fun getForDateForSites(date: String, siteCodes: List<String>): List<Attendance> =
        siteCodes.distinct().flatMap { code ->
            collection.whereEqualTo("date", date).whereEqualTo("siteCode", code)
                .get().await().documents.mapNotNull { it.toObjectSafe(Attendance::class.java, "attendance") }
        }

    suspend fun getForDate(date: String): List<Attendance> =
        collection.whereEqualTo("date", date).get().await().documents
            .mapNotNull { it.toObjectSafe(Attendance::class.java, "attendance") }

    /** Every attendance record ever written — used only by the admin-only full data backup export. */
    suspend fun getAll(): List<Attendance> =
        collection.get().await().documents.mapNotNull { it.toObjectSafe(Attendance::class.java, "attendance") }

    suspend fun getForMonth(monthStr: String): List<Attendance> {
        val start = DateUtils.monthStart(monthStr)
        val endExclusive = DateUtils.monthEndExclusive(monthStr)
        return collection
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThan("date", endExclusive)
            .get().await()
            .documents
            .mapNotNull { it.toObjectSafe(Attendance::class.java, "attendance") }
    }

    suspend fun getRecord(date: String, workerId: String): Attendance? =
        collection.document(docId(date, workerId)).get().await().toObjectSafe(Attendance::class.java, "attendance")

    /**
     * "My Attendance History" for office staff: records they personally marked (i.e. punched
     * themselves in/out). A single-field equality query — sorted client-side rather than via
     * Firestore orderBy, to avoid needing a composite index for markedBy+date.
     */
    suspend fun getMarkedBy(email: String, limit: Int = 60): List<Attendance> =
        collection.whereEqualTo("markedBy", email).get().await().documents
            .mapNotNull { it.toObjectSafe(Attendance::class.java, "attendance") }
            .sortedByDescending { it.lastAction }
            .take(limit)

    suspend fun writeMark(date: String, workerId: String, fields: Map<String, Any?>) {
        collection.document(docId(date, workerId)).set(fields, SetOptions.merge()).await()
    }

    /** Bulk manual-correction write — same merge semantics as writeMark, applied to many workers
     * at once for the same date (e.g. backfilling a whole crew for a day the app wasn't used). */
    suspend fun bulkWriteMark(date: String, workerIds: List<String>, commonFields: Map<String, Any?>) {
        workerIds.chunked(Constants.FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { workerId ->
                val fields = commonFields + mapOf("workerId" to workerId, "date" to date)
                batch.set(collection.document(docId(date, workerId)), fields, SetOptions.merge())
            }
            batch.commit().await()
        }
    }

    /** Batched upsert used by the full-backup restore flow — merges onto whatever's already there. */
    suspend fun batchUpsert(records: List<Attendance>) {
        records.chunked(Constants.FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { a -> batch.set(collection.document(docId(a.date, a.workerId)), a, SetOptions.merge()) }
            batch.commit().await()
        }
    }
}
