package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Worker
import kotlin.math.abs

/**
 * Cross-checks attendance marked in SitePulse against the ERP biometric report (report 8127).
 *
 * This is the answer to the one question GPS cannot settle. A geofence proves a *phone* was on
 * site; it cannot prove the man whose badge was scanned was standing next to it. The biometric
 * system can, because the worker had to put his own finger on a reader — so where the two
 * sources disagree, somebody has some explaining to do.
 *
 * It is a detective control, not a preventive one: nothing here stops a bad mark being made at
 * the gate, it makes one findable afterwards. That is usually enough, because a foreman who
 * knows the reconciliation runs does not make the mark in the first place.
 *
 * Read the findings honestly. [Verdict.MARKED_NOT_PUNCHED] is the suspicious one, but it has
 * innocent explanations too — a worker who never reaches a biometric reader cannot punch at all.
 * See [Summary.coverage].
 */
object BiometricReconciliation {

    enum class Verdict {
        /** Both sources agree the worker was present. */
        AGREED,

        /** Marked present in SitePulse with no biometric punch at all. The one worth looking at. */
        MARKED_NOT_PUNCHED,

        /** Biometric shows he was here; SitePulse has no record. An attendance that was missed. */
        PUNCHED_NOT_MARKED,

        /** Both present, but the in-times are far enough apart to be worth a look. */
        TIME_MISMATCH,
    }

    data class Finding(
        val workerId: String,
        val name: String,
        val date: String,
        val siteCode: String,
        val verdict: Verdict,
        /** Who marked the SitePulse record — the accountable name, blank when there is no record. */
        val markedBy: String,
        val sitePulseIn: String,
        val biometricIn: String,
        /** Absolute in-time difference in minutes, when both sides have a time. */
        val gapMinutes: Int?,
    ) {
        val needsReview: Boolean get() = verdict != Verdict.AGREED
    }

    data class Summary(
        val findings: List<Finding>,
        val agreed: Int,
        val markedNotPunched: Int,
        val punchedNotMarked: Int,
        val timeMismatch: Int,
        val dates: List<String>,
        /**
         * How many of the marked workers appear anywhere in the biometric file. If this is low,
         * the biometric simply does not cover these people — a remote project with no reader on
         * it — and the MARKED_NOT_PUNCHED count means nothing. Surfaced so nobody reads an
         * absence of coverage as evidence of fraud.
         */
        val coverage: Int,
        val markedTotal: Int,
    ) {
        val reviewCount: Int get() = markedNotPunched + punchedNotMarked + timeMismatch

        /** True when too little of the marked workforce is on the biometric to draw conclusions. */
        val coverageTooLowToJudge: Boolean
            get() = markedTotal > 0 && coverage * 2 < markedTotal
    }

    /**
     * @param toleranceMinutes how far apart the two in-times may be before it is worth flagging.
     *   Generous by default: the biometric sits at a gate or camp the worker reaches before the
     *   workface, so a real gap of half an hour is normal and means nothing.
     */
    fun run(
        attendance: List<Attendance>,
        punches: List<BiometricPunch>,
        workers: List<Worker>,
        toleranceMinutes: Int = 90,
    ): Summary {
        val dates = punches.map { it.date }.distinct().sorted()
        // Only judge days the biometric file actually covers. Comparing against a date the report
        // does not include would mark an entire day's attendance as unverified.
        val scoped = attendance.filter { it.date in dates && it.hasIn }

        val nameById = workers.associate { it.id to it.name }
        val punchByKey = punches.associateBy { it.workerId to it.date }
        val attByKey = scoped.associateBy { it.workerId to it.date }

        val findings = mutableListOf<Finding>()

        for (a in scoped) {
            val punch = punchByKey[a.workerId to a.date]
            val spIn = DateUtils.formatTimeHm(a.checkIn)
            val bioIn = punch?.punchIn.orEmpty()
            val gap = minuteGap(a, bioIn)
            val verdict = when {
                punch == null || !punch.hasPunch -> Verdict.MARKED_NOT_PUNCHED
                gap != null && gap > toleranceMinutes -> Verdict.TIME_MISMATCH
                else -> Verdict.AGREED
            }
            findings.add(
                Finding(
                    workerId = a.workerId,
                    name = nameById[a.workerId] ?: punch?.name.orEmpty(),
                    date = a.date,
                    siteCode = a.siteCode,
                    verdict = verdict,
                    markedBy = a.markedBy,
                    sitePulseIn = spIn,
                    biometricIn = bioIn,
                    gapMinutes = gap,
                )
            )
        }

        // The other direction: the biometric saw him, SitePulse did not. An unpaid day, usually.
        for (p in punches) {
            if (!p.hasPunch) continue
            if (attByKey.containsKey(p.workerId to p.date)) continue
            findings.add(
                Finding(
                    workerId = p.workerId,
                    name = nameById[p.workerId] ?: p.name,
                    date = p.date,
                    siteCode = "",
                    verdict = Verdict.PUNCHED_NOT_MARKED,
                    markedBy = "",
                    sitePulseIn = "",
                    biometricIn = p.punchIn,
                    gapMinutes = null,
                )
            )
        }

        val punchedIds = punches.filter { it.hasPunch }.map { it.workerId }.toHashSet()
        val markedIds = scoped.map { it.workerId }.toHashSet()

        val sorted = findings.sortedWith(
            compareBy({ verdictOrder(it.verdict) }, { it.date }, { it.workerId })
        )

        return Summary(
            findings = sorted,
            agreed = sorted.count { it.verdict == Verdict.AGREED },
            markedNotPunched = sorted.count { it.verdict == Verdict.MARKED_NOT_PUNCHED },
            punchedNotMarked = sorted.count { it.verdict == Verdict.PUNCHED_NOT_MARKED },
            timeMismatch = sorted.count { it.verdict == Verdict.TIME_MISMATCH },
            dates = dates,
            coverage = markedIds.count { it in punchedIds },
            markedTotal = markedIds.size,
        )
    }

    /** Worst first, so the list opens on what someone actually has to act on. */
    private fun verdictOrder(v: Verdict): Int = when (v) {
        Verdict.MARKED_NOT_PUNCHED -> 0
        Verdict.TIME_MISMATCH -> 1
        Verdict.PUNCHED_NOT_MARKED -> 2
        Verdict.AGREED -> 3
    }

    /** Minutes between the SitePulse check-in and the biometric punch, or null if either is absent. */
    private fun minuteGap(a: Attendance, biometricIn: String): Int? {
        val sp = DateUtils.localHourMinute(a.checkIn) ?: return null
        val bio = parseHhMm(biometricIn) ?: return null
        return abs((sp.first * 60 + sp.second) - (bio.first * 60 + bio.second))
    }

    /** "07:45", "7:45 AM", "07:45:12" — whatever shape the ERP prints its times in. */
    internal fun parseHhMm(raw: String): Pair<Int, Int>? {
        val s = raw.trim()
        if (s.isBlank()) return null
        val upper = s.uppercase()
        val pm = upper.contains("PM")
        val am = upper.contains("AM")
        val digits = upper.replace("AM", "").replace("PM", "").trim()
        val parts = digits.split(':', '.').mapNotNull { it.trim().toIntOrNull() }
        if (parts.size < 2) return null
        var hour = parts[0]
        val minute = parts[1]
        if (hour !in 0..23 || minute !in 0..59) return null
        if (pm && hour < 12) hour += 12
        if (am && hour == 12) hour = 0
        return hour to minute
    }
}
