package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Worker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * These decide whether a foreman gets asked to explain himself, so they are written to pin the
 * false accusations as hard as the true ones.
 */
class BiometricReconciliationTest {

    private val date = "2026-09-01"

    private fun worker(id: String, name: String) = Worker(id = id, name = name, designation = "Mason")

    /** SitePulse attendance is stored as an ISO instant; build one at a local wall-clock time. */
    private fun att(id: String, hour: Int, minute: Int = 0, site: String = "SITE-A", by: String = "foreman@ktc.test") =
        Attendance(
            workerId = id, date = date, siteCode = site, markedBy = by,
            checkIn = DateUtils.isoFromLocalTime(date, hour, minute),
        )

    private fun punch(id: String, inTime: String, name: String = "X") =
        BiometricPunch(workerId = id, name = name, date = date, punchIn = inTime, punchOut = "")

    // ---- The case the whole feature exists for -------------------------------------------

    @Test
    fun `marked in the app with no biometric punch is flagged`() {
        val summary = BiometricReconciliation.run(
            attendance = listOf(att("W-1", 7)),
            punches = listOf(punch("W-2", "07:05")), // a different worker punched; W-1 did not
            workers = listOf(worker("W-1", "Ghost")),
        )
        val f = summary.findings.first { it.workerId == "W-1" }
        assertEquals(BiometricReconciliation.Verdict.MARKED_NOT_PUNCHED, f.verdict)
        assertEquals("The accountable name must be carried through", "foreman@ktc.test", f.markedBy)
        assertEquals(1, summary.markedNotPunched)
    }

    @Test
    fun `both sources agreeing is not flagged`() {
        val summary = BiometricReconciliation.run(
            attendance = listOf(att("W-1", 7, 30)),
            punches = listOf(punch("W-1", "07:10")),
            workers = listOf(worker("W-1", "Real")),
        )
        assertEquals(BiometricReconciliation.Verdict.AGREED, summary.findings.single().verdict)
        assertEquals(0, summary.reviewCount)
    }

    @Test
    fun `a punch with no app record is surfaced as a missed marking`() {
        // The opposite error, and the one that costs the worker money rather than the company.
        val summary = BiometricReconciliation.run(
            attendance = emptyList(),
            punches = listOf(punch("W-9", "06:55", name = "Unpaid")),
            workers = emptyList(),
        )
        val f = summary.findings.single()
        assertEquals(BiometricReconciliation.Verdict.PUNCHED_NOT_MARKED, f.verdict)
        assertEquals("Unpaid", f.name)
    }

    // ---- Guarding against false accusations ----------------------------------------------

    @Test
    fun `a normal gap between gate punch and workface check-in is not a mismatch`() {
        // The reader is at the camp gate; the man reaches the workface later. Routine, not fraud.
        val summary = BiometricReconciliation.run(
            attendance = listOf(att("W-1", 8, 0)),
            punches = listOf(punch("W-1", "06:45")), // 75 minutes earlier
            workers = listOf(worker("W-1", "Early")),
        )
        assertEquals(BiometricReconciliation.Verdict.AGREED, summary.findings.single().verdict)
    }

    @Test
    fun `a gap beyond tolerance is flagged as a time mismatch, not as fraud`() {
        val summary = BiometricReconciliation.run(
            attendance = listOf(att("W-1", 14, 0)),
            punches = listOf(punch("W-1", "06:00")), // eight hours apart
            workers = listOf(worker("W-1", "Odd")),
        )
        val f = summary.findings.single()
        assertEquals(BiometricReconciliation.Verdict.TIME_MISMATCH, f.verdict)
        assertEquals(480, f.gapMinutes)
    }

    @Test
    fun `a day the biometric file does not cover is left alone`() {
        // Judging a date absent from the report would flag every mark on it as unverified.
        val other = Attendance(
            workerId = "W-1", date = "2026-08-31", siteCode = "SITE-A",
            checkIn = DateUtils.isoFromLocalTime("2026-08-31", 7, 0),
        )
        val summary = BiometricReconciliation.run(
            attendance = listOf(other),
            punches = listOf(punch("W-1", "07:00")), // only 2026-09-01
            workers = listOf(worker("W-1", "Yesterday")),
        )
        assertTrue("attendance outside the report's dates must be ignored",
            summary.findings.none { it.date == "2026-08-31" })
    }

    @Test
    fun `low biometric coverage is reported so nobody reads it as fraud`() {
        // A site with no reader: four men marked, one on the biometric. The three unpunched are
        // not suspects, and the summary has to say so.
        val summary = BiometricReconciliation.run(
            attendance = listOf(att("W-1", 7), att("W-2", 7), att("W-3", 7), att("W-4", 7)),
            punches = listOf(punch("W-1", "07:00")),
            workers = emptyList(),
        )
        assertEquals(1, summary.coverage)
        assertEquals(4, summary.markedTotal)
        assertTrue(summary.coverageTooLowToJudge)
    }

    @Test
    fun `good coverage does not raise the low-coverage warning`() {
        val summary = BiometricReconciliation.run(
            attendance = listOf(att("W-1", 7), att("W-2", 7)),
            punches = listOf(punch("W-1", "07:00"), punch("W-2", "07:00")),
            workers = emptyList(),
        )
        assertFalse(summary.coverageTooLowToJudge)
    }

    @Test
    fun `a punch row carrying no times at all counts as no punch`() {
        val summary = BiometricReconciliation.run(
            attendance = listOf(att("W-1", 7)),
            punches = listOf(BiometricPunch("W-1", "Blank", date, "", "")),
            workers = emptyList(),
        )
        assertEquals(BiometricReconciliation.Verdict.MARKED_NOT_PUNCHED, summary.findings.single().verdict)
    }

    @Test
    fun `findings open on what has to be acted on`() {
        val summary = BiometricReconciliation.run(
            attendance = listOf(att("W-OK", 7), att("W-BAD", 7)),
            punches = listOf(punch("W-OK", "07:00"), punch("W-MISS", "07:00")),
            workers = emptyList(),
        )
        assertEquals(BiometricReconciliation.Verdict.MARKED_NOT_PUNCHED, summary.findings.first().verdict)
        assertEquals(BiometricReconciliation.Verdict.AGREED, summary.findings.last().verdict)
    }

    // ---- Time parsing --------------------------------------------------------------------

    @Test
    fun `punch times are read in the shapes an ERP prints them`() {
        assertEquals(7 to 45, BiometricReconciliation.parseHhMm("07:45"))
        assertEquals(7 to 45, BiometricReconciliation.parseHhMm("7:45 AM"))
        assertEquals(19 to 5, BiometricReconciliation.parseHhMm("7:05 PM"))
        assertEquals(0 to 30, BiometricReconciliation.parseHhMm("12:30 AM"))
        assertEquals(12 to 30, BiometricReconciliation.parseHhMm("12:30 PM"))
        assertEquals(7 to 45, BiometricReconciliation.parseHhMm("07:45:12"))
        assertNull(BiometricReconciliation.parseHhMm(""))
        assertNull(BiometricReconciliation.parseHhMm("--"))
        assertNull(BiometricReconciliation.parseHhMm("99:99"))
    }
}
