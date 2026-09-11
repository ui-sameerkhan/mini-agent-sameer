package com.ktc.sitepulse.domain

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStreamReader

/** A parsed spreadsheet: raw headers (as typed in the file) + rows keyed by those headers. */
data class RawTable(val headers: List<String>, val rows: List<Map<String, String>>)

/**
 * Reads .xlsx/.xls (Apache POI) or .csv (hand-rolled RFC4180 parser, since POI
 * doesn't handle CSV) into a header->value row table, matching the web app's
 * XLSX.utils.sheet_to_json(sheet, {defval:""}) behavior (blank cells -> "").
 */
object SpreadsheetReader {
    /**
     * @param headerHints column names the caller expects, normalised the same way
     *   [ColumnMatcher] does. When given, the header row is *searched for* rather than assumed
     *   to be the first row — a report printed on company letterhead puts its real headers
     *   several rows down, and reading the letterhead as headers finds no columns at all.
     *   With no hints the behaviour is exactly as before: the first row is the header row.
     */
    fun read(context: Context, uri: Uri, fileName: String, headerHints: List<String> = emptyList()): RawTable {
        val isCsv = fileName.lowercase().endsWith(".csv")
        return if (isCsv) readCsv(context, uri, headerHints) else readXlsx(context, uri, headerHints)
    }

    /**
     * Index of the row that carries the column headings.
     *
     * Returns 0 when no hint matches, so a file this does not recognise behaves exactly as it
     * did before rather than guessing and silently reading the wrong row as headers.
     */
    fun findHeaderRow(rows: List<List<String>>, hints: List<String>, searchDepth: Int = 25): Int {
        if (hints.isEmpty()) return 0
        val wanted = hints.map { ColumnMatcher.normalize(it) }.toSet()
        for (i in 0 until minOf(rows.size, searchDepth)) {
            val matches = rows[i].count { ColumnMatcher.normalize(it) in wanted }
            if (matches > 0) return i
        }
        return 0
    }

    private fun readXlsx(context: Context, uri: Uri, headerHints: List<String>): RawTable {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open file" }
            return readXlsxStream(input, headerHints)
        }
    }

    /**
     * The workbook half of [readXlsx], with no Android types in sight — so a test can feed it a
     * real export and check what POI actually produces, rather than what one assumes it will.
     * The cell formatting in particular is the ERP's, not ours, and worth observing directly.
     */
    fun readXlsxStream(input: java.io.InputStream, headerHints: List<String> = emptyList()): RawTable {
        run {
            WorkbookFactory.create(input).use { wb ->
                val sheet = wb.getSheetAt(0)
                val fmt = DataFormatter()

                // Read enough of the top of the sheet to locate the headings.
                val scanEnd = minOf(sheet.lastRowNum, sheet.firstRowNum + 24)
                val scanned = (sheet.firstRowNum..scanEnd).map { r ->
                    val row = sheet.getRow(r)
                    if (row == null) emptyList()
                    else (0 until row.lastCellNum.coerceAtLeast(0)).map { c ->
                        fmt.formatCellValue(row.getCell(c)).trim()
                    }
                }
                val headerIdx = sheet.firstRowNum + findHeaderRow(scanned, headerHints)

                val headerRow = sheet.getRow(headerIdx) ?: return RawTable(emptyList(), emptyList())
                val headers = (0 until headerRow.lastCellNum.coerceAtLeast(0)).map { c ->
                    fmt.formatCellValue(headerRow.getCell(c)).trim()
                }
                val rows = mutableListOf<Map<String, String>>()
                for (r in (headerIdx + 1)..sheet.lastRowNum) {
                    val row = sheet.getRow(r) ?: continue
                    val map = LinkedHashMap<String, String>()
                    var anyNonBlank = false
                    headers.forEachIndexed { c, h ->
                        if (h.isBlank()) return@forEachIndexed
                        val v = fmt.formatCellValue(row.getCell(c)).trim()
                        if (v.isNotBlank()) anyNonBlank = true
                        map[h] = v
                    }
                    if (anyNonBlank) rows.add(map)
                }
                return RawTable(headers.filter { it.isNotBlank() }, rows)
            }
        }
    }

    private fun readCsv(context: Context, uri: Uri, headerHints: List<String>): RawTable {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open file" }
            val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
            val lines = reader.readLines()
            if (lines.isEmpty()) return RawTable(emptyList(), emptyList())
            val parsed = lines.take(25).map { parseCsvLine(it).map { c -> c.trim() } }
            val headerIdx = findHeaderRow(parsed, headerHints)
            val headers = parseCsvLine(lines[headerIdx]).map { it.trim() }
            val rows = lines.drop(headerIdx + 1).mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                val cells = parseCsvLine(line)
                val map = LinkedHashMap<String, String>()
                headers.forEachIndexed { i, h -> if (h.isNotBlank()) map[h] = cells.getOrElse(i) { "" }.trim() }
                map
            }
            return RawTable(headers.filter { it.isNotBlank() }, rows)
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { out.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}

/** Header normalization + synonym matching, matching the web app's flexible column matching. */
object ColumnMatcher {
    fun normalize(header: String): String = header.lowercase().filter { it.isLetter() }

    /** Finds the raw header (as it appears in the sheet) matching any of the given normalized synonyms. */
    fun resolve(headers: List<String>, synonyms: List<String>): String? =
        headers.find { normalize(it) in synonyms }

    fun cell(row: Map<String, String>, header: String?): String = header?.let { row[it] }.orEmpty()
}

data class ImportSummary(
    val added: Int,
    val updated: Int,
    val existingIdsBeingOverwritten: List<String>,
    val skippedNotFound: List<String> = emptyList(),
    val reassigned: List<Triple<String, String?, String>> = emptyList(), // name, oldSite, newSite
)
