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
    /** Local "HH:mm" as printed by the ERP, or blank when the report has no in-time. */
    val punchIn: String,
    val punchOut: String,
) {
    val hasPunch: Boolean get() = punchIn.isNotBlank() || punchOut.isNotBlank()
}

sealed class ParsedBiometric {
    data class Ok(
        val punches: List<BiometricPunch>,
        val dates: List<String>,
        /** Rows dropped because they carried no employee ID at all. */
        val skippedRows: Int,
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

        val punches = mutableListOf<BiometricPunch>()
        var skipped = 0
        for (row in table.rows) {
            val id = ColumnMatcher.cell(row, idH).trim()
            if (id.isBlank()) { skipped++; continue }
            val date = DateUtils.normaliseDate(ColumnMatcher.cell(row, dateH)).ifBlank { fallbackDate }
            punches.add(
                BiometricPunch(
                    workerId = id,
                    name = ColumnMatcher.cell(row, nameH).trim(),
                    date = date,
                    punchIn = ColumnMatcher.cell(row, inH).trim(),
                    punchOut = ColumnMatcher.cell(row, outH).trim(),
                )
            )
        }

        if (punches.isEmpty()) {
            return ParsedBiometric.NoValidRows(
                "No rows in this file carried an employee ID, so there is nothing to compare."
            )
        }

        // One worker can appear on several rows in a day (multiple in/out pairs). Collapse to the
        // earliest in and the latest out, which is what a day's presence actually means.
        val merged = punches
            .groupBy { it.workerId to it.date }
            .map { (_, rows) ->
                val ins = rows.map { it.punchIn }.filter { it.isNotBlank() }
                val outs = rows.map { it.punchOut }.filter { it.isNotBlank() }
                rows.first().copy(
                    name = rows.firstOrNull { it.name.isNotBlank() }?.name.orEmpty(),
                    punchIn = ins.minOrNull().orEmpty(),
                    punchOut = outs.maxOrNull().orEmpty(),
                )
            }
            .sortedWith(compareBy({ it.date }, { it.workerId }))

        return ParsedBiometric.Ok(
            punches = merged,
            dates = merged.map { it.date }.distinct().sorted(),
            skippedRows = skipped,
        )
    }
}
