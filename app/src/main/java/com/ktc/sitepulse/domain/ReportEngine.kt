package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.Attendance
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
 * Plain RGB triple — deliberately NOT java.awt.Color. That class isn't part of
 * the Android runtime (ART); it resolves at compile time via the desktop JDK
 * toolchain but throws NoClassDefFoundError on-device the moment it's touched.
 * XSSFColor's byte[]-RGB constructor lets us style cells without ever loading it.
 */
private data class Rgb(val r: Int, val g: Int, val b: Int) {
    fun toBytes(): ByteArray = byteArrayOf(r.toByte(), g.toByte(), b.toByte())
}

/**
 * Port of downloadAtt()/makeSheet() from the web app: multi-sheet Excel
 * attendance report with per-project sheets, a summary, a trade-wise summary,
 * and a leave-aware absent report (day or month mode). See spec section 8.
 */
object ReportEngine {

    data class Params(
        val dateOrMonth: String, // "YYYY-MM-DD" for day, "YYYY-MM" for month
        val range: String,       // "day" | "month"
        val siteScope: String,   // "ALL" or a site code
        val generatedBy: String,
    )

    private val DARK_GREEN = Rgb(0x0B, 0x4D, 0x3A)
    private val MID_GREEN = Rgb(0x17, 0x87, 0x5E)
    private val LIGHT_GREEN = Rgb(0xEA, 0xF3, 0xEE)
    private val GOLD = Rgb(0xD4, 0xA3, 0x4A)
    private val INK = Rgb(0x14, 0x1F, 0x1A)
    private val GRAY = Rgb(0x5B, 0x6B, 0x63)

