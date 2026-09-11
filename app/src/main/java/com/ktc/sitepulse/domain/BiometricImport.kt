package com.ktc.sitepulse.domain

/**
 * One row of the ERP biometric report (KTC's report 8127): who punched, and when.
 *
 * This never reaches Firestore. It is read from the uploaded file, compared against the
 * attendance already recorded, and discarded — the biometric system stays the system of record
 * for punches, and SitePulse stays the system of record for marked attendance. Storing a copy
 * would only create a third version of the truth to keep in sync.
 */
data class BiometricPunch(
    val workerId: String,
    val name: String,
    val date: String,
    /** Normalised 24-hour "HH:mm", whatever shape the ERP printed it in. Blank when absent. */
    val punchIn: String,
    val punchOut: String,
    /**
     * The ERP job/project code the punch was booked against, when the report carries one.
     * Informational: it identifies which project an export covers, so a report for another
     * project is obvious rather than silently producing a page of discrepancies.
     */
    val jobCode: String = "",
) {
    val hasPunch: Boolean get() = punchIn.isNotBlank() || punchOut.isNotBlank()
}

sealed class ParsedBiometric {
    data class Ok(
        val punches: List<BiometricPunch>,
        val dates: List<String>,
        /** Rows dropped because they carried no employee ID at all. */
        val skippedRows: Int,
        /** Distinct ERP job codes the file covers — usually exactly one. */
        val jobCodes: List<String> = emptyList(),
    ) : ParsedBiometric()

    data class ColumnsNotFound(val message: String) : ParsedBiometric()
    data class NoValidRows(val message: String) : ParsedBiometric()
}

/**
 * Reads the ERP biometric report into [BiometricPunch] rows.
 *
 * Header names are matched through [ColumnMatcher], which strips case, spaces and punctuation,
 * so "Employee ID", "EMP. ID" and "EmpId" all resolve the same way. That matters here more than
 * for the other imports: this file comes out of a system nobody involved controls, and its column
 * captions may change between ERP versions without warning.
 */
object BiometricImport {

    private val idSynonyms = listOf(
        "employeeid", "empid", "employeecode", "empcode", "workerid", "staffid", "id", "eno", "empno",
    )
    private val nameSynonyms = listOf("employeename", "name", "empname", "workername")
    private val dateSynonyms = listOf("date", "attendancedate", "punchdate", "logdate", "day")
    private val inSynonyms = listOf(
        "punchin", "in", "intime", "checkin", "firstin", "timein", "entry", "entrytime",
    )
    private val outSynonyms = listOf(
        "punchout", "out", "outtime", "checkout", "lastout", "timeout", "exit", "exittime",
    )
    private val jobSynonyms = listOf("jobcode", "job", "projectcode", "project", "site", "costcentre")

    /**
     * Column names worth searching the top of the sheet for. The real 8127 export prints five
     * rows of company letterhead above its headings, so the headings are not on row 1.
     */
    val headerHints: List<String> = idSynonyms + inSynonyms + listOf("empname", "employeename")

    /**
     * Splits a cell that may hold a date, a time, or both, into an ISO date and a 24-hour time.
     *
     * The ERP stores punches as real datetimes and prints them through its own cell format —
     * "11-Sep-2026 06:57 AM" in the export seen so far. A month name is a mercy here: a numeric
     * "11/09/2026" would be genuinely ambiguous, and this is payroll evidence, so guessing
     * day-first on a US-formatted export would silently shift every punch to the wrong day.
     */
    internal fun splitDateTime(raw: String): Pair<String, String> {
        val v = raw.trim()
        if (v.isBlank()) return "" to ""

        // A bare time, with no date attached.
        if (!v.contains(' ') && !v.contains('T')) {
            return if (v.contains(':')) "" to formatHhMm(v) else DateUtils.normaliseDate(v) to ""
        }

        // Otherwise the first token is the date and the rest is the time (possibly with AM/PM).
        val head = v.substringBefore(' ').substringBefore('T')
        val tail = v.removePrefix(head).trim()
        val date = DateUtils.normaliseDate(head)
        return if (date.isBlank()) "" to formatHhMm(v) else date to formatHhMm(tail)
    }

    /** "6:57 AM" / "06:57:04" / "18:05" -> "06:57" / "06:57" / "18:05"; "" when unreadable. */
    private fun formatHhMm(raw: String): String {
        val hm = BiometricReconciliation.parseHhMm(raw) ?: return ""
        return "%02d:%02d".format(hm.first, hm.second)
    }

