package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ktc.sitepulse.data.model.StaffWorkerLink
import kotlinx.coroutines.tasks.await

/**
 * staffWorkerLinks/{email} — permanently binds an office-staff login email to
 * the first Worker ID it ever successfully checked in with. Doc ID is the
 * lowercased email so lookup/assignment is a single-document read/write.
 */
class StaffWorkerLinkRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("staffWorkerLinks")
    private fun docIdFor(email: String) = email.trim().lowercase()

    /** Returns the Worker ID this email is permanently bound to, or null if not yet assigned. */
    suspend fun getLinkedWorkerId(email: String): String? {
        val snap = collection.document(docIdFor(email)).get().await()
        return snap.toObjectSafe(StaffWorkerLink::class.java, "staffWorkerLinks")?.workerId?.ifBlank { null }
    }

    /**
     * Assigns [workerId] to [email] the first time it's called for that email; on every later
     * call it just returns the already-bound Worker ID, ignoring [workerId]. Runs inside a
     * transaction so two racing "first check-in" calls can't both win with different IDs.
     */
    suspend fun assignIfAbsent(email: String, workerId: String, assignedAt: String): String {
        val ref = collection.document(docIdFor(email))
        return db.runTransaction { txn ->
            val existing = txn.get(ref).toObject(StaffWorkerLink::class.java)?.workerId?.ifBlank { null }
            if (existing != null) {
                existing
            } else {
                txn.set(ref, StaffWorkerLink(email = email.trim().lowercase(), workerId = workerId, assignedAt = assignedAt))
                workerId
            }
        }.await()
    }
}
