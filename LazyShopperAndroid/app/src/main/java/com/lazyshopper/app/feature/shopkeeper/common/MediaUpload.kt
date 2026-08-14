package com.lazyshopper.app.feature.shopkeeper.common

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Reads a picked content [Uri] fully into memory and wraps it as a multipart "file" part for
 * `/api/kyc/upload`, `/api/upload` and `/api/upload-video`. Scoped to feature/shopkeeper/ (kept
 * as a local copy rather than importing another workstream's helper) so this package has no
 * compile-time dependency on files owned by a parallel agent.
 */
fun uriToMultipart(context: Context, uri: Uri, partName: String = "file"): MultipartBody.Part {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "image/jpeg"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    val ext = when {
        mime.contains("png") -> "png"
        mime.contains("webp") -> "webp"
        mime.contains("gif") -> "gif"
        mime.contains("mp4") -> "mp4"
        mime.contains("webm") -> "webm"
        mime.contains("quicktime") || mime.contains("mov") -> "mov"
        else -> "jpg"
    }
    val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, "upload_${System.currentTimeMillis()}.$ext", requestBody)
}

/** File size in MB, for the promo-video 60MB client-side pre-check (`/upload-video` also rejects server-side). */
fun fileSizeMb(context: Context, uri: Uri): Double {
    val afd = runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r") }.getOrNull()
    val length = afd?.use { it.length } ?: -1L
    return if (length <= 0) 0.0 else length / (1024.0 * 1024.0)
}
