package com.ktc.sitepulse.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun Uri.displayName(context: Context): String {
    context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx) ?: "file"
    }
    return this.lastPathSegment ?: "file"
}
