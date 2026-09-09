package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.ArrivalRequest
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Blocked
import com.ktc.sitepulse.data.model.Leave
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.Worker
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

/**
 * A single-tap, admin-only export of every collection SitePulse stores in Firestore, as one
 * plain .xlsx workbook. Firestore itself is a managed, replicated database — an app crash on
 * the phone can never touch it — but this gives admins an offline, human-readable copy they can
 * save to Drive/email on their own schedule, independent of the Firebase project itself.
 */
object BackupEngine {

    // Deliberately NOT java.awt.Color — see the identical note in ReportEngine.kt. Not shared
    // with it to avoid touching that already-verified, production-critical report path.
    private data class Rgb(val r: Int, val g: Int, val b: Int) {
        fun toBytes(): ByteArray = byteArrayOf(r.toByte(), g.toByte(), b.toByte())
    }

    private val DARK_BLUE = Rgb(0x00, 0x11, 0x3D)
    private val MID_BLUE = Rgb(0x0B, 0x66, 0xD6)
    private val LIGHT_BLUE = Rgb(0xE8, 0xF1, 0xFC)
    private val GOLD = Rgb(0xD4, 0xA3, 0x4A)
    private val INK = Rgb(0x14, 0x1F, 0x1A)
    private val GRAY = Rgb(0x5B, 0x6B, 0x63)

    fun generate(
        outputDir: File,
        workers: List<Worker>,
        sites: List<Site>,
        attendance: List<Attendance>,
        leaves: List<Leave>,
        blocked: List<Blocked>,
        arrivals: List<ArrivalRequest>,
        generatedBy: String,
    ): File {
        // In-memory, not streaming — see the note in ReportEngine.generate(): POI's streaming
        // workbook cannot create a sheet at all on Android.
        val wb = XSSFWorkbook()
        val styles = Styles(wb)
        val meta = listOf(
            "Generated: ${DateUtils.formatDateTime(DateUtils.nowIso())} · By: $generatedBy",
            "Full data backup — every record, all time",
        )

        addSheet(
            wb, styles, "WORKERS", "SITEPULSE — FULL BACKUP: WORKERS", meta,
            headers = listOf("SNo", "ID", "Name", "Designation", "Company", "Site", "Aligned Date", "Status", "Left Date", "Annual Leave Days"),
            rows = workers.sortedBy { it.sno }.map { w ->
                listOf(w.sno.toString(), w.id, w.name, w.designation, w.company ?: "", w.site ?: "", w.alignedDate ?: "", w.status, w.leftDate ?: "", w.annualLeaveDays.toString())
            },
        )

        addSheet(
            wb, styles, "SITES", "SITEPULSE — FULL BACKUP: SITES", meta,
            headers = listOf("Code", "Name", "Latitude", "Longitude", "Geofence Radius (m)", "WiFi SSID", "Night Start Hour", "Day Start Hour"),
            rows = sites.sortedBy { it.code }.map { s ->
                listOf(
                    s.code, s.name, s.lat.toString(), s.lng.toString(), s.radius.toString(), s.wifiSsid ?: "",
                    s.nightStartHour?.toString() ?: "", s.dayStartHour?.toString() ?: "",
                )
            },
        )

        addSheet(
            wb, styles, "ATTENDANCE", "SITEPULSE — FULL BACKUP: ATTENDANCE", meta,
            headers = listOf(
                "Date", "Site Code", "Site Name", "Worker ID", "Shift", "Check IN", "Check OUT",
                "IN Lat", "IN Lng", "OUT Lat", "OUT Lng", "IN Dist (m)", "OUT Dist (m)", "Marked Via", "Marked By", "Last Action",
                "Corrected", "Corrected By", "Corrected At",
            ),
            rows = attendance.sortedWith(compareBy({ it.date }, { it.siteCode }, { it.workerId })).map { a ->
                listOf(
                    a.date, a.siteCode, a.siteName, a.workerId, a.shift ?: "", a.checkIn ?: "", a.out ?: "",
                    a.inGps?.lat?.toString() ?: "", a.inGps?.lng?.toString() ?: "",
                    a.outGps?.lat?.toString() ?: "", a.outGps?.lng?.toString() ?: "",
                    a.inDist?.toString() ?: "", a.outDist?.toString() ?: "", a.markedVia, a.markedBy, a.lastAction,
                    if (a.corrected) "Yes" else "", a.correctedBy ?: "", a.correctedAt ?: "",
                )
            },
        )

        addSheet(
            wb, styles, "LEAVES", "SITEPULSE — FULL BACKUP: LEAVE RECORDS", meta,
            headers = listOf(
                "Doc ID", "Worker ID", "Site", "Leave Type", "From", "To", "Reason", "Status", "Marked By",
                "Requested By", "Approved By", "Approved At", "Rejected By", "Rejected At", "Timestamp",
            ),
            rows = leaves.sortedByDescending { it.ts }.map { l ->
                listOf(
                    l.docId, l.workerId, l.site, l.leaveType, l.fromDate, l.toDate, l.reason ?: "", l.status, l.markedBy,
                    l.requestedBy ?: "", l.approvedBy ?: "", l.approvedAt ?: "", l.rejectedBy ?: "", l.rejectedAt ?: "", l.ts,
                )
            },
        )

        addSheet(
            wb, styles, "BLOCKED ATTEMPTS", "SITEPULSE — FULL BACKUP: BLOCKED ATTEMPTS", meta,
            headers = listOf("Doc ID", "Date", "Time", "Worker ID", "Name", "Nearest Site", "Distance (m)", "Action", "GPS Lat", "GPS Lng", "GPS Accuracy (m)"),
            rows = blocked.sortedByDescending { it.date }.map { b ->
                listOf(
                    b.docId, b.date, b.time, b.workerId, b.name, b.nearestSite, b.distance.toString(), b.action,
                    b.gps?.lat?.toString() ?: "", b.gps?.lng?.toString() ?: "", b.gps?.acc?.toString() ?: "",
                )
            },
        )

        addSheet(
            wb, styles, "ARRIVAL REQUESTS", "SITEPULSE — FULL BACKUP: ARRIVAL REQUESTS", meta,
            headers = listOf(
                "Doc ID", "Site", "Worker ID", "Name", "Designation", "Requested Date", "Requested By",
                "Status", "Approved By", "Approved At", "Rejected By", "Rejected At", "Timestamp",
            ),
            rows = arrivals.sortedByDescending { it.ts }.map { r ->
                listOf(
                    r.docId, r.site, r.workerId, r.name, r.designation, r.requestedDate, r.requestedBy,
                    r.status, r.approvedBy ?: "", r.approvedAt ?: "", r.rejectedBy ?: "", r.rejectedAt ?: "", r.ts,
                )
            },
        )

        val outFile = File(outputDir, "SitePulse_FullBackup_${DateUtils.todayStrUtc()}.xlsx")
        try {
            FileOutputStream(outFile).use { wb.write(it) }
        } finally {
            wb.close()
        }
        return outFile
    }