    fun generate(
        outputDir: File,
        attendanceRows: List<Attendance>,
        workers: List<Worker>,
        sites: List<Site>,
        leaves: List<Leave>,
        params: Params,
    ): File {
        val workerById = workers.associateBy { it.id }
        val scoped = if (params.siteScope == "ALL") attendanceRows else attendanceRows.filter { it.siteCode == params.siteScope }
        val sorted = scoped.sortedWith(compareBy({ it.siteCode }, { it.date }, { it.workerId }))

        val expected = workers.filter {
            !it.site.isNullOrBlank() && it.status != "left" && (params.siteScope == "ALL" || it.site == params.siteScope)
        }

        val wb = XSSFWorkbook()
        val styles = Styles(wb)
        val metaLines = buildMetaLines(params)

        if (sorted.isEmpty() && expected.isEmpty()) {
            addSheet(
                wb, styles, "NO RECORDS",
                title = "SITEPULSE — NO CHECK-INS RECORDED", meta = metaLines,
                headers = listOf("Note"), rows = listOf(listOf("No check-ins recorded for this selection."))
            )
        } else {
            val exportHeaders = listOf(
                "Date", "Project Code", "Project Name", "Worker ID", "Worker Name", "Designation",
                "Assigned Site", "Company", "Shift", "Check IN", "Check OUT", "Hours", "IN Location",
                "OUT Location", "Dist from Site (m)", "Marked By", "ERP Aligned Site", "Site Match"
            )
            fun toRow(a: Attendance): List<String> {
                val w = workerById[a.workerId]
                val siteMatchLabel = when {
                    a.alignedSite == null -> ""
                    a.siteMismatch -> "⚠ DEVIATION"
                    else -> "Match"
                }
                return listOf(
                    a.date, a.siteCode, a.siteName, a.workerId, w?.name ?: "", w?.designation ?: "",
                    w?.site ?: "", w?.company?.ifBlank { null } ?: "KTC", a.shift ?: "",
                    DateUtils.formatTimeHm(a.checkIn), DateUtils.formatTimeHm(a.out), hoursLabel(a),
                    a.inGps?.let { "${it.lat}, ${it.lng}" } ?: "",
                    a.outGps?.let { "${it.lat}, ${it.lng}" } ?: "",
                    a.inDist?.toString() ?: "", a.markedBy, a.alignedSite ?: "", siteMatchLabel
                )
            }

            if (params.siteScope == "ALL") {
                sorted.groupBy { it.siteCode }.forEach { (code, rows) ->
                    addSheet(
                        wb, styles, sheetName(code),
                        title = "SITEPULSE — ATTENDANCE: $code", meta = metaLines,
                        headers = exportHeaders, rows = rows.map(::toRow)
                    )
                }
                val summaryRows = sorted.groupBy { it.siteCode }.map { (code, rows) ->
                    listOf(
                        code, rows.firstOrNull()?.siteName ?: "", rows.size.toString(),
                        rows.count { it.hasIn }.toString(), rows.count { it.hasOut }.toString()
                    )
                }
                if (summaryRows.isNotEmpty()) {
                    addSheet(
                        wb, styles, "SUMMARY", title = "SITEPULSE — PROJECT SUMMARY", meta = metaLines,
                        headers = listOf("Project Code", "Project Name", "Total Records", "Checked IN", "Checked OUT"),
                        rows = summaryRows
                    )
                }
                val tradeRows = tradeSummary(sorted, workerById, includeSiteColumn = true)
                if (tradeRows.isNotEmpty()) {
                    addSheet(
                        wb, styles, "TRADE SUMMARY", title = "SITEPULSE — TRADE-WISE SUMMARY (ALL PROJECTS)", meta = metaLines,
                        headers = listOf("Project Code", "Trade", "Present Count"), rows = tradeRows
                    )
                }
                if (params.range == "month") {
                    val hoursRows = monthlyHoursSummary(sorted, workerById, includeSiteColumn = true)
                    if (hoursRows.isNotEmpty()) {
                        addSheet(
                            wb, styles, "MONTHLY HOURS", title = "SITEPULSE — MONTHLY HOURS (ALL PROJECTS)", meta = metaLines,
                            headers = listOf("Project Code", "Worker ID", "Worker Name", "Designation", "Days Present", "Total Hours", "Avg Hours/Day"),
                            rows = hoursRows
                        )
                    }
                }
            } else if (sorted.isNotEmpty()) {
                addSheet(
                    wb, styles, sheetName(params.siteScope),
                    title = "SITEPULSE — ATTENDANCE: ${params.siteScope}", meta = metaLines,
                    headers = exportHeaders, rows = sorted.map(::toRow)
                )
                val tradeRows = tradeSummary(sorted, workerById, includeSiteColumn = false)
                if (tradeRows.isNotEmpty()) {
                    addSheet(
                        wb, styles, "TRADE SUMMARY", title = "SITEPULSE — TRADE-WISE SUMMARY: ${params.siteScope}", meta = metaLines,
                        headers = listOf("Trade", "Present Count"), rows = tradeRows
                    )
                }
                if (params.range == "month") {
                    val hoursRows = monthlyHoursSummary(sorted, workerById, includeSiteColumn = false)
                    if (hoursRows.isNotEmpty()) {
                        addSheet(
                            wb, styles, "MONTHLY HOURS", title = "SITEPULSE — MONTHLY HOURS: ${params.siteScope}", meta = metaLines,
                            headers = listOf("Worker ID", "Worker Name", "Designation", "Days Present", "Total Hours", "Avg Hours/Day"),
                            rows = hoursRows
                        )
                    }
                }
            }

            val absentRows = if (params.range == "day") {
                buildAbsentDay(params.dateOrMonth, expected, sorted, leaves)
            } else {
                buildAbsentMonth(params.dateOrMonth, expected, workerById, allAttendanceForMonth = attendanceRows, leaves)
            }
            if (absentRows.first.isNotEmpty()) {
                addSheet(
                    wb, styles, "ABSENT REPORT", title = "SITEPULSE — ABSENT REPORT", meta = metaLines,
                    headers = absentRows.second, rows = absentRows.first
                )
            }
        }

        val scopeLabel = if (params.siteScope == "ALL") "AllProjects" else params.siteScope
        val fileName = "SitePulse_${scopeLabel}_${params.dateOrMonth}.xlsx"
        val outFile = File(outputDir, fileName)
        FileOutputStream(outFile).use { wb.write(it) }
        wb.close()
        return outFile
    }

