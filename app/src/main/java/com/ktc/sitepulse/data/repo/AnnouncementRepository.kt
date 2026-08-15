package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ktc.sitepulse.data.model.Announcement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AnnouncementRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("announcements")

    private fun DocumentSnapshot.toAnnouncement(): Announcement? =
        toObjectSafe(Announcement::class.java, "announcements")?.copy(docId = id)

    /** Live subscription to only the most recent broadcast — shown as a dismissible banner app-wide. */
    fun latest(): Flow<Announcement?> =
        collection.orderBy("sentAt", Query.Direction.DESCENDING).limit(1).asFlow()
            .map { docs -> docs.firstOrNull()?.toAnnouncement() }

    suspend fun send(message: String, sentBy: String, sentAt: String) {
        collection.add(Announcement(message = message, sentBy = sentBy, sentAt = sentAt)).await()
    }
}
