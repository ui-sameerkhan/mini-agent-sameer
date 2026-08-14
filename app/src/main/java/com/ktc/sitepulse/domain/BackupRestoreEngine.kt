package com.ktc.sitepulse.domain

import android.content.Context
import android.net.Uri
import com.ktc.sitepulse.data.model.ArrivalRequest
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Blocked
import com.ktc.sitepulse.data.model.BlockedGps
import com.ktc.sitepulse.data.model.GpsPoint
import com.ktc.sitepulse.data.model.Leave
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.Worker
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class BackupParseException(message: String) : Exception(message)

data class BackupRestoreResult(
    val workers: List<Worker> = emptyList(),
    val sites: List<Site> = emptyList(),
    val attendance: List<Attendance> = emptyList(),
    val leaves: List<Leave> = emptyList(),
    val blocked: List<Blocked> = emptyList(),
    val arrivals: List<ArrivalRequest> = emptyList(),
) {
    val isEmpty: Boolean get() =
        workers.isEmpty() && sites.isEmpty() && attendance.isEmpty() && leaves.isEmpty() && blocked.isEmpty() && arrivals.isEmpty()
}

/**
 * Parses a workbook produced by BackupEngine back into model objects, ready to upsert into
 * Firestore. Every sheet is optional (a file with only some sheets restores only that data) —
 * headers are matched by name rather than fixed column position, so a manually-edited backup
 * (columns reordered, extra columns added) still restores correctly as long as the header text
 * is unchanged.
 */
object BackupRestoreEngine {

