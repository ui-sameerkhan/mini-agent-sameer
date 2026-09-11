package com.ktc.sitepulse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs a real 8127 export through the reader and parser.
 *
 * The other tests build a table by hand from what the file *looks* like. This one reads the
 * actual workbook through Apache POI, which is what decides how a datetime cell is rendered —
 * the ERP's own cell format, not ours. Every assumption about that formatting was inferred
 * before this test existed, and inferring it is exactly how the first version got the times
 * wrong.
 *
 * The sample is one day of a single project. Its real shape, as observed rather than assumed:
 * five rows of company letterhead above the headings, 384 employee rows plus a "Printed by"
 * footer, one exactly duplicated row, punches stored as datetimes with no Date column anywhere,
 * and — the part that matters most — 62 employees listed with no punch at all. The export names
 * everyone it expected, so a blank punch is a fact about the worker, not a damaged cell.
 *
 * Worth knowing about this particular day: every punch in it falls between 05:00 and 08:59, and
 * all 21 night-shift rows have no punch whatsoever. AM/PM handling is therefore covered by the
 * synthetic case in BiometricImportTest, since this file has no evening punch to prove it with.
 */
class BiometricRealFileTest {

    private fun table(): RawTable {
        val stream = javaClass.classLoader!!.getResourceAsStream("biometric_8127_sample.xlsx")
            ?: error("sample workbook missing from test resources")
        return stream.use { SpreadsheetReader.readXlsxStream(it, BiometricImport.headerHints) }
    }

    @Test
    fun `the real export parses`() {
        val t = table()
        assertEquals(
            "headings should be found beneath the letterhead",
            listOf("S.No", "Emp.Code", "Emp.Name", "JobCode", "Punch In", "Punch Out", "SHIFT_TYPE"),
            t.headers,
        )
        // 384 employees plus the "Printed by: …" footer the ERP prints at the end.
        assertEquals(385, t.rows.size)

        val parsed = BiometricImport.parse(t, "2026-01-01")
        assertTrue("parse failed: $parsed", parsed is ParsedBiometric.Ok)
        parsed as ParsedBiometric.Ok

        // 384 employee rows, one an exact duplicate of another, so 383 distinct.
        assertEquals(383, parsed.punches.size)
        assertEquals("the footer row carries no employee code and should be dropped", 1, parsed.skippedRows)
        assertEquals(listOf("C-26-923"), parsed.jobCodes)
    }

    @Test
    fun `the day comes from the punches, not the date the operator had selected`() {
        // The file has no Date column at all. Falling back to the selected date would file the
        // whole report against the wrong day — every worker marked, none of them verified.
        val parsed = BiometricImport.parse(table(), "2026-01-01") as ParsedBiometric.Ok
        assertEquals(listOf("2026-09-11"), parsed.dates)
    }

    @Test
    fun `punch times survive the ERP's own cell format`() {
        val parsed = BiometricImport.parse(table(), "2026-09-11") as ParsedBiometric.Ok
        val naveen = parsed.punches.first { it.workerId == "5900" }
        assertEquals("06:57", naveen.punchIn)

        // Every row that has a punch must yield a readable time. A time this could not read
        // would look identical to no punch at all, and put an innocent worker on the review list.
        val punched = parsed.punches.filter { it.hasPunch }
        assertEquals("no punched row should be unreadable", 0, punched.count { it.punchIn.isBlank() })
        assertEquals(321, punched.size)

        // Spread across a working day, not collapsed onto one value.
        assertTrue(punched.map { it.punchIn.take(2) }.distinct().size > 3)
    }

    @Test
    fun `employees listed with no punch are kept, not silently dropped`() {
        // These are the men the report says did not punch. Losing them would quietly remove the
        // only evidence that someone marked present never touched a reader.
        val parsed = BiometricImport.parse(table(), "2026-09-11") as ParsedBiometric.Ok
        val noPunch = parsed.punches.filter { !it.hasPunch }
        assertEquals(62, noPunch.size)
        assertTrue("they still belong to the report's day",
            noPunch.all { it.date == "2026-09-11" })
    }

    @Test
    fun `the night shift does not reach this biometric at all`() {
        // Not a parsing question — an operational one, pinned here because it decides how the
        // result must be read. Every NIGHT row in this export is punch-less, so on a day like
        // this the night crew will appear as "marked, no punch" for the plainest of reasons:
        // no reader saw them. That is a coverage gap, not a discrepancy, and reading it as one
        // would put a whole shift's foremen under suspicion.
        val parsed = BiometricImport.parse(table(), "2026-09-11") as ParsedBiometric.Ok
        val punchedHours = parsed.punches.filter { it.hasPunch }.map { it.punchIn.take(2).toInt() }
        assertEquals("this day's punches are all early morning", 5, punchedHours.min())
        assertTrue("none of them are evening punches", punchedHours.max() < 12)
    }

    @Test
    fun `reconciling the real file against matching attendance finds nothing to review`() {
        val parsed = BiometricImport.parse(table(), "2026-09-11") as ParsedBiometric.Ok

        // Attendance that agrees with every punch, marked within the tolerance window. The
        // punch-less rows are marked too, at a nominal time — they are the interesting ones.
        val attendance = parsed.punches.map { p ->
            val (h, m) = (p.punchIn.ifBlank { "07:00" }).split(":").map { it.toInt() }
            com.ktc.sitepulse.data.model.Attendance(
                workerId = p.workerId, date = p.date, siteCode = "C-26-923",
                markedBy = "tk@ktc.test",
                checkIn = DateUtils.isoFromLocalTime(p.date, h, m),
            )
        }

        val summary = BiometricReconciliation.run(attendance, parsed.punches, emptyList())
        assertEquals(321, summary.agreed)
        // The 63 with no punch are marked in the app but absent from the biometric — exactly
        // what this is for. Nothing else should be flagged.
        assertEquals(62, summary.markedNotPunched)
        assertEquals(0, summary.punchedNotMarked)
        assertEquals(0, summary.timeMismatch)
        assertTrue("coverage should be ample", !summary.coverageTooLowToJudge)
    }

    @Test
    fun `a worker marked but absent from the biometric is the one that surfaces`() {
        val parsed = BiometricImport.parse(table(), "2026-09-11") as ParsedBiometric.Ok
        val ghost = com.ktc.sitepulse.data.model.Attendance(
            workerId = "NOT-IN-BIOMETRIC", date = "2026-09-11", siteCode = "C-26-923",
            markedBy = "foreman@ktc.test",
            checkIn = DateUtils.isoFromLocalTime("2026-09-11", 7, 0),
        )
        val real = parsed.punches.filter { it.hasPunch }.take(50).map { p ->
            val (h, m) = p.punchIn.split(":").map { it.toInt() }
            com.ktc.sitepulse.data.model.Attendance(
                workerId = p.workerId, date = p.date, siteCode = "C-26-923",
                markedBy = "tk@ktc.test",
                checkIn = DateUtils.isoFromLocalTime(p.date, h, m),
            )
        }

        val summary = BiometricReconciliation.run(real + ghost, parsed.punches, emptyList())
        val flagged = summary.findings.filter {
            it.verdict == BiometricReconciliation.Verdict.MARKED_NOT_PUNCHED
        }
        assertEquals(1, flagged.size)
        assertEquals("NOT-IN-BIOMETRIC", flagged.single().workerId)
        assertEquals("foreman@ktc.test", flagged.single().markedBy)
    }
}
