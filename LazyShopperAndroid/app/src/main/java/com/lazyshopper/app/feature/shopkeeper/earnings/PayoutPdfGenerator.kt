package com.lazyshopper.app.feature.shopkeeper.earnings

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.lazyshopper.app.core.data.remote.dto.Settlement
import com.lazyshopper.app.feature.shopkeeper.common.money
import java.io.File
import java.io.FileOutputStream

/**
 * Generates a single-page, plain-text payout statement PDF for one settlement row and hands it
 * off via `ACTION_SEND` (share sheet) through the app's `FileProvider`. No fancy styling needed —
 * this is a receipt, not a report.
 */
object PayoutPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f

    fun buildAndShare(context: Context, settlement: Settlement) {
        val file = build(context, settlement)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Payout statement — ${settlement.id}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share payout statement"))
    }

    private fun build(context: Context, settlement: Settlement): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val labelPaint = Paint().apply { textSize = 11f; color = 0xFF666666.toInt() }
        val valuePaint = Paint().apply { textSize = 13f }
        val bigPaint = Paint().apply { textSize = 18f; isFakeBoldText = true }

        var y = MARGIN + 10f
        canvas.drawText("Lazy Shopper — Payout Statement", MARGIN, y, titlePaint)
        y += 30f

        fun row(label: String, value: String) {
            canvas.drawText(label, MARGIN, y, labelPaint)
            y += 16f
            canvas.drawText(value, MARGIN, y, valuePaint)
            y += 26f
        }

        row("Settlement ID", settlement.id)
        row("Shop", settlement.shop_name ?: "—")
        row("Shopkeeper", settlement.shopkeeper_name ?: "—")
        row("Order ID", settlement.order_id ?: "—")
        row("Settled at", settlement.settled_at ?: "—")
        row("Payout status", settlement.payout_status ?: "—")

        y += 10f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, labelPaint)
        y += 28f

        canvas.drawText("Platform commission", MARGIN, y, labelPaint)
        canvas.drawText(money(settlement.platform_amount), PAGE_WIDTH - MARGIN - 120f, y, valuePaint)
        y += 26f

        canvas.drawText("Amount paid out", MARGIN, y, labelPaint)
        canvas.drawText(money(settlement.amount), PAGE_WIDTH - MARGIN - 120f, y, bigPaint)
        y += 40f

        canvas.drawText(
            "This is a system-generated statement and does not require a signature.",
            MARGIN,
            PAGE_HEIGHT - MARGIN,
            labelPaint,
        )

        document.finishPage(page)

        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "payout_${settlement.id}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }
}
