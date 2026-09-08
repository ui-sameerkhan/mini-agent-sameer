package com.ktc.sitepulse.domain

import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Leave
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.Worker

/**
 * One row of a manpower breakdown. The counts are identical whether the grouping is by trade,
 * by project, or by supplier, so all three tables on the dashboard render from this one type
 * instead of three near-duplicate row composables.
 */
data class ManpowerRow(
    val key: String,
    val label: String,
    val total: Int,
    val present: Int,
    val onLeave: Int,
) {
    /** Derived rather than stored — absent is "everyone left over", so it can never disagree
     * with the other three counts no matter how the grouping was built. */
    val absent: Int get() = (total - present - onLeave).coerceAtLeast(0)
}

/**
 * A complete day's manpower picture, computed in one pass so the dashboard renders every card
 * and breakdown from a single value rather than recounting the same lists per widget.
 */
data class ManpowerSummary(
    val date: String,
    val totalEmployees: Int,
    val present: Int,
    val absent: Int,
    val late: Int,
    val onLeave: Int,
    val checkedOut: Int,
    val ktcEmployees: Int,
    val supplierEmployees: Int,
    val totalProjects: Int,
    val activeProjects: Int,
    val byTrade: List<ManpowerRow>,
    val byProject: List<ManpowerRow>,
    val bySupplier: List<ManpowerRow>,
) {
    val presentPercent: Int get() = if (totalEmployees == 0) 0 else (present * 100) / totalEmployees

    companion object {
        val EMPTY = ManpowerSummary(
            date = "", totalEmployees = 0, present = 0, absent = 0, late = 0, onLeave = 0,
            checkedOut = 0, ktcEmployees = 0, supplierEmployees = 0, totalProjects = 0,
            activeProjects = 0, byTrade = emptyList(), byProject = emptyList(), bySupplier = emptyList(),
        )
    }
}

/** Label used for KTC's own (non-outsourced) staff wherever a supplier name would otherwise go. */
const val KTC_OWN_LABEL = "KTC (own manpower)"

/** Shown instead of a blank cell when a worker has no trade or no project recorded. */
private const val UNSPECIFIED_TRADE = "Unspecified Trade"
private const val UNASSIGNED_PROJECT = "Unassigned"

object Manpower {

    /**
     * Only "approved" leave excuses a worker — a pending application doesn't yet. Mirrors the
     * rule already applied by the Absent Report, kept here as one pure function so the dashboard
     * and the reports can never drift apart on what "on leave" means.
     */
    fun isOnLeave(leaves: List<Leave>, workerId: String, dateStr: String): Boolean =
        isOnLeaveIndexed(leaves.filter { it.workerId == workerId }, dateStr)

    /** The same rule against one worker's leave records, for callers that already grouped them. */
    internal fun isOnLeaveIndexed(workerLeaves: List<Leave>?, dateStr: String): Boolean =
        workerLeaves?.any {
            it.status == "approved" &&
                runCatching { DateUtils.isWithin(dateStr, it.fromDate, it.toDate.ifBlank { it.fromDate }) }
                    .getOrDefault(false)
        } ?: false

    /**
     * A day-shift check-in at or after the site's late threshold. Night shifts are excluded —
     * their start hour is a different thing entirely, and flagging them against a daytime
     * threshold would mark an entire night crew late every single night.
     */
    fun isLate(attendance: Attendance, siteByCode: Map<String, Site>): Boolean {
        if (!attendance.hasIn) return false
        if (attendance.shift == "Night") return false
        val threshold = siteByCode[attendance.siteCode]?.lateAfterHour?.toInt()
            ?: Constants.DEFAULT_LATE_AFTER_HOUR
        val hour = DateUtils.localHourMinute(attendance.checkIn)?.first ?: return false
        return hour >= threshold
    }

    /**
     * Computes the whole day in a single pass.
     *
     * Workers marked "left" are excluded from every count — they're kept in Firestore as
     * historical record, but counting an ex-employee as "absent" every day would make the
     * absent figure meaningless for daily manpower management.
     */
    fun compute(
        workers: List<Worker>,
        attendance: List<Attendance>,
        leaves: List<Leave>,
        sites: List<Site>,
        date: String,
    ): ManpowerSummary {
        val active = workers.filter { !it.isLeft }
        val siteByCode = sites.associateBy { it.code }
        val siteNameByCode = sites.associate { it.code to it.name }

        // Attendance can legitimately hold more than one row per worker across sites; collapse to
        // a set so a worker is only ever counted once toward "present".
        val presentIds = attendance.filter { it.hasIn }.map { it.workerId }.toSet()
        val checkedOutIds = attendance.filter { it.hasOut }.map { it.workerId }.toSet()

        // Leave is indexed by worker once rather than rescanned per worker: on a roster of a few
        // thousand with a few hundred open leave records, the naive form is millions of
        // comparisons on the path that drives the dashboard.
        val leavesByWorker = leaves.groupBy { it.workerId }
        val onLeaveIds = active.asSequence()
            .filter { w -> isOnLeaveIndexed(leavesByWorker[w.id], date) }
            .map { it.id }
            .toSet()

        val activeIds = active.mapTo(HashSet()) { it.id }
        val presentCount = active.count { it.id in presentIds }
        val onLeaveCount = active.count { it.id in onLeaveIds }
        val lateCount = attendance
            .asSequence()
            .filter { it.workerId in presentIds && it.workerId in activeIds && isLate(it, siteByCode) }
            .map { it.workerId }
            .toSet()
            .size

        fun breakdown(grouping: (Worker) -> Pair<String, String>): List<ManpowerRow> =
            active.groupBy { grouping(it) }
                .map { (keyLabel, group) ->
                    val (key, label) = keyLabel
                    ManpowerRow(
                        key = key,
                        label = label,
                        total = group.size,
                        present = group.count { it.id in presentIds },
                        onLeave = group.count { it.id in onLeaveIds },
                    )
                }
                // Biggest crews first — that's the order a timekeeper scans a manpower sheet in.
                .sortedWith(compareByDescending<ManpowerRow> { it.total }.thenBy { it.label })

        val byTrade = breakdown { w ->
            val trade = w.designation.trim().ifBlank { UNSPECIFIED_TRADE }
            trade to trade
        }
        val byProject = breakdown { w ->
            val code = w.site?.trim().orEmpty().ifBlank { "" }
            if (code.isBlank()) "" to UNASSIGNED_PROJECT
            else code to (siteNameByCode[code]?.let { "$it ($code)" } ?: code)
        }
        val bySupplier = breakdown { w ->
            val company = w.company?.trim().orEmpty()
            if (company.isBlank()) "" to KTC_OWN_LABEL else company to company
        }

        // "Active" = a project with at least one worker assigned to it today. Sites are never
        // deleted when a job finishes, so a raw site count would keep counting closed projects.
        val activeProjectCodes = active.mapNotNull { it.site?.trim()?.takeIf { c -> c.isNotBlank() } }.toSet()

        return ManpowerSummary(
            date = date,
            totalEmployees = active.size,
            present = presentCount,
            absent = (active.size - presentCount - onLeaveCount).coerceAtLeast(0),
            late = lateCount,
            onLeave = onLeaveCount,
            checkedOut = active.count { it.id in checkedOutIds },
            ktcEmployees = active.count { !it.isOutsourced },
            supplierEmployees = active.count { it.isOutsourced },
            totalProjects = sites.size,
            activeProjects = sites.count { it.code in activeProjectCodes },
            byTrade = byTrade,
            byProject = byProject,
            bySupplier = bySupplier,
        )
    }
}