    private fun sheetName(code: String): String = code.take(31).ifBlank { "NA" }

    private fun hoursLabel(a: Attendance): String =
        DateUtils.hoursBetween(a.checkIn, a.out)?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: ""

    private fun buildMetaLines(p: Params): List<String> {
        val generated = "Generated: ${DateUtils.formatDateTime(DateUtils.nowIso())} · By: ${p.generatedBy}"
        val dateLine = if (p.range == "day") "Date: ${p.dateOrMonth}" else "Month: ${p.dateOrMonth}"
        val scopeLine = "Project Scope: " + if (p.siteScope == "ALL") "All Projects" else p.siteScope
        return listOf(generated, dateLine, scopeLine)
    }

    private fun tradeSummary(rows: List<Attendance>, workerById: Map<String, Worker>, includeSiteColumn: Boolean): List<List<String>> {
        val present = rows.filter { it.hasIn }
        return if (includeSiteColumn) {
            present.groupBy { it.siteCode to (workerById[it.workerId]?.designation ?: "") }
                .filter { it.key.second.isNotBlank() }
                .map { (k, v) -> listOf(k.first, k.second, v.size.toString()) }
                .sortedWith(compareBy({ it[0] }, { -it[2].toInt() }))
        } else {
            present.groupBy { workerById[it.workerId]?.designation ?: "" }
                .filter { it.key.isNotBlank() }
                .map { (trade, v) -> listOf(trade, v.size.toString()) }
                .sortedByDescending { it[1].toInt() }
        }
    }

    /** Total hours worked per worker per project for the export's date range — the "Days
     * Present" and "Total Hours" a worker logs against each project code they checked into.
     * A worker who covered two projects in the same month gets a row per project, which is
     * the point: hours are attributed to wherever they actually worked, not one home site. */
    private fun monthlyHoursSummary(rows: List<Attendance>, workerById: Map<String, Worker>, includeSiteColumn: Boolean): List<List<String>> {
        val present = rows.filter { it.hasIn }
        return present.groupBy { it.siteCode to it.workerId }
            .map { (key, recs) ->
                val (site, workerId) = key
                val w = workerById[workerId]
                val totalHours = recs.sumOf { DateUtils.hoursBetween(it.checkIn, it.out) ?: 0.0 }
                val daysPresent = recs.map { it.date }.distinct().size
                val avgHours = if (daysPresent > 0) totalHours / daysPresent else 0.0
                val base = listOf(
                    workerId, w?.name ?: "", w?.designation ?: "", daysPresent.toString(),
                    String.format(java.util.Locale.US, "%.2f", totalHours),
                    String.format(java.util.Locale.US, "%.2f", avgHours),
                )
                RowWithSort2(site, totalHours, if (includeSiteColumn) listOf(site) + base else base)
            }
            .sortedWith(compareBy({ it.site }, { -it.totalHours }))
            .map { it.row }
    }

    private data class RowWithSort2(val site: String, val totalHours: Double, val row: List<String>)

    private fun buildAbsentDay(
        date: String, expected: List<Worker>, dayRows: List<Attendance>, leaves: List<Leave>,
    ): Pair<List<List<String>>, List<String>> {
        val presentIds = dayRows.filter { it.hasIn }.map { it.workerId }.toSet()
        val headers = listOf("Date", "Project Code", "Worker ID", "Worker Name", "Designation", "Company", "Status", "Aligned Since")
        val rows = expected.filter { it.id !in presentIds }
            .sortedWith(compareBy({ it.site ?: "" }, { it.name }))
            .map { w ->
                val onLeave = leaves.any { it.workerId == w.id && it.status == "approved" && DateUtils.isWithin(date, it.fromDate, it.toDate.ifBlank { it.fromDate }) }
                listOf(
                    date, w.site ?: "", w.id, w.name, w.designation,
                    w.company?.ifBlank { null } ?: "KTC",
                    if (onLeave) "ON LEAVE" else "ABSENT",
                    w.alignedDate ?: ""
                )
            }
        return rows to headers
    }

