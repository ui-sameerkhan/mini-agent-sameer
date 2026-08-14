package com.lazyshopper.app.feature.shopkeeper.common

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * One-shot best-effort location fix: tries the last cached fix first, falls back to requesting
 * a fresh one. Returns null if location can't be resolved (permission missing, no fix, etc.) —
 * callers should treat that as "try again" rather than a hard error. Kept as a local copy (rather
 * than importing feature/delivery's equivalent) so this package has no compile-time dependency on
 * a parallel workstream's files.
 */
@SuppressLint("MissingPermission")
suspend fun fetchCurrentLocation(context: Context): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.lastLocation
        .addOnSuccessListener { loc ->
            if (loc != null) {
                if (cont.isActive) cont.resume(loc.latitude to loc.longitude)
            } else {
                val cts = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { fresh ->
                        if (cont.isActive) cont.resume(fresh?.let { it.latitude to it.longitude })
                    }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }
        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
}
