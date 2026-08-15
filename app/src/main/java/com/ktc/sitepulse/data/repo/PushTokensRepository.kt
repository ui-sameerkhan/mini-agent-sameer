package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Registry of one FCM token per signed-in device, keyed by lowercased login email
 * (pushTokens/{email}). Used to fan out an admin announcement to every opted-in
 * device via the existing single-recipient NetlifyApi.sendPush() — see
 * SitePulseViewModel.sendAnnouncement().
 */
class PushTokensRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("pushTokens")

    suspend fun register(email: String, token: String, updatedAt: String) {
        val id = email.lowercase()
        collection.document(id).set(
            mapOf("email" to id, "token" to token, "updatedAt" to updatedAt),
            SetOptions.merge()
        ).await()
    }

    /** Every registered device token, deduplicated — admin-only, used only for broadcast fan-out. */
    suspend fun allTokens(): List<String> =
        collection.get().await().documents.mapNotNull { it.getString("token") }.distinct()
}