    private fun buildAbsentMonth(
        monthStr: String, expected: List<Worker>, workerById: Map<String, Worker>,
        allAttendanceForMonth: List<Attendance>, leaves: List<Leave>,
    ): Pair<List<List<String>>, List<String>> {
        val elapsed = DateUtils.elapsedDaysFor(monthStr)
        val byWorker = allAttendanceForMonth.filter { it.hasIn }.groupBy { it.workerId }
        val headers = listOf("Project Code", "Worker ID", "Worker Name", "Designation", "Company", "Days Elapsed", "Days Present", "Days On Leave", "Days Absent", "Attendance %")
        val rows = expected.map { w ->
            val presentDates = (byWorker[w.id] ?: emptyList()).map { it.date }.distinct().size
            val leaveCount = (1..elapsed).count { day ->
                val dateStr = "%s-%02d".format(monthStr, day)
                leaves.any { it.workerId == w.id && it.status == "approved" && DateUtils.isWithin(dateStr, it.fromDate, it.toDate.ifBlank { it.fromDate }) }
            }
            val absent = maxOf(0, elapsed - presentDates - leaveCount)
            val pct = if (elapsed > 0) Math.round(presentDates * 100.0 / elapsed) else 0
            RowWithSort(
                absent,
                listOf(
                    w.site ?: "", w.id, w.name, w.designation, w.company?.ifBlank { null } ?: "KTC",
                    elapsed.toString(), presentDates.toString(), leaveCount.toString(), absent.toString(), "$pct%"
                )
            )
        }.sortedWith(compareBy({ -it.absent }, { it.row[0] })).map { it.row }
        return rows to headers
    }

    private data class RowWithSort(val absent: Int, val row: List<String>)

    // ---- Styling (letterhead / header / zebra banding) ----

    private class Styles(wb: XSSFWorkbook) {
        val title: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { bold = true; fontHeightInPoints = 14.toShort(); color = IndexedColors.WHITE.index }
            setFont(f); fillColor(this, DARK_GREEN)
        }
        val subtitle: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { bold = true; italic = true; fontHeightInPoints = 11.toShort(); setColor(XSSFColor(GOLD.toBytes(), null)) }
            setFont(f); fillColor(this, DARK_GREEN)
        }
        val meta: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { fontHeightInPoints = 10.toShort(); setColor(XSSFColor(GRAY.toBytes(), null)) }
            setFont(f); fillColor(this, LIGHT_GREEN)
        }
        val header: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { bold = true; fontHeightInPoints = 11.toShort(); color = IndexedColors.WHITE.index }
            setFont(f); fillColor(this, MID_GREEN)
            alignment = HorizontalAlignment.CENTER
            allBorders(this)
        }
        val dataPlain: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { fontHeightInPoints = 11.toShort(); setColor(XSSFColor(INK.toBytes(), null)) }
            setFont(f); allBorders(this)
        }
        val dataBand: XSSFCellStyle = wb.createCellStyle().apply {
            val f = wb.createFont().apply { fontHeightInPoints = 11.toShort(); setColor(XSSFColor(INK.toBytes(), null)) }
            setFont(f); fillColor(this, LIGHT_GREEN); allBorders(this)
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
        val safeName = wb.getSheet(sheetName)?.let { "$sheetName-${wb.numberOfSheets}" } ?: sheetName
        val sheet = wb.createSheet(safeName)
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
