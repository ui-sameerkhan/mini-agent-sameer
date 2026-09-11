package com.ktc.sitepulse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The 8127 report comes out of a system nobody here controls, so the parser is written to be
 * forgiving about headers, dates and times, and these pin how far that forgiveness goes.
 */
class BiometricImportTest {

    private fun table(headers: List<String>, vararg rows: List<String>) = RawTable(
        headers = headers,
        rows = rows.map { r -> headers.indices.associate { headers[it] to (r.getOrNull(it) ?: "") } },
    )

    @Test
    fun `reads the plain shape`() {
        val t = table(
            listOf("Employee ID", "Employee Name", "Date", "Punch In", "Punch Out"),
            listOf("W-1", "Aslam", "2026-09-01", "07:02", "17:30"),
            listOf("W-2", "Bilal", "2026-09-01", "07:11", "17:28"),
        )
        val r = BiometricImport.parse(t, "2026-09-02") as ParsedBiometric.Ok
        assertEquals(2, r.punches.size)
        assertEquals(listOf("2026-09-01"), r.dates)
        assertEquals("Aslam", r.punches.first().name)
        assertEquals("07:02", r.punches.first().punchIn)
    }

    @Test
    fun `tolerates the header spellings the ERP might use`() {
        // Same report, different captions — this is what breaks importers in practice.
        val t = table(
            listOf("EMP. CODE", "NAME", "LOG DATE", "IN TIME", "OUT TIME"),
            listOf("W-1", "Aslam", "01/09/2026", "07:02", "17:30"),
        )
        val r = BiometricImport.parse(t, "2026-09-02") as ParsedBiometric.Ok
        assertEquals("W-1", r.punches.single().workerId)
        assertEquals("day-first slash dates are the local convention", "2026-09-01", r.punches.single().date)
    }

    @Test
    fun `falls back to the chosen date when the report has no date column`() {
        val t = table(
            listOf("Employee ID", "Name", "In", "Out"),
            listOf("W-1", "Aslam", "07:02", "17:30"),
        )
        val r = BiometricImport.parse(t, "2026-09-05") as ParsedBiometric.Ok
        assertEquals("2026-09-05", r.punches.single().date)
    }

    @Test
    fun `several rows for one worker collapse to first in and last out`() {
        // Multiple in/out pairs across a day is normal on a biometric; presence is the envelope.
        val t = table(
            listOf("Employee ID", "Date", "Punch In", "Punch Out"),
            listOf("W-1", "2026-09-01", "12:40", "17:30"),
            listOf("W-1", "2026-09-01", "07:02", "11:55"),
        )
        val r = BiometricImport.parse(t, "2026-09-01") as ParsedBiometric.Ok
        val p = r.punches.single()
        assertEquals("07:02", p.punchIn)
        assertEquals("17:30", p.punchOut)
    }

    @Test
    fun `rows with no employee id are skipped, not guessed at`() {
        val t = table(
            listOf("Employee ID", "Date", "Punch In"),
            listOf("", "2026-09-01", "07:02"),
            listOf("W-1", "2026-09-01", "07:03"),
        )
        val r = BiometricImport.parse(t, "2026-09-01") as ParsedBiometric.Ok
        assertEquals(1, r.punches.size)
        assertEquals(1, r.skippedRows)
    }

    @Test
    fun `a file with no id column is refused with the columns it did find`() {
        val t = table(listOf("Something", "Else"), listOf("a", "b"))
        val r = BiometricImport.parse(t, "2026-09-01")
        assertTrue(r is ParsedBiometric.ColumnsNotFound)
        assertTrue((r as ParsedBiometric.ColumnsNotFound).message.contains("Something"))
    }

    @Test
    fun `a file with ids but no punch columns is refused`() {
        // Nothing to verify against — better to say so than to report everyone as unpunched.
        val t = table(listOf("Employee ID", "Name"), listOf("W-1", "Aslam"))
        val r = BiometricImport.parse(t, "2026-09-01")
        assertTrue(r is ParsedBiometric.ColumnsNotFound)
    }

    // ---- The real 8127 export ---------------------------------------------------------------
    //
    // Shape taken from an actual file: five rows of company letterhead above the headings,
    // "Emp.Code"/"Emp.Name" rather than "Employee ID", a JobCode column, no Date column at all,
    // and punches as datetimes rendered by the ERP's own cell format ("11-Sep-2026 06:57 AM").
    // Every one of these broke the first version of this parser.

    private fun realShape(vararg rows: List<String>) = RawTable(
        headers = listOf("S.No", "Emp.Code", "Emp.Name", "JobCode", "Punch In", "Punch Out", "SHIFT_TYPE"),
        rows = rows.map { r ->
            listOf("S.No", "Emp.Code", "Emp.Name", "JobCode", "Punch In", "Punch Out", "SHIFT_TYPE")
                .mapIndexed { i, h -> h to (r.getOrNull(i) ?: "") }.toMap()
        },
    )

