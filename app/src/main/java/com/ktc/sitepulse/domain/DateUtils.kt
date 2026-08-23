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
}
