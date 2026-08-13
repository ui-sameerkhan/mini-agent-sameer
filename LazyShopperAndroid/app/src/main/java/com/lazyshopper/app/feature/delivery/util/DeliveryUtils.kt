package com.lazyshopper.app.feature.delivery.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume

/**
 * Reads a picked content [Uri] fully into memory and wraps it as a multipart "file" part for
 * the `/api/kyc/upload` and `/api/upload*` endpoints. KYC document photos are small enough
 * (compressed camera/gallery images) that a naive in-memory read is fine here.
 */
fun uriToMultipart(context: Context, uri: Uri, partName: String = "file"): MultipartBody.Part {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "image/jpeg"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    val ext = when {
        mime.contains("png") -> "png"
        mime.contains("webp") -> "webp"
        mime.contains("gif") -> "gif"
        else -> "jpg"
    }
    val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, "upload_${System.currentTimeMillis()}.$ext", requestBody)
}

/**
 * One-shot best-effort location fix: tries the last cached fix first, falls back to requesting
 * a fresh one. Returns null if location can't be resolved (permission missing, no fix, etc.) —
 * callers should treat that as "try again" rather than a hard error.
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
