package com.ktc.sitepulse.data.repo

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ktc.sitepulse.domain.LatLng
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

data class GpsFix(val lat: Double, val lng: Double, val accuracyM: Float)

open class GpsException(message: String) : Exception(message)

/** A fake-GPS app (or a rooted/dev-mode device set to a mock location provider) reported this
 * fix — Android flags this itself via Location.isFromMockProvider(). Since the whole point of
 * geofenced check-in is a GPS fix nobody can fake, this is treated as its own rejection rather
 * than a generic GPS failure, and logged for admin review the same way an outside-geofence
 * attempt is (see AttendanceEngine.mark()). */
class MockLocationException(message: String) : GpsException(message)

/**
 * Mirrors the web app's getGPS(): a high-accuracy attempt first (accepting a
 * fix up to 3 minutes old so a whole crew can check in quickly off one warm
 * fix), falling back to a lower-accuracy attempt on failure/timeout.
 */
class LocationProvider(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentFix(): GpsFix {
        try {
            return attempt(Priority.PRIORITY_HIGH_ACCURACY, timeoutMs = 10_000, maxAgeMs = 180_000)
        } catch (e: CancellationException) {
            throw e
        } catch (e: MockLocationException) {
            throw e // no point retrying at lower accuracy — the device itself is spoofed
        } catch (e: Exception) {
            // fall through to low-accuracy retry, matching the web app's two-step attempt
        }
        try {
            return attempt(Priority.PRIORITY_BALANCED_POWER_ACCURACY, timeoutMs = 8_000, maxAgeMs = 180_000)
        } catch (e: TimeoutCancellationException) {
            throw GpsException("Location request timed out")
        } catch (e: SecurityException) {
            throw GpsException("GPS denied")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun attempt(priority: Int, timeoutMs: Long, maxAgeMs: Long): GpsFix {
        // A recent cached fix satisfies the "maximumAge" allowance the web app uses.
        val last = runCatching { client.lastLocation.await() }.getOrNull()
        if (last != null && System.currentTimeMillis() - last.time <= maxAgeMs) {
            return last.toFix()
        }
        val request = CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setMaxUpdateAgeMillis(maxAgeMs)
            .build()
        val location: Location = withTimeout(timeoutMs) {
            client.getCurrentLocation(request, null).await()
        } ?: throw GpsException("GPS not supported")
        return location.toFix()
    }

    private fun Location.toFix(): GpsFix {
        if (isFromMockProvider) {
            throw MockLocationException("Mock/fake GPS detected on this device — attendance cannot be verified from a spoofed location.")
        }
        return GpsFix(latitude, longitude, accuracy)
    }
}

fun GpsFix.toLatLng() = LatLng(lat, lng)
