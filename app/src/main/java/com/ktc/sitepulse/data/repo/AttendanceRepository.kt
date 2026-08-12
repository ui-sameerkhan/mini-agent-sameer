package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
            .map { docs -> docs.mapNotNull { it.toObject(Attendance::class.java) } }
    }

    suspend fun getForDate(date: String): List<Attendance> =
        collection.whereEqualTo("date", date).get().await().toObjects(Attendance::class.java)

    suspend fun getForMonth(monthStr: String): List<Attendance> {
        val start = DateUtils.monthStart(monthStr)
        val endExclusive = DateUtils.monthEndExclusive(monthStr)
        return collection
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThan("date", endExclusive)
            .get().await()
            .toObjects(Attendance::class.java)
    }

    suspend fun getRecord(date: String, workerId: String): Attendance? =
        collection.document(docId(date, workerId)).get().await().toObject(Attendance::class.java)

    suspend fun writeMark(date: String, workerId: String, fields: Map<String, Any?>) {
        collection.document(docId(date, workerId)).set(fields, SetOptions.merge()).await()
    }
}
