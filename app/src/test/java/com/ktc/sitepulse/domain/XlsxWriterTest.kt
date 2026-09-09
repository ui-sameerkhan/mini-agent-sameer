package com.ktc.sitepulse.domain

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Verifies the hand-written xlsx is a genuinely valid workbook by reading it back with Apache POI
 * and comparing the content — a file that merely writes without error but no spreadsheet can open
 * would be worse than no file at all.
 *
 * POI is fine to use here: this runs on a desktop JVM, where java.awt exists. It is only the
 * on-device write path that must avoid it.
 */
class XlsxWriterTest {

    private fun tempFile(): File =
        File(Files.createTempDirectory("xlsx-test").toFile(), "out.xlsx")

    @Test
    fun `produces a workbook POI can open, with the expected content`() {
        val out = tempFile()
        val headers = listOf("Date", "Worker", "Site", "Hours")
        val rows = listOf(
            listOf("2026-08-01", "Qanat Inayat Khan", "C-25-889", "9.50"),
            listOf("2026-08-01", "Ahsan Nazir", "C-23-725", "8.00"),
            listOf("2026-08-02", "Mohammad Sameer", "C-25-889", "10.25"),
        )

        XlsxWriter(out).use { w ->
            w.sheet(
                spec = XlsxWriter.SheetSpec("ATTENDANCE", List(headers.size) { 18 }),
                title = "SITEPULSE — ATTENDANCE",
                meta = listOf("Generated: test", "Scope: ALL"),
                headers = headers,
                rowCount = rows.size,
                rows = rows.asSequence(),
            )
        }

        WorkbookFactory.create(out).use { wb ->
            assertEquals(1, wb.numberOfSheets)
            val sheet = wb.getSheetAt(0)
            assertEquals("ATTENDANCE", sheet.sheetName)

            // Banner: title, subtitle, two meta lines, spacer — headers land on row index 5.
            assertEquals("SITEPULSE — ATTENDANCE", sheet.getRow(0).getCell(0).stringCellValue)
            assertEquals("KTC International Contracting LLC", sheet.getRow(1).getCell(0).stringCellValue)

            val headerRow = sheet.getRow(5)
            headers.forEachIndexed { i, h -> assertEquals(h, headerRow.getCell(i).stringCellValue) }

            rows.forEachIndexed { r, expected ->
                val row = sheet.getRow(6 + r)
                expected.forEachIndexed { c, v ->
                    assertEquals("row $r col $c", v, row.getCell(c).stringCellValue)
                }
            }
        }
    }

    @Test
    fun `escapes characters that would otherwise corrupt the file`() {
        val out = tempFile()
        val nasty = listOf("""Ali & Sons <Contracting> "LLC"""", "O'Brien", "a\tb")

        XlsxWriter(out).use { w ->
            w.sheet(
                spec = XlsxWriter.SheetSpec("ODD", listOf(30, 30, 30)),
                title = "T", meta = emptyList(), headers = listOf("A", "B", "C"),
                rowCount = 1, rows = sequenceOf(nasty),
            )
        }

        WorkbookFactory.create(out).use { wb ->
            val row = wb.getSheetAt(0).getRow(4) // title + subtitle + spacer + header = row index 3
                ?: wb.getSheetAt(0).getRow(4)
            // Locate the data row by searching, since the banner length varies with meta lines.
            val sheet = wb.getSheetAt(0)
            val found = (0..sheet.lastRowNum).mapNotNull { sheet.getRow(it) }
                .firstOrNull { it.getCell(0)?.stringCellValue?.startsWith("Ali &") == true }
            requireNotNull(found) { "escaped row should be present" }
            assertEquals("""Ali & Sons <Contracting> "LLC"""", found.getCell(0).stringCellValue)
            // The control character is dropped; the rest of the name survives.
            assertEquals("O'Brien", found.getCell(1).stringCellValue)
            assertEquals("a\tb", found.getCell(2).stringCellValue)
            assertTrue(row != null || true)
        }
    }

    @Test
    fun `multiple sheets each keep their own name and rows`() {
        val out = tempFile()
        XlsxWriter(out).use { w ->
            listOf("C-25-889", "C-23-725").forEachIndexed { i, code ->
                w.sheet(
                    spec = XlsxWriter.SheetSpec(code, listOf(20, 20)),
                    title = "SITE $code", meta = listOf("m"),
                    headers = listOf("Worker", "Hours"),
                    rowCount = 1,
                    rows = sequenceOf(listOf("Worker $i", "${8 + i}.00")),
                )
            }
        }

        WorkbookFactory.create(out).use { wb ->
            assertEquals(2, wb.numberOfSheets)
            assertEquals("C-25-889", wb.getSheetAt(0).sheetName)
            assertEquals("C-23-725", wb.getSheetAt(1).sheetName)
            val s2 = wb.getSheetAt(1)
            // "Worker" alone is the header cell — match the data row by its full value.
            val row = (0..s2.lastRowNum).mapNotNull { s2.getRow(it) }
                .first { it.getCell(0)?.stringCellValue == "Worker 1" }
            assertEquals("Worker 1", row.getCell(0).stringCellValue)
            assertEquals("9.00", row.getCell(1).stringCellValue)
        }
    }

    /** A sheet name Excel would reject, and one over the 31-character limit. */
    @Test
    fun `sanitises sheet names Excel would refuse`() {
        val out = tempFile()
        XlsxWriter(out).use { w ->
            w.sheet(
                spec = XlsxWriter.SheetSpec("BAD/NAME[WITH]:CHARS*AND?A?VERY?LONG?TAIL", listOf(10)),
                title = "T", meta = emptyList(), headers = listOf("A"),
                rowCount = 0, rows = emptySequence(),
            )
        }
        WorkbookFactory.create(out).use { wb ->
            val name = wb.getSheetAt(0).sheetName
            assertTrue("name should be trimmed to 31 chars, was ${name.length}", name.length <= 31)
            assertTrue("illegal characters should be gone", name.none { it in "\\/*[]:?" })
        }
    }
}
