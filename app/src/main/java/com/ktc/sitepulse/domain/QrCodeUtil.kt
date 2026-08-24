package com.ktc.sitepulse.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

/** Generates a printable QR code image for a worker's ID, for badges that don't have one yet. */
object QrCodeUtil {

    fun generate(content: String, sizePx: Int = 800): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /** Writes the QR bitmap to the shared "qr" cache folder so it can be shared via FileProvider. */
    fun saveToCache(context: Context, bitmap: Bitmap, workerId: String): File {
        val dir = File(context.cacheDir, "qr").apply { mkdirs() }
        val file = File(dir, "worker_${workerId}_qr.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file
    }
}
