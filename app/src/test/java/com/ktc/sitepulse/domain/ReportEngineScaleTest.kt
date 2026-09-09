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
 * Establishes how large an export the report can actually produce, by generating the volume for
 * real rather than reasoning about it. The unit-test heap is capped at 256m in build.gradle.kts
 * to match what Android grants an app; a test passing on a laptop's multi-gigabyte default would
 * prove nothing about a phone.
 *
 * The ceiling is POI's in-memory workbook, which holds every cell as an object. Its streaming
 * workbook would lift that, but cannot be used here at all: SXSSFSheet builds an
 * AutoSizeColumnTracker in its constructor, which reaches java.awt.font.FontRenderContext —
 * absent from the Android runtime — so every createSheet() dies with NoClassDefFoundError
 * on-device. Raising the ceiling therefore needs an xlsx writer that does not go through POI.
 *
 * Run with:  ./gradlew :app:testDebugUnitTest --tests '*ReportEngineScaleTest*'
 */
class ReportEngineScaleTest {

    private val fullCompany = 4_000
    private val workingDays = 26

    private fun sites() = listOf(
        Site(code = "C-25-889", name = "JVC", lat = 25.05, lng = 55.20, radius = 500),
        Site(code = "C-23-725", name = "Deira Island", lat = 25.28, lng = 55.33, radius = 500),
        Site(code = "C-26-923", name = "EMAAR South", lat = 24.85, lng = 55.15, radius = 500),
    )

    private fun workers(count: Int): List<Worker> {
        val trades = listOf("Carpenter", "Assistant Carpenter", "Mason", "Helper", "Steel Fixer", "Electrician", "Watchman")
        val suppliers = listOf(null, "Al Nahda Manpower", "Gulf Labour Supply")
        val codes = sites().map { it.code }
        return (1..count).map { i ->
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

    /** One attendance row per worker per day, for [days] days — the shape a real period produces. */
    private fun attendanceFor(month: String, workers: List<Worker>, days: Int): List<Attendance> {
        val siteNames = sites().associate { it.code to it.name }
        val rows = ArrayList<Attendance>(workers.size * days)
        for (day in 1..days) {
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

    private fun generateAndAssert(
        workers: List<Worker>,
        attendance: List<Attendance>,
        dateOrMonth: String,
        range: String,
        label: String,
    ) {
        val outDir = Files.createTempDirectory("sitepulse-scale").toFile()
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
                dateOrMonth = dateOrMonth, range = range, siteScope = "ALL", generatedBy = "scale-test",
            ),
        )

        runtime.gc()
        val after = runtime.totalMemory() - runtime.freeMemory()
        println(
            "[scale] $label: ${workers.size} workers, ${attendance.size} rows -> " +
                "${file.length() / 1024} KB in ${System.currentTimeMillis() - started}ms; " +
                "heap ${before / 1024 / 1024}MB -> ${after / 1024 / 1024}MB"
        )
        assertTrue("report should exist and be non-trivial", file.exists() && file.length() > 10_000)
        file.delete()
        outDir.deleteRecursively()
    }

    /** The daily report, for the entire company — the one run every day. */
    @Test
    fun `full company daily report completes`() {
        val workers = workers(fullCompany)
        generateAndAssert(workers, attendanceFor("2026-08", workers, days = 1), "2026-08-01", "day", "full company day")
    }

    /**
     * A single project's month. Month mode does considerably more than the daily report — a
     * per-worker hours summary and a per-worker-per-day absent matrix — so roster size drives
     * memory as much as row count, and this is the shape that decides whether month-end
     * reporting works for a project.
     */
    @Test
    fun `single project full month completes`() {
        val workers = workers(400)
        generateAndAssert(workers, attendanceFor("2026-08", workers, days = workingDays), "2026-08", "month", "one project month")
    }
}
