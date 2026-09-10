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
