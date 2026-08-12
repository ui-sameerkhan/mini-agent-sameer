package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.Worker

sealed class ParsedImport {
    data class Ok(
        val records: List<Worker>,
        val overwritingIds: List<String>,
        val newCount: Int,
        val skippedNotFound: List<String> = emptyList(),
        val reassignments: List<Triple<String, String?, String>> = emptyList(), // name, old site, new site
    ) : ParsedImport()

    data class ColumnsNotFound(val message: String) : ParsedImport()
    data class NoValidRows(val message: String) : ParsedImport()
}

/** Mirrors uploadExcel() / uploadOutsource() / uploadRoster() from the web app exactly. */
object WorkersImport {

    private val idSynonyms = listOf("employeeid", "empid", "workerid", "id")
    private val nameSynonyms = listOf("employeename", "name")
    private val designationSynonyms = listOf("designation", "trade", "role")
    private val snoSynonyms = listOf("sno", "serial", "srno")
    private val siteSynonyms = listOf("site", "project", "projectcode")
    private val companySynonyms = listOf("companyname", "company", "vendor", "contractor", "agency")
    private val outsourceNameSynonyms = listOf("name", "workername", "employeename")
    private val projectCodeSynonyms = listOf("projectcode", "project", "site")
    private val alignedDateSynonyms = listOf("aligneddate", "joindate", "arrivaldate", "date")

    fun parseWorkersMaster(table: RawTable, existing: List<Worker>, nextSnoStart: Long): ParsedImport {
        val idH = ColumnMatcher.resolve(table.headers, idSynonyms)
        val nameH = ColumnMatcher.resolve(table.headers, nameSynonyms)
        if (idH == null || nameH == null) {
            return ParsedImport.ColumnsNotFound(
                "Couldn't find Employee ID / Employee Name columns. Columns found: ${table.headers.joinToString(", ")}"
            )
        }
        val designationH = ColumnMatcher.resolve(table.headers, designationSynonyms)
        val snoH = ColumnMatcher.resolve(table.headers, snoSynonyms)
        val siteH = ColumnMatcher.resolve(table.headers, siteSynonyms)

        val existingById = existing.associateBy { it.id }
        var sno = nextSnoStart
        val records = mutableListOf<Worker>()
        val overwriting = mutableListOf<String>()

        for (row in table.rows) {
            val id = ColumnMatcher.cell(row, idH).trim()
            val name = ColumnMatcher.cell(row, nameH).trim()
            if (id.isBlank() || name.isBlank()) continue
            val designation = ColumnMatcher.cell(row, designationH).ifBlank { "Worker" }
            val site = ColumnMatcher.cell(row, siteH).trim().uppercase().ifBlank { null }
            val rowSno = ColumnMatcher.cell(row, snoH).toLongOrNull()
            val existingWorker = existingById[id]
            if (existingWorker != null) overwriting.add(id)
            records.add(
                Worker(
                    sno = rowSno ?: existingWorker?.sno ?: sno++,
                    id = id,
                    name = name,
                    designation = designation,
                    site = site,
                    status = "active",
                )
            )
        }
        if (records.isEmpty()) return ParsedImport.NoValidRows("No rows with both an Employee ID and Employee Name were found.")
        return ParsedImport.Ok(records, overwriting, records.size - overwriting.size)
    }

    fun parseOutsource(table: RawTable, existing: List<Worker>, nextSnoStart: Long): ParsedImport {
        val idH = ColumnMatcher.resolve(table.headers, idSynonyms)
        val nameH = ColumnMatcher.resolve(table.headers, outsourceNameSynonyms)
        val companyH = ColumnMatcher.resolve(table.headers, companySynonyms)
        if (idH == null || nameH == null || companyH == null) {
            return ParsedImport.ColumnsNotFound(
                "Couldn't find Worker ID / Name / Company Name columns. Columns found: ${table.headers.joinToString(", ")}"
            )
        }
        val designationH = ColumnMatcher.resolve(table.headers, designationSynonyms)
        val existingById = existing.associateBy { it.id }
        var sno = nextSnoStart
        val records = mutableListOf<Worker>()
        val overwriting = mutableListOf<String>()

        for (row in table.rows) {
            val id = ColumnMatcher.cell(row, idH).trim()
            val name = ColumnMatcher.cell(row, nameH).trim()
            val company = ColumnMatcher.cell(row, companyH).trim()
            if (id.isBlank() || name.isBlank() || company.isBlank()) continue
            val existingWorker = existingById[id]
            val designation = ColumnMatcher.cell(row, designationH).ifBlank {
                existingWorker?.designation?.ifBlank { null } ?: "Outsourced"
            }
            if (existingWorker != null) overwriting.add(id)
            records.add(
                Worker(
                    sno = existingWorker?.sno ?: sno++,
                    id = id,
                    name = name,
                    designation = designation,
                    company = company,
                    site = existingWorker?.site,
                    status = "active",
                )
            )
        }
        if (records.isEmpty()) return ParsedImport.NoValidRows("No rows with Worker ID, Name and Company Name were found.")
        return ParsedImport.Ok(records, overwriting, records.size - overwriting.size)
    }

    fun parseRoster(table: RawTable, existing: List<Worker>, todayStr: String): ParsedImport {
        val codeH = ColumnMatcher.resolve(table.headers, projectCodeSynonyms)
        val idH = ColumnMatcher.resolve(table.headers, idSynonyms)
        if (codeH == null || idH == null) {
            return ParsedImport.ColumnsNotFound(
                "Couldn't find Project Code / Worker ID columns. Columns found: ${table.headers.joinToString(", ")}"
            )
        }
        val dateH = ColumnMatcher.resolve(table.headers, alignedDateSynonyms)
        val existingById = existing.associateBy { it.id }
        val records = mutableListOf<Worker>()
        val notFound = mutableListOf<String>()
        val reassignments = mutableListOf<Triple<String, String?, String>>()

        for (row in table.rows) {
            val code = ColumnMatcher.cell(row, codeH).trim().uppercase()
            val id = ColumnMatcher.cell(row, idH).trim()
            if (code.isBlank() || id.isBlank()) continue
            val worker = existingById[id]
            if (worker == null) {
                notFound.add(id)
                continue
            }
            val alignedDate = ColumnMatcher.cell(row, dateH).ifBlank { todayStr }
            if (!worker.site.isNullOrBlank() && worker.site != code) {
                reassignments.add(Triple(worker.name, worker.site, code))
            }
            records.add(
                worker.copy(site = code, alignedDate = alignedDate, status = "active")
            )
        }
        if (records.isEmpty() && notFound.isEmpty()) {
            return ParsedImport.NoValidRows("No rows with both Project Code and Worker ID were found.")
        }
        if (records.isEmpty()) {
            return ParsedImport.ColumnsNotFound("None of the Worker IDs in this file exist yet: ${notFound.take(10).joinToString(", ")}${if (notFound.size > 10) "…" else ""}")
        }
        return ParsedImport.Ok(records, emptyList(), records.size, notFound, reassignments)
    }
}
