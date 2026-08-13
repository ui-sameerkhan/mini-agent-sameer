package com.lazyshopper.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LazyShopperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ORDERS,
                "Order updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RIDER_LOCATION,
                "Delivery tracking",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CHAT,
                "Chat messages",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    companion object {
        const val CHANNEL_ORDERS = "orders"
        const val CHANNEL_RIDER_LOCATION = "rider_location"
        const val CHANNEL_CHAT = "chat"
    }
}
