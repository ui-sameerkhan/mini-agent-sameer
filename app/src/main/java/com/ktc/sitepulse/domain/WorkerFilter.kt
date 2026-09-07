package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.Worker

/** Sentinel used by every dropdown for "don't filter on this field". */
const val FILTER_ALL = "ALL"

/** Site filter value meaning "assigned to no project at all". */
const val FILTER_UNASSIGNED = "NONE"

enum class WorkerStatusFilter(val label: String) {
    ACTIVE("Active only"),
    LEFT("Left only"),
    ALL("All statuses"),
}

enum class WorkerSort(val label: String) {
    NAME("Name (A–Z)"),
    ID("Employee ID"),
    TRADE("Trade"),
    PROJECT("Project"),
    SUPPLIER("Supplier"),
}

/**
 * Everything the Employees screen filters on, in one value — so the screen holds a single piece
 * of state instead of six loose variables, and "Reset filters" is a one-line assignment.
 */
data class WorkerFilterState(
    val query: String = "",
    val site: String = FILTER_ALL,
    val trade: String = FILTER_ALL,
    val supplier: String = FILTER_ALL,
    val status: WorkerStatusFilter = WorkerStatusFilter.ACTIVE,
    val sort: WorkerSort = WorkerSort.NAME,
) {
    /** Drives the "Reset" button's enabled state and the "filters are active" hint. */
    val isDefault: Boolean
        get() = query.isBlank() && site == FILTER_ALL && trade == FILTER_ALL &&
            supplier == FILTER_ALL && status == WorkerStatusFilter.ACTIVE && sort == WorkerSort.NAME
}

object WorkerFilters {

    /**
     * Free-text search across every field a timekeeper would recognise a person by — ID, name,
     * trade, supplier and project — because on site people are looked up by whichever of those
     * the person asking happens to know.
     */
    fun matchesQuery(worker: Worker, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return worker.id.contains(q, ignoreCase = true) ||
            worker.name.contains(q, ignoreCase = true) ||
            worker.designation.contains(q, ignoreCase = true) ||
            worker.company.orEmpty().contains(q, ignoreCase = true) ||
            worker.site.orEmpty().contains(q, ignoreCase = true)
    }

    fun apply(workers: List<Worker>, state: WorkerFilterState): List<Worker> {
        val filtered = workers.filter { w ->
            val statusOk = when (state.status) {
                WorkerStatusFilter.ACTIVE -> !w.isLeft
                WorkerStatusFilter.LEFT -> w.isLeft
                WorkerStatusFilter.ALL -> true
            }
            val siteOk = when (state.site) {
                FILTER_ALL -> true
                FILTER_UNASSIGNED -> w.site.isNullOrBlank()
                else -> w.site == state.site
            }
            val tradeOk = state.trade == FILTER_ALL || w.designation.equals(state.trade, ignoreCase = true)
            val supplierOk = when (state.supplier) {
                FILTER_ALL -> true
                // "" is the KTC-own bucket — these workers have no company recorded at all.
                "" -> !w.isOutsourced
                else -> w.company.equals(state.supplier, ignoreCase = true)
            }
            statusOk && siteOk && tradeOk && supplierOk && matchesQuery(w, state.query)
        }
        return sort(filtered, state.sort)
    }

    private fun sort(workers: List<Worker>, sort: WorkerSort): List<Worker> = when (sort) {
        WorkerSort.NAME -> workers.sortedBy { it.name.lowercase() }
        // Employee IDs are numeric in practice but stored as strings, so "10" must not sort
        // before "9" — fall back to the string only when an ID genuinely isn't a number.
        WorkerSort.ID -> workers.sortedWith(compareBy({ it.id.toLongOrNull() ?: Long.MAX_VALUE }, { it.id }))
        WorkerSort.TRADE -> workers.sortedWith(compareBy({ it.designation.lowercase() }, { it.name.lowercase() }))
        WorkerSort.PROJECT -> workers.sortedWith(compareBy({ it.site.orEmpty().lowercase() }, { it.name.lowercase() }))
        WorkerSort.SUPPLIER -> workers.sortedWith(compareBy({ it.company.orEmpty().lowercase() }, { it.name.lowercase() }))
    }

    /** Distinct trades present on the roster, for the Trade dropdown. */
    fun tradeOptions(workers: List<Worker>): List<String> =
        workers.map { it.designation.trim() }.filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() }

    /** Distinct supplier names present on the roster, for the Supplier dropdown. */
    fun supplierOptions(workers: List<Worker>): List<String> =
        workers.mapNotNull { it.company?.trim() }.filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() }

    /**
     * True when [id] already belongs to a different worker. Employee IDs are the Firestore
     * document ID, so saving a duplicate silently merges into — and corrupts — the existing
     * person's record rather than failing; the Add form has to catch it up front.
     */
    fun isDuplicateId(workers: List<Worker>, id: String): Boolean {
        val trimmed = id.trim()
        return trimmed.isNotEmpty() && workers.any { it.id.equals(trimmed, ignoreCase = true) }
    }
}
