package com.lazyshopper.app.feature.admin.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * Copies a content:// image Uri picked via the system picker into a temp cache file and wraps it
 * as a Retrofit multipart Part, matching ProductsApi.uploadImage's `@Part file` signature.
 */
fun uriToImagePart(context: Context, uri: Uri, partName: String = "file"): MultipartBody.Part? {
    return runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        var displayName = "upload.jpg"
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) displayName = cursor.getString(idx) ?: displayName
        }
        val tempFile = File.createTempFile("admin_upload_", "_$displayName", context.cacheDir)
        resolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        val body = tempFile.asRequestBody(mime.toMediaTypeOrNull())
        MultipartBody.Part.createFormData(partName, displayName, body)
    }.getOrNull()
}