    fun parse(context: Context, uri: Uri): BackupRestoreResult {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open file" }
            WorkbookFactory.create(input).use { wb ->
                if (wb !is XSSFWorkbook) throw BackupParseException("Not a valid SitePulse backup file (.xlsx)")

                val workers = readSheet(wb, "WORKERS", WORKER_HEADERS)?.mapNotNull { toWorker(it) } ?: emptyList()
                val sites = readSheet(wb, "SITES", SITE_HEADERS)?.mapNotNull { toSite(it) } ?: emptyList()
                val attendance = readSheet(wb, "ATTENDANCE", ATTENDANCE_HEADERS)?.mapNotNull { toAttendance(it) } ?: emptyList()
                val leaves = readSheet(wb, "LEAVES", LEAVE_HEADERS)?.mapNotNull { toLeave(it) } ?: emptyList()
                val blocked = readSheet(wb, "BLOCKED ATTEMPTS", BLOCKED_HEADERS)?.mapNotNull { toBlocked(it) } ?: emptyList()
                val arrivals = readSheet(wb, "ARRIVAL REQUESTS", ARRIVAL_HEADERS)?.mapNotNull { toArrival(it) } ?: emptyList()

                val result = BackupRestoreResult(workers, sites, attendance, leaves, blocked, arrivals)
                if (result.isEmpty) {
                    throw BackupParseException("No recognizable SitePulse backup sheets found in this file.")
                }
                return result
            }
        }
    }

    private val WORKER_HEADERS = listOf("SNo", "ID", "Name", "Designation", "Company", "Site", "Aligned Date", "Status", "Left Date")
    private val SITE_HEADERS = listOf("Code", "Name", "Latitude", "Longitude", "Geofence Radius (m)", "WiFi SSID")
    private val ATTENDANCE_HEADERS = listOf(
        "Date", "Site Code", "Site Name", "Worker ID", "Shift", "Check IN", "Check OUT",
        "IN Lat", "IN Lng", "OUT Lat", "OUT Lng", "IN Dist (m)", "OUT Dist (m)", "Marked Via", "Marked By", "Last Action",
    )
    private val LEAVE_HEADERS = listOf(
        "Doc ID", "Worker ID", "Site", "From", "To", "Reason", "Status", "Marked By",
        "Requested By", "Approved By", "Approved At", "Rejected By", "Rejected At", "Timestamp",
    )
    private val BLOCKED_HEADERS = listOf("Doc ID", "Date", "Time", "Worker ID", "Name", "Nearest Site", "Distance (m)", "Action", "GPS Lat", "GPS Lng", "GPS Accuracy (m)")
    private val ARRIVAL_HEADERS = listOf(
        "Doc ID", "Site", "Worker ID", "Name", "Designation", "Requested Date", "Requested By",
        "Status", "Approved By", "Approved At", "Rejected By", "Rejected At", "Timestamp",
    )

    private fun toWorker(m: Map<String, String>): Worker? {
        val id = m["ID"].orEmpty()
        if (id.isBlank()) return null
        return Worker(
            sno = m["SNo"]?.toLongOrNull() ?: 0,
            id = id,
            name = m["Name"].orEmpty(),
            designation = m["Designation"].orEmpty(),
            company = m["Company"]?.ifBlank { null },
            site = m["Site"]?.ifBlank { null },
            alignedDate = m["Aligned Date"]?.ifBlank { null },
            status = m["Status"]?.ifBlank { null } ?: "active",
            leftDate = m["Left Date"]?.ifBlank { null },
        )
    }

    private fun toSite(m: Map<String, String>): Site? {
        val code = m["Code"].orEmpty()
        if (code.isBlank()) return null
        return Site(
            code = code,
            name = m["Name"].orEmpty(),
            lat = m["Latitude"]?.toDoubleOrNull() ?: 0.0,
            lng = m["Longitude"]?.toDoubleOrNull() ?: 0.0,
            radius = m["Geofence Radius (m)"]?.toLongOrNull() ?: 500,
            wifiSsid = m["WiFi SSID"]?.ifBlank { null },
        )
    }

    private fun toAttendance(m: Map<String, String>): Attendance? {
        val date = m["Date"].orEmpty()
        val workerId = m["Worker ID"].orEmpty()
        if (date.isBlank() || workerId.isBlank()) return null
        val inLat = m["IN Lat"]?.toDoubleOrNull()
        val inLng = m["IN Lng"]?.toDoubleOrNull()
        val outLat = m["OUT Lat"]?.toDoubleOrNull()
        val outLng = m["OUT Lng"]?.toDoubleOrNull()
        return Attendance(
            workerId = workerId,
            date = date,
            siteCode = m["Site Code"].orEmpty(),
            siteName = m["Site Name"].orEmpty(),
            lastAction = m["Last Action"]?.ifBlank { null } ?: (m["Check OUT"]?.ifBlank { null } ?: m["Check IN"].orEmpty()),
            markedBy = m["Marked By"].orEmpty(),
            shift = m["Shift"]?.ifBlank { null },
            checkIn = m["Check IN"]?.ifBlank { null },
            inGps = if (inLat != null && inLng != null) GpsPoint(inLat, inLng) else null,
            inDist = m["IN Dist (m)"]?.toLongOrNull(),
            out = m["Check OUT"]?.ifBlank { null },
            outGps = if (outLat != null && outLng != null) GpsPoint(outLat, outLng) else null,
            outDist = m["OUT Dist (m)"]?.toLongOrNull(),
            markedVia = m["Marked Via"]?.ifBlank { null } ?: "gps",
        )
    }

    private fun toLeave(m: Map<String, String>): Leave? {
        val workerId = m["Worker ID"].orEmpty()
        val fromDate = m["From"].orEmpty()
        if (workerId.isBlank() || fromDate.isBlank()) return null
        return Leave(
            docId = m["Doc ID"].orEmpty(),
            workerId = workerId,
            site = m["Site"].orEmpty(),
            fromDate = fromDate,
            toDate = m["To"]?.ifBlank { null } ?: fromDate,
            reason = m["Reason"]?.ifBlank { null },
            markedBy = m["Marked By"].orEmpty(),
            ts = m["Timestamp"]?.ifBlank { null } ?: DateUtils.nowIso(),
            status = m["Status"]?.ifBlank { null } ?: "approved",
            requestedBy = m["Requested By"]?.ifBlank { null },
            approvedBy = m["Approved By"]?.ifBlank { null },
            approvedAt = m["Approved At"]?.ifBlank { null },
            rejectedBy = m["Rejected By"]?.ifBlank { null },
            rejectedAt = m["Rejected At"]?.ifBlank { null },
        )
    }

    private fun toBlocked(m: Map<String, String>): Blocked? {
        val workerId = m["Worker ID"].orEmpty()
        val date = m["Date"].orEmpty()
        if (workerId.isBlank() || date.isBlank()) return null
        val lat = m["GPS Lat"]?.toDoubleOrNull()
        val lng = m["GPS Lng"]?.toDoubleOrNull()
        return Blocked(
            docId = m["Doc ID"].orEmpty(),
            workerId = workerId,
            name = m["Name"].orEmpty(),
            date = date,
            time = m["Time"].orEmpty(),
            gps = if (lat != null && lng != null) BlockedGps(lat, lng, m["GPS Accuracy (m)"]?.toDoubleOrNull()) else null,
            nearestSite = m["Nearest Site"].orEmpty(),
            distance = m["Distance (m)"]?.toLongOrNull() ?: 0,
            action = m["Action"].orEmpty(),
        )
    }

    private fun toArrival(m: Map<String, String>): ArrivalRequest? {
        val workerId = m["Worker ID"].orEmpty()
        val site = m["Site"].orEmpty()
        if (workerId.isBlank() || site.isBlank()) return null
        return ArrivalRequest(
            docId = m["Doc ID"].orEmpty(),
            site = site,
            workerId = workerId,
            name = m["Name"].orEmpty(),
            designation = m["Designation"].orEmpty(),
            requestedDate = m["Requested Date"].orEmpty(),
            requestedBy = m["Requested By"].orEmpty(),
            status = m["Status"]?.ifBlank { null } ?: "pending",
            ts = m["Timestamp"]?.ifBlank { null } ?: DateUtils.nowIso(),
            approvedAt = m["Approved At"]?.ifBlank { null },
            approvedBy = m["Approved By"]?.ifBlank { null },
            rejectedAt = m["Rejected At"]?.ifBlank { null },
            rejectedBy = m["Rejected By"]?.ifBlank { null },
        )
    }

    /**
     * Finds [sheetName], locates its header row by matching the first expected header text
     * (skipping the letterhead/title/meta rows above it), then reads every row below as a
     * header->value map until the sheet ends. Returns null if the sheet isn't present at all.
     */
    private fun readSheet(wb: XSSFWorkbook, sheetName: String, expectedHeaders: List<String>): List<Map<String, String>>? {
        val sheet: XSSFSheet = wb.getSheet(sheetName) ?: return null
        val fmt = DataFormatter()
        var headerRowIdx = -1
        for (r in sheet.firstRowNum..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            if (fmt.formatCellValue(row.getCell(0)).trim() == expectedHeaders.first()) {
                headerRowIdx = r
                break
            }
        }
        if (headerRowIdx == -1) return emptyList()

        val headerRow = sheet.getRow(headerRowIdx)
        val headerByCol = (0 until headerRow.lastCellNum.coerceAtLeast(0)).associate { c ->
            c to fmt.formatCellValue(headerRow.getCell(c)).trim()
        }

        val rows = mutableListOf<Map<String, String>>()
        for (r in (headerRowIdx + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val map = LinkedHashMap<String, String>()
            var anyNonBlank = false
            headerByCol.forEach { (c, h) ->
                if (h.isBlank()) return@forEach
                val v = fmt.formatCellValue(row.getCell(c)).trim()
                if (v.isNotBlank()) anyNonBlank = true
                map[h] = v
            }
            if (anyNonBlank) rows.add(map)
        }
        return rows
    }
}