    /**
     * @param fallbackDate used when the report carries no date column — a single-day export, where
     *   the date lives in the filename or a header the sheet does not repeat per row.
     */
    fun parse(table: RawTable, fallbackDate: String): ParsedBiometric {
        val idH = ColumnMatcher.resolve(table.headers, idSynonyms)
        if (idH == null) {
            return ParsedBiometric.ColumnsNotFound(
                "Couldn't find an Employee ID column in this file. Columns found: " +
                    table.headers.joinToString(", ").ifBlank { "(none)" }
            )
        }
        val nameH = ColumnMatcher.resolve(table.headers, nameSynonyms)
        val dateH = ColumnMatcher.resolve(table.headers, dateSynonyms)
        val inH = ColumnMatcher.resolve(table.headers, inSynonyms)
        val outH = ColumnMatcher.resolve(table.headers, outSynonyms)

        if (inH == null && outH == null) {
            return ParsedBiometric.ColumnsNotFound(
                "Found Employee ID but no punch-in or punch-out column, so there is nothing to " +
                    "verify against. Columns found: ${table.headers.joinToString(", ")}"
            )
        }

        val jobH = ColumnMatcher.resolve(table.headers, jobSynonyms)

        val punches = mutableListOf<BiometricPunch>()
        var skipped = 0
        for (row in table.rows) {
            val id = ColumnMatcher.cell(row, idH).trim()
            if (id.isBlank()) { skipped++; continue }

            val (inDate, inTime) = splitDateTime(ColumnMatcher.cell(row, inH))
            val (outDate, outTime) = splitDateTime(ColumnMatcher.cell(row, outH))

            // The day this punch belongs to, in order of trust: an explicit Date column, then
            // the date carried inside the punch itself. Rows with neither are resolved below,
            // once the rest of the file has said which day it is about.
            val date = DateUtils.normaliseDate(ColumnMatcher.cell(row, dateH))
                .ifBlank { inDate }
                .ifBlank { outDate }

            punches.add(
                BiometricPunch(
                    workerId = id,
                    name = ColumnMatcher.cell(row, nameH).trim(),
                    date = date,
                    punchIn = inTime,
                    punchOut = outTime,
                    jobCode = ColumnMatcher.cell(row, jobH).trim(),
                )
            )
        }

        // A row can legitimately carry no date: the export lists everyone it expected, including
        // those who never punched, and a row with no punch has no datetime to take a date from.
        // Those rows belong to the same day as the rest of the file, so the file decides — not
        // whatever date the operator happened to have on screen. Getting this wrong split one
        // report across two days, and the day holding the non-punchers was then reconciled
        // against no attendance at all.
        val statedDates = punches.map { it.date }.filter { it.isNotBlank() }
        val fileDate = statedDates.groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: fallbackDate
        val dated = punches.map { if (it.date.isBlank()) it.copy(date = fileDate) else it }

        if (dated.isEmpty()) {
            return ParsedBiometric.NoValidRows(
                "No rows in this file carried an employee ID, so there is nothing to compare."
            )
        }

        // One worker can appear on several rows in a day (multiple in/out pairs). Collapse to the
        // earliest in and the latest out, which is what a day's presence actually means.
        val merged = dated
            .groupBy { it.workerId to it.date }
            .map { (_, rows) ->
                val ins = rows.map { it.punchIn }.filter { it.isNotBlank() }
                val outs = rows.map { it.punchOut }.filter { it.isNotBlank() }
                rows.first().copy(
                    name = rows.firstOrNull { it.name.isNotBlank() }?.name.orEmpty(),
                    punchIn = ins.minOrNull().orEmpty(),
                    punchOut = outs.maxOrNull().orEmpty(),
                    jobCode = rows.firstOrNull { it.jobCode.isNotBlank() }?.jobCode.orEmpty(),
                )
            }
            .sortedWith(compareBy({ it.date }, { it.workerId }))

        return ParsedBiometric.Ok(
            punches = merged,
            dates = merged.map { it.date }.distinct().sorted(),
            skippedRows = skipped,
            jobCodes = merged.map { it.jobCode }.filter { it.isNotBlank() }.distinct().sorted(),
        )
    }
}
