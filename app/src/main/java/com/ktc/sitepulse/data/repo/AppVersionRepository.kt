package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ktc.sitepulse.data.model.AppVersionGate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Same settings collection/permissions as SettingsRepository's adminPushToken doc — read by
 * any signed-in user, written by admin only. See AppVersionGate for what this enforces. */
class AppVersionRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val docRef get() = db.collection("settings").document("appVersion")

    fun live(): Flow<AppVersionGate?> =
        docRef.asFlow().map { snap -> snap?.toObjectSafe(AppVersionGate::class.java, "appVersion") }

    suspend fun save(minVersionCode: Long, updateUrl: String, message: String) {
        docRef.set(
            mapOf("minVersionCode" to minVersionCode, "updateUrl" to updateUrl, "message" to message),
            SetOptions.merge()
        ).await()
    }
}
