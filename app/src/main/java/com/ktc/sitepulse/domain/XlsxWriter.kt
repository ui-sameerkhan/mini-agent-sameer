package com.ktc.sitepulse.domain

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes .xlsx files directly, without Apache POI.
 *
 * Two reasons this exists rather than using POI's streaming workbook, which is the obvious answer:
 *
 *  1. POI's SXSSFSheet builds an AutoSizeColumnTracker in its constructor, which reaches
 *     java.awt.font.FontRenderContext. The Android runtime has no java.awt, so every
 *     createSheet() fails outright — streaming is simply unavailable on a phone.
 *  2. POI's in-memory workbook holds every cell as an object, which a full-company month
 *     (~104,000 rows of 19 columns) cannot fit inside the heap Android grants an app.
 *
 * An .xlsx is a zip of XML parts, so writing one is not exotic: rows are serialised straight into
 * the zip stream as they are produced and never accumulate. Memory stays flat regardless of row
 * count, and nothing here touches a class the Android runtime lacks.
 *
 * Rows arrive as a [Sequence] deliberately. Taking a List would mean materialising every row —
 * at a hundred thousand rows the row objects alone outweigh the workbook this replaces.
 *
 * Strings are written inline (t="inlineStr") rather than through a shared-strings table. A shared
 * table is smaller for repetitive data but has to be held in full until the end; inline strings
 * cost a little file size and keep the writer streaming.
 */
class XlsxWriter(private val outFile: File) : AutoCloseable {

    /** A cell style, referenced by index from the styles table written at close(). */
    enum class Style(val index: Int) {
        DEFAULT(0), TITLE(1), SUBTITLE(2), META(3), HEADER(4), DATA(5), DATA_BAND(6),
    }

    /** What a sheet needs before its rows: name, and the column widths to apply. */
    data class SheetSpec(val name: String, val columnWidths: List<Int>)

    private val zip = ZipOutputStream(FileOutputStream(outFile))
    private val sheets = mutableListOf<String>()
    private var closed = false

    /**
     * Writes one complete sheet. [rows] is consumed once, lazily, straight into the file.
     *
     * [rowCount] is needed up front only for the auto-filter range, which Excel wants as a fixed
     * reference — the caller knows it from the source collection without materialising the rows.
     */
    fun sheet(
        spec: SheetSpec,
        title: String,
        meta: List<String>,
        headers: List<String>,
        rowCount: Int,
        rows: Sequence<List<String>>,
    ) {
        check(!closed) { "Writer already closed" }
        val index = sheets.size + 1
        sheets.add(spec.name)

        zip.putNextEntry(ZipEntry("xl/worksheets/sheet$index.xml"))
        val w = BufferedWriter(OutputStreamWriter(zip, Charsets.UTF_8), 64 * 1024)

        val lastCol = (headers.size - 1).coerceAtLeast(0)
        w.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        w.write("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        if (spec.columnWidths.isNotEmpty()) {
            w.write("<cols>")
            spec.columnWidths.forEachIndexed { i, width ->
                w.write("""<col min="${i + 1}" max="${i + 1}" width="$width" customWidth="1"/>""")
            }
            w.write("</cols>")
        }

        w.write("<sheetData>")
        var r = 1

        // Letterhead: one merged, full-width banner row per line.
        fun bannerRow(text: String, style: Style, heightPt: Double) {
            w.write("""<row r="$r" ht="$heightPt" customHeight="1">""")
            // Every cell in the merged span still needs a style, or the fill stops at column A.
            for (c in 0..lastCol) {
                if (c == 0) cell(w, c, r, text, style) else cell(w, c, r, null, style)
            }
            w.write("</row>")
            r++
        }

        bannerRow(title, Style.TITLE, 26.0)
        bannerRow("KTC International Contracting LLC", Style.SUBTITLE, 16.0)
        meta.forEach { bannerRow(it, Style.META, 15.0) }
        w.write("""<row r="$r" ht="6" customHeight="1"/>""")
        r++

        val headerRow = r
        w.write("""<row r="$r">""")
        headers.forEachIndexed { c, h -> cell(w, c, r, h, Style.HEADER) }
        w.write("</row>")
        r++

        var written = 0
        for (row in rows) {
            val style = if (written % 2 == 1) Style.DATA_BAND else Style.DATA
            w.write("""<row r="$r">""")
            row.forEachIndexed { c, v -> cell(w, c, r, v, style) }
            w.write("</row>")
            r++
            written++
        }
        w.write("</sheetData>")

        // Merged regions and the auto-filter follow sheetData, per the schema's element order.
        val bannerRows = 2 + meta.size
        if (lastCol > 0) {
            w.write("""<mergeCells count="$bannerRows">""")
            for (i in 1..bannerRows) {
                w.write("""<mergeCell ref="${colName(0)}$i:${colName(lastCol)}$i"/>""")
            }
            w.write("</mergeCells>")
        }
        if (rowCount > 0) {
            val lastRow = headerRow + rowCount
            w.write("""<autoFilter ref="${colName(0)}$headerRow:${colName(lastCol)}$lastRow"/>""")
        }

        w.write("</worksheet>")
        w.flush()
        zip.closeEntry()
    }

