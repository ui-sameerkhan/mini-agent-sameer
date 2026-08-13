package com.lazyshopper.app.rider.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.lazyshopper.app.LazyShopperApp
import com.lazyshopper.app.core.data.remote.api.OrdersApi
import com.lazyshopper.app.core.data.remote.dto.LocationInput
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Runs only while the rider is toggled "online" on the Dashboard (see
 * `DeliveryDashboardScreen`'s `LaunchedEffect(state.online)`, which starts/stops this service
 * in lockstep with the toggle). Pushes a GPS fix to the server roughly every 20s so the
 * customer-facing order-tracking screen has a live rider position to render.
 *
 * The toggle's own state is re-derived from `GET /api/auth/me` (`user.available`) on every
 * Dashboard load, which is the actual server-side source of truth for "is this rider online" —
 * so even if the process (and this service with it) was killed while online, reopening the app
 * converges the toggle, and therefore this service, back to the correct state automatically.
 */
@AndroidEntryPoint
class LocationForegroundService : Service() {

    @Inject lateinit var ordersApi: OrdersApi

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        startLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, LazyShopperApp.CHANNEL_RIDER_LOCATION)
            .setContentTitle("You're online")
            .setContentText("Sharing your live location so customers can track their delivery")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationCallback != null) return // already running, nothing to do

        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, PUSH_INTERVAL_MS)
            .setMinUpdateIntervalMillis(PUSH_INTERVAL_MS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                serviceScope.launch {
                    runCatching { ordersApi.pushLocation(LocationInput(location.latitude, location.longitude)) }
                }
            }
        }
        locationCallback = callback
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    companion object {
        private const val NOTIFICATION_ID = 4201
        private const val PUSH_INTERVAL_MS = 20_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LocationForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationForegroundService::class.java))
        }
    }
}
