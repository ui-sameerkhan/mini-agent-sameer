package com.ktc.sitepulse.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Mirrors the original app's date conventions exactly:
 *  - "today" for attendance doc IDs is the UTC calendar date
 *    (JS: new Date().toISOString().slice(0,10)) — NOT the device's local date.
 *  - Night-shift classification uses the LOCAL device hour.
 * Both are intentionally kept as in the web app so data stays consistent
 * with existing Firestore records.
 */
object DateUtils {
    private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayStrUtc(): String = LocalDate.now(ZoneOffset.UTC).format(ISO_DATE)

    fun nowIso(): String = Instant.now().toString()

    fun yesterdayOf(dateStr: String): String =
        LocalDate.parse(dateStr, ISO_DATE).minusDays(1).format(ISO_DATE)

    fun localHour(): Int = LocalTime.now().hour

    /** nightStart/dayStart let a specific site override the company-wide 18:00/05:00 default
     * (e.g. a site running a genuinely different shift pattern) — see Site.nightStartHour. */
    fun shiftFor(hour: Int = localHour(), nightStart: Int = 18, dayStart: Int = 5): String =
        if (hour >= nightStart || hour < dayStart) "Night" else "Day"

    /** Formats an ISO instant string to a localized "h:mm a" time, or "" if null/unparseable. */
    fun formatTimeHm(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val instant = Instant.parse(iso)
            val local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            local.format(DateTimeFormatter.ofPattern("h:mm a"))
        } catch (e: DateTimeParseException) {
            ""
        }
    }

    fun formatDateTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val instant = Instant.parse(iso)
            val local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            local.format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))
        } catch (e: DateTimeParseException) {
            ""
        }
    }

    fun monthOf(dateStr: String): String = dateStr.substring(0, 7) // "YYYY-MM"

    fun monthStart(monthStr: String): String = "$monthStr-01"

    fun monthEndExclusive(monthStr: String): String {
        val ym = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"))
        val next = ym.plusMonths(1)
        return "%04d-%02d-01".format(next.year, next.monthValue)
    }

    fun daysInMonth(monthStr: String): Int =
        YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM")).lengthOfMonth()

    /** Days elapsed so far if monthStr is the current UTC month, else the full month length. */
    fun elapsedDaysFor(monthStr: String): Int {
        val currentMonth = monthOf(todayStrUtc())
        return if (monthStr == currentMonth) {
            LocalDate.now(ZoneOffset.UTC).dayOfMonth
        } else {
            daysInMonth(monthStr)
        }
    }

    /** Hours between a check-in and check-out ISO instant, or null if either is missing/unparseable
     * or out precedes in (e.g. a still-open shift). Real elapsed time, so a night shift crossing
     * midnight is handled automatically — no separate date-rollover logic needed. */
    fun hoursBetween(inIso: String?, outIso: String?): Double? {
        if (inIso.isNullOrBlank() || outIso.isNullOrBlank()) return null
        return try {
            val seconds = java.time.Duration.between(Instant.parse(inIso), Instant.parse(outIso)).seconds
            if (seconds < 0) null else seconds / 3600.0
        } catch (e: DateTimeParseException) {
            null
        }
    }

    /** Builds an ISO instant from a calendar date + a local HH:mm time — used by the admin's
     * manual attendance correction dialog, where the picker only knows a clock time. */
    fun isoFromLocalTime(dateStr: String, hour: Int, minute: Int, plusDays: Int = 0): String {
        val date = LocalDate.parse(dateStr, ISO_DATE).plusDays(plusDays.toLong())
        val local = LocalDateTime.of(date, LocalTime.of(hour, minute))
        return local.atZone(ZoneId.systemDefault()).toInstant().toString()
    }

    /** The inverse of [isoFromLocalTime] — local hour/minute to pre-fill an edit dialog, or null if unparseable. */
    fun localHourMinute(iso: String?): Pair<Int, Int>? {
        if (iso.isNullOrBlank()) return null
        return try {
            val local = LocalDateTime.ofInstant(Instant.parse(iso), ZoneId.systemDefault())
            local.hour to local.minute
        } catch (e: DateTimeParseException) {
            null
        }
    }

    fun isWithin(dateStr: String, fromDate: String, toDate: String): Boolean {
        val d = LocalDate.parse(dateStr, ISO_DATE)
        val from = LocalDate.parse(fromDate, ISO_DATE)
        val to = LocalDate.parse(toDate, ISO_DATE)
        return !d.isBefore(from) && !d.isAfter(to)
    }

    /**
     * Best-effort "YYYY-MM-DD" from whatever a spreadsheet from another system happens to contain.
     * Returns "" when nothing sensible can be read, so the caller can fall back to a known date
     * rather than inventing one.
     *
     * The ERP prints dates in its own locale and Excel hands some cells back as a serial number,
     * so guessing is unavoidable — but the guessing is bounded here rather than spread across
     * every importer. Day-first is assumed for ambiguous slash dates (01/09/2026 is 1 September),
     * because that is the convention everywhere this is used; an ISO date is passed through
     * untouched, and a value with a four-digit year first is read year-first regardless.
     */
    fun normaliseDate(raw: String?): String {
        val s = raw?.trim().orEmpty()
        if (s.isBlank()) return ""

        // Excel serial date: days since 1899-12-30. Only treat plausibly-dated numbers this way.
        s.toDoubleOrNull()?.let { serial ->
            val days = serial.toLong()
            if (days in 20_000..60_000) {
                return LocalDate.of(1899, 12, 30).plusDays(days).format(ISO_DATE)
            }
            return ""
        }

        // Drop any time part the cell carries alongside the date.
        val datePart = s.split(' ', 'T').first()
        val parts = datePart.split('-', '/', '.').filter { it.isNotBlank() }
        if (parts.size != 3) return tryMonthName(datePart)

        val nums = parts.map { it.toIntOrNull() ?: return tryMonthName(datePart) }
        val (y, m, d) = when {
            parts[0].length == 4 -> Triple(nums[0], nums[1], nums[2])   // 2026-09-01
            parts[2].length == 4 -> Triple(nums[2], nums[1], nums[0])   // 01/09/2026, day first
            else -> Triple(2000 + nums[2], nums[1], nums[0])            // 01/09/26
        }
        return try {
            LocalDate.of(y, m, d).format(ISO_DATE)
        } catch (e: java.time.DateTimeException) {
            ""
        }
    }

    private val MONTHS = listOf(
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
    )

    /** Handles the "01-Sep-2026" shape the ERP uses on some reports. */
    private fun tryMonthName(s: String): String {
        val parts = s.split('-', '/', ' ', '.').filter { it.isNotBlank() }
        if (parts.size != 3) return ""
        val monthIdx = parts.indexOfFirst { p -> MONTHS.any { p.lowercase().startsWith(it) } }
        if (monthIdx == -1) return ""
        val month = MONTHS.indexOfFirst { parts[monthIdx].lowercase().startsWith(it) } + 1
        val others = parts.filterIndexed { i, _ -> i != monthIdx }.mapNotNull { it.toIntOrNull() }
        if (others.size != 2) return ""
        val year = others.firstOrNull { it > 31 }?.let { if (it < 100) 2000 + it else it } ?: return ""
        val day = others.first { it <= 31 }
        return try {
            LocalDate.of(year, month, day).format(ISO_DATE)
        } catch (e: java.time.DateTimeException) {
            ""
        }
    }
}