    /** Writes the package parts that can only be built once every sheet is known. */
    override fun close() {
        if (closed) return
        closed = true
        try {
            entry("[Content_Types].xml", contentTypes())
            entry("_rels/.rels", rootRels())
            entry("xl/workbook.xml", workbook())
            entry("xl/_rels/workbook.xml.rels", workbookRels())
            entry("xl/styles.xml", styles())
        } finally {
            zip.close()
        }
    }

    // ---- Cells ---------------------------------------------------------------------------

    private fun cell(w: BufferedWriter, col: Int, row: Int, value: String?, style: Style) {
        val ref = "${colName(col)}$row"
        if (value.isNullOrEmpty()) {
            w.write("""<c r="$ref" s="${style.index}"/>""")
        } else {
            w.write("""<c r="$ref" s="${style.index}" t="inlineStr"><is><t xml:space="preserve">""")
            w.write(escape(value))
            w.write("</t></is></c>")
        }
    }

    /** 0 -> A, 25 -> Z, 26 -> AA. */
    private fun colName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (true) {
            sb.insert(0, ('A' + n % 26))
            n = n / 26 - 1
            if (n < 0) break
        }
        return sb.toString()
    }

    /**
     * XML escaping, plus stripping control characters. Attendance data carries names and free-text
     * reasons typed on phones; a stray control character makes the whole file unopenable, which is
     * a far worse outcome than losing one invisible character.
     */
    private fun escape(s: String): String {
        val sb = StringBuilder(s.length + 16)
        for (ch in s) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch == '\'' -> sb.append("&apos;")
                ch == '\n' || ch == '\t' -> sb.append(ch)
                ch.code < 0x20 -> Unit // unrepresentable in XML 1.0
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    // ---- Package parts -------------------------------------------------------------------

    private fun entry(path: String, body: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(body.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypes(): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        sheets.indices.forEach {
            append("""<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun rootRels(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            "</Relationships>"

    private fun workbook(): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """)
        append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
        sheets.forEachIndexed { i, name ->
            append("""<sheet name="${escape(sheetName(name))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        sheets.indices.forEach {
            append("""<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""")
        }
        append("""<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    /** Excel rejects these characters in a sheet name, and caps the length at 31. */
    private fun sheetName(raw: String): String =
        raw.replace(Regex("""[\\/*\[\]:?]"""), "-").take(31).ifBlank { "Sheet" }

    /**
     * The six styles the reports use, in the fixed order [Style] indexes them by. Colours are the
     * same brand values the POI version used, as ARGB.
     */
    private fun styles(): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        append("""<fonts count="7">""")
        append("""<font><sz val="11"/><name val="Calibri"/></font>""")                                        // 0 default
        append("""<font><b/><sz val="14"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>""")             // 1 title
        append("""<font><b/><i/><sz val="11"/><color rgb="FFD4A34A"/><name val="Calibri"/></font>""")         // 2 subtitle
        append("""<font><sz val="10"/><color rgb="FF5B6B63"/><name val="Calibri"/></font>""")                 // 3 meta
        append("""<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>""")             // 4 header
        append("""<font><sz val="11"/><color rgb="FF141F1A"/><name val="Calibri"/></font>""")                 // 5 data
        append("""<font><sz val="11"/><color rgb="FF141F1A"/><name val="Calibri"/></font>""")                 // 6 band
        append("</fonts>")

        // Excel requires fill 0 = none and fill 1 = gray125 before any custom fill.
        append("""<fills count="5">""")
        append("""<fill><patternFill patternType="none"/></fill>""")
        append("""<fill><patternFill patternType="gray125"/></fill>""")
        append("""<fill><patternFill patternType="solid"><fgColor rgb="FF0B4D3A"/><bgColor indexed="64"/></patternFill></fill>""") // 2 dark green
        append("""<fill><patternFill patternType="solid"><fgColor rgb="FF17875E"/><bgColor indexed="64"/></patternFill></fill>""") // 3 mid green
        append("""<fill><patternFill patternType="solid"><fgColor rgb="FFEAF3EE"/><bgColor indexed="64"/></patternFill></fill>""") // 4 light green
        append("</fills>")

        append("""<borders count="2">""")
        append("<border><left/><right/><top/><bottom/><diagonal/></border>")
        append("""<border><left style="thin"/><right style="thin"/><top style="thin"/><bottom style="thin"/><diagonal/></border>""")
        append("</borders>")

        append("""<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""")

        append("""<cellXfs count="7">""")
        append("""<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""")
        append("""<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center"/></xf>""")
        append("""<xf numFmtId="0" fontId="2" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center"/></xf>""")
        append("""<xf numFmtId="0" fontId="3" fillId="4" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center"/></xf>""")
        append("""<xf numFmtId="0" fontId="4" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>""")
        append("""<xf numFmtId="0" fontId="5" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"/>""")
        append("""<xf numFmtId="0" fontId="6" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>""")
        append("</cellXfs>")

        append("</styleSheet>")
    }
}
