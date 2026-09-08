package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.GpsPoint
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.Worker
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Proves the monthly export survives a full-company month.
 *
 * The report used to build the entire workbook in memory, which at roughly 4,000 workers over a
 * month — some 104,000 rows of 19 columns — exceeds the heap an Android app is given and takes
 * the app down rather than producing a file. These tests generate that volume for real and assert
 * the file comes out, so the claim rests on a measurement rather than on reasoning about it.
 *
 * Run with:  ./gradlew :app:testDebugUnitTest --tests '*ReportEngineScaleTest*'
 */
class ReportEngineScaleTest {

    private val workerCount = 4_000
    private val workingDays = 26

    private fun sites() = listOf(
        Site(code = "C-25-889", name = "JVC", lat = 25.05, lng = 55.20, radius = 500),
        Site(code = "C-23-725", name = "Deira Island", lat = 25.28, lng = 55.33, radius = 500),
        Site(code = "C-26-923", name = "EMAAR South", lat = 24.85, lng = 55.15, radius = 500),
    )

    private fun workers(): List<Worker> {
        val trades = listOf("Carpenter", "Assistant Carpenter", "Mason", "Helper", "Steel Fixer", "Electrician", "Watchman")
        val suppliers = listOf(null, "Al Nahda Manpower", "Gulf Labour Supply")
        val codes = sites().map { it.code }
        return (1..workerCount).map { i ->
            Worker(
                sno = i.toLong(),
                id = "W%05d".format(i),
                name = "Worker Number $i",
                designation = trades[i % trades.size],
                company = suppliers[i % suppliers.size],
                site = codes[i % codes.size],
                status = "active",
            )
        }
    }

    /** One attendance row per worker per working day — the shape a real month produces. */
    private fun monthAttendance(month: String, workers: List<Worker>): List<Attendance> {
        val siteNames = sites().associate { it.code to it.name }
        val rows = ArrayList<Attendance>(workers.size * workingDays)
        for (day in 1..workingDays) {
            val date = "%s-%02d".format(month, day)
            for (w in workers) {
                val code = w.site!!
                rows.add(
                    Attendance(
                        workerId = w.id,
                        date = date,
                        siteCode = code,
                        siteName = siteNames[code] ?: code,
                        lastAction = "${date}T14:00:00Z",
                        markedBy = "timekeeper@ktc-manpower.com",
                        shift = if (w.sno % 5 == 0L) "Night" else "Day",
                        checkIn = "${date}T04:00:00Z",
                        inGps = GpsPoint(25.05, 55.20),
                        inDist = 42,
                        out = "${date}T14:00:00Z",
                        outGps = GpsPoint(25.05, 55.20),
                        outDist = 51,
                        alignedSite = code,
                    )
                )
            }
        }
        return rows
    }

    @Test
    fun `full company month export completes`() {
        val outDir = Files.createTempDirectory("sitepulse-scale").toFile()
        val workers = workers()
        val attendance = monthAttendance("2026-08", workers)

        assertTrue(
            "fixture should reproduce a full-company month",
            attendance.size >= 100_000,
        )

        val runtime = Runtime.getRuntime()
        runtime.gc()
        val before = runtime.totalMemory() - runtime.freeMemory()
        val started = System.currentTimeMillis()

        val file: File = ReportEngine.generate(
            outputDir = outDir,
            attendanceRows = attendance,
            workers = workers,
            sites = sites(),
            leaves = emptyList(),
            holidays = emptyList(),
            params = ReportEngine.Params(
                dateOrMonth = "2026-08",
                range = "month",
                siteScope = "ALL",
                generatedBy = "scale-test",
            ),
        )

        val elapsed = System.currentTimeMillis() - started
        runtime.gc()
        val after = runtime.totalMemory() - runtime.freeMemory()

        println(
            "[scale] ${attendance.size} attendance rows -> ${file.name} " +
                "(${file.length() / 1024} KB) in ${elapsed}ms; " +
                "heap ${(before / 1024 / 1024)}MB -> ${(after / 1024 / 1024)}MB"
        )

        assertTrue("report file should exist", file.exists())
        assertTrue("report file should not be empty", file.length() > 10_000)

        file.delete()
        outDir.deleteRecursively()
    }

    @Test
    fun `single day export for the full company completes`() {
        val outDir = Files.createTempDirectory("sitepulse-scale-day").toFile()
        val workers = workers()
        val attendance = monthAttendance("2026-08", workers).filter { it.date == "2026-08-01" }

        val file = ReportEngine.generate(
            outputDir = outDir,
            attendanceRows = attendance,
            workers = workers,
            sites = sites(),
            leaves = emptyList(),
            holidays = emptyList(),
            params = ReportEngine.Params(
                dateOrMonth = "2026-08-01",
                range = "day",
                siteScope = "ALL",
                generatedBy = "scale-test",
            ),
        )

        println("[scale] daily: ${attendance.size} rows -> ${file.length() / 1024} KB")
        assertTrue(file.exists() && file.length() > 10_000)

        file.delete()
        outDir.deleteRecursively()
    }
}