    // ---- Styling (letterhead / header / zebra banding) — mirrors ReportEngine's, blue/gold branded ----

    private class Styles(wb: XSSFWorkbook) {
        val title: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { bold = true; fontHeightInPoints = 14.toShort(); color = IndexedColors.WHITE.index }
            setFont(f); fillColor(this, DARK_BLUE)
        }
        val subtitle: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { bold = true; italic = true; fontHeightInPoints = 11.toShort(); setColor(XSSFColor(GOLD.toBytes(), null)) }
            setFont(f); fillColor(this, DARK_BLUE)
        }
        val meta: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { fontHeightInPoints = 10.toShort(); setColor(XSSFColor(GRAY.toBytes(), null)) }
            setFont(f); fillColor(this, LIGHT_BLUE)
        }
        val header: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { bold = true; fontHeightInPoints = 11.toShort(); color = IndexedColors.WHITE.index }
            setFont(f); fillColor(this, MID_BLUE)
            alignment = HorizontalAlignment.CENTER
            allBorders(this)
        }
        val dataPlain: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { fontHeightInPoints = 11.toShort(); setColor(XSSFColor(INK.toBytes(), null)) }
            setFont(f); allBorders(this)
        }
        val dataBand: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { fontHeightInPoints = 11.toShort(); setColor(XSSFColor(INK.toBytes(), null)) }
            setFont(f); fillColor(this, LIGHT_BLUE); allBorders(this)
        }

        private fun fillColor(style: XSSFCellStyle, color: Rgb) {
            style.setFillForegroundColor(XSSFColor(color.toBytes(), null))
            style.fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        private fun allBorders(style: CellStyle) {
            style.borderTop = BorderStyle.THIN
            style.borderBottom = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
        }
    }

    private fun addSheet(
        wb: XSSFWorkbook, styles: Styles, sheetName: String,
        title: String, meta: List<String>, headers: List<String>, rows: List<List<String>>,
    ) {
        val sheet = wb.createSheet(sheetName)
        val lastCol = (headers.size - 1).coerceAtLeast(0)
        var r = 0

        fun letterheadRow(text: String, style: XSSFCellStyle, heightPt: Float) {
            val row = sheet.createRow(r)
            row.heightInPoints = heightPt
            for (c in 0..lastCol) row.createCell(c).cellStyle = style
            row.getCell(0).setCellValue(text)
            if (lastCol > 0) sheet.addMergedRegion(CellRangeAddress(r, r, 0, lastCol))
            r++
        }

        letterheadRow(title, styles.title, 26f)
        letterheadRow("KTC International Contracting LLC", styles.subtitle, 16f)
        meta.forEach { letterheadRow(it, styles.meta, 15f) }
        sheet.createRow(r).heightInPoints = 6f; r++ // spacer

        val headerRowIdx = r
        val headerRow = sheet.createRow(r)
        headers.forEachIndexed { c, h ->
            val cell = headerRow.createCell(c)
            cell.setCellValue(h)
            cell.cellStyle = styles.header
        }
        r++

        rows.forEachIndexed { i, dataRow ->
            val row = sheet.createRow(r)
            val style = if (i % 2 == 1) styles.dataBand else styles.dataPlain
            dataRow.forEachIndexed { c, v ->
                val cell = row.createCell(c)
                cell.setCellValue(v)
                cell.cellStyle = style
            }
            r++
        }

        if (rows.isNotEmpty()) {
            sheet.setAutoFilter(CellRangeAddress(headerRowIdx, r - 1, 0, lastCol))
        }
        sheet.createFreezePane(0, headerRowIdx + 1)

        headers.forEachIndexed { c, h ->
            val width = (h.length + 2).coerceIn(14, 28)
            sheet.setColumnWidth(c, width * 256)
        }
    }
}
