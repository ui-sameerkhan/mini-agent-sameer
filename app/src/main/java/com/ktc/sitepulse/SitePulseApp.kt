package com.ktc.sitepulse

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.initialize
import com.ktc.sitepulse.util.CrashReporter

/**
 * Equivalent of the web app's:
 *   initializeFirestore(app, { localCache: persistentLocalCache({ tabManager: persistentSingleTabManager() }) })
 * The Firestore Android SDK persists to disk and queues offline writes for
 * automatic sync on reconnect by default; we just make that explicit and
 * unbounded (matching the browser's IndexedDB-backed cache).
 */
class SitePulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        Firebase.initialize(this)

        Firebase.firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(persistentCacheSettings {})
            .build()

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.notification_channel_id),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
