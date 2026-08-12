package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class SettingsRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val docRef get() = db.collection("settings").document("adminPushToken")

    suspend fun saveAdminPushToken(token: String, updatedBy: String, updatedAt: String) {
        docRef.set(mapOf("token" to token, "updatedAt" to updatedAt, "updatedBy" to updatedBy), SetOptions.merge()).await()
    }

    suspend fun getAdminPushToken(): String? =
        (docRef.get().await().get("token") as? String)
}
