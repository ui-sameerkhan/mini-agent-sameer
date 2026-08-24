package com.ktc.sitepulse.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.ktc.sitepulse.data.model.Worker
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

    /**
     * Lays out one QR badge per worker (name + ID printed underneath) across A4 pages,
     * 3x4 grid per page, and writes the result as a single printable PDF.
     */
    fun generateBulkPdf(context: Context, workers: List<Worker>): File {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 24
        val cols = 3
        val rows = 4
        val cellW = (pageWidth - margin * 2) / cols
        val cellH = (pageHeight - margin * 2) / rows
        val qrSize = (minOf(cellW, cellH) - 46).coerceAtLeast(40)

        val namePaint = Paint().apply { textAlign = Paint.Align.CENTER; textSize = 10f; color = Color.BLACK; isAntiAlias = true }
        val idPaint = Paint().apply { textAlign = Paint.Align.CENTER; textSize = 9f; color = Color.DKGRAY; isAntiAlias = true }

        val pdf = PdfDocument()
        var page: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var pageNumber = 0
        val perPage = cols * rows

        workers.forEachIndexed { index, w ->
            val posInPage = index % perPage
            if (posInPage == 0) {
                page?.let { pdf.finishPage(it) }
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page!!.canvas
            }
            val col = posInPage % cols
            val row = posInPage / cols
            val cellLeft = (margin + col * cellW).toFloat()
            val cellTop = (margin + row * cellH).toFloat()

            val qrLeft = cellLeft + (cellW - qrSize) / 2f
            val qrTop = cellTop + 8f
            val destRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
            canvas!!.drawBitmap(generate(w.id, 300), null, destRect, null)

            val centerX = cellLeft + cellW / 2f
            canvas!!.drawText(w.name.take(24), centerX, qrTop + qrSize + 15f, namePaint)
            canvas!!.drawText("ID ${w.id}", centerX, qrTop + qrSize + 29f, idPaint)
        }
        page?.let { pdf.finishPage(it) }

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "worker_qr_badges_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()
        return file
    }
}