    @Test
    fun `reads the real 8127 export`() {
        val t = realShape(
            listOf("1", "5900", "Naveen kumar", "C-26-923", "11-Sep-2026 06:57 AM", "11-Sep-2026 06:57 AM", "DAY"),
            listOf("2", "3985", "Almar Atig Gomez", "C-26-923", "11-Sep-2026 07:15 AM", "11-Sep-2026 07:15 AM", "DAY"),
        )
        val r = BiometricImport.parse(t, "2026-01-01") as ParsedBiometric.Ok
        assertEquals(2, r.punches.size)
        val p = r.punches.first { it.workerId == "5900" }
        assertEquals("the date must come from the punch, not the operator's selected date", "2026-09-11", p.date)
        assertEquals("06:57", p.punchIn)
        assertEquals("Naveen kumar", p.name)
        assertEquals("C-26-923", p.jobCode)
        assertEquals(listOf("C-26-923"), r.jobCodes)
    }

    @Test
    fun `an evening punch is not read as morning`() {
        // The export prints 12-hour times with AM/PM. Dropping the marker would file a night
        // shift's 6pm punch as 6am and flag the man as a discrepancy.
        val t = realShape(listOf("1", "77", "Night Man", "C-26-923", "11-Sep-2026 06:15 PM", "", "NIGHT"))
        val r = BiometricImport.parse(t, "2026-01-01") as ParsedBiometric.Ok
        assertEquals("18:15", r.punches.single().punchIn)
    }

    @Test
    fun `the header row is found beneath the letterhead`() {
        // The five letterhead rows, then the real headings — as the ERP prints it.
        val sheet = listOf(
            listOf("KTC International Contracting LLC", "", "", "", "", "", "8092-Biometric Punch"),
            listOf("PO BOX NO.28427", "", "", "", "", "", "@strFromDate: 2026-09-11"),
            listOf("Business Bay, Dubai", "", "", "", "", "", "@strJobDocNo: C-26-923"),
            listOf("Tel +971-4-5876599", "", "", "", "", "", "@strLoginID: Ajay.Kumar"),
            listOf("www.ktcco.net", "", "", "", "", "", "11-Sep-2026 14:03:15"),
            listOf("S.No", "Emp.Code", "Emp.Name", "JobCode", "Punch In", "Punch Out", "SHIFT_TYPE"),
            listOf("1", "5900", "Naveen kumar", "C-26-923", "11-Sep-2026 06:57 AM", "", "DAY"),
        )
        assertEquals(5, SpreadsheetReader.findHeaderRow(sheet, BiometricImport.headerHints))
    }

    @Test
    fun `a sheet whose headings really are on row one is left alone`() {
        // The worker and roster imports rely on this; changing it would break working uploads.
        val sheet = listOf(
            listOf("Employee ID", "Employee Name", "Punch In"),
            listOf("W-1", "Aslam", "07:02"),
        )
        assertEquals(0, SpreadsheetReader.findHeaderRow(sheet, BiometricImport.headerHints))
    }

    @Test
    fun `a sheet nothing matches falls back to row one rather than guessing`() {
        val sheet = listOf(listOf("Alpha", "Beta"), listOf("1", "2"))
        assertEquals(0, SpreadsheetReader.findHeaderRow(sheet, BiometricImport.headerHints))
    }

    @Test
    fun `datetime cells split into a date and a 24-hour time`() {
        assertEquals("2026-09-11" to "06:57", BiometricImport.splitDateTime("11-Sep-2026 06:57 AM"))
        assertEquals("2026-09-11" to "18:15", BiometricImport.splitDateTime("11-Sep-2026 06:15 PM"))
        assertEquals("2026-09-11" to "06:57", BiometricImport.splitDateTime("2026-09-11 06:57:04"))
        assertEquals("" to "07:02", BiometricImport.splitDateTime("07:02"))
        assertEquals("2026-09-11" to "", BiometricImport.splitDateTime("11-Sep-2026"))
        assertEquals("" to "", BiometricImport.splitDateTime(""))
    }

    @Test
    fun `the duplicated row the real export contained collapses to one`() {
        val t = realShape(
            listOf("1", "4807", "Dup Man", "C-26-923", "11-Sep-2026 05:44 AM", "11-Sep-2026 05:44 AM", "DAY"),
            listOf("2", "4807", "Dup Man", "C-26-923", "11-Sep-2026 05:44 AM", "11-Sep-2026 05:44 AM", "DAY"),
        )
        val r = BiometricImport.parse(t, "2026-09-11") as ParsedBiometric.Ok
        assertEquals(1, r.punches.size)
    }

    @Test
    fun `date formats an ERP might print`() {
        assertEquals("2026-09-01", DateUtils.normaliseDate("2026-09-01"))
        assertEquals("2026-09-01", DateUtils.normaliseDate("01/09/2026"))
        assertEquals("2026-09-01", DateUtils.normaliseDate("01-09-26"))
        assertEquals("2026-09-01", DateUtils.normaliseDate("01-Sep-2026"))
        assertEquals("2026-09-01", DateUtils.normaliseDate("2026-09-01 07:02:00"))
        assertEquals("", DateUtils.normaliseDate(""))
        assertEquals("", DateUtils.normaliseDate("not a date"))
        // An Excel serial, which is what a date cell often arrives as.
        assertEquals("2026-09-01", DateUtils.normaliseDate("46266"))
    }
}
