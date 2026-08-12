package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Blocked
import com.ktc.sitepulse.data.model.BlockedGps
import com.ktc.sitepulse.data.model.GpsPoint
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.data.repo.AttendanceRepository
import com.ktc.sitepulse.data.repo.BlockedRepository
import com.ktc.sitepulse.data.repo.GpsException
import com.ktc.sitepulse.data.repo.LocationProvider
import com.ktc.sitepulse.data.repo.toLatLng
import kotlin.math.roundToLong

enum class MarkDirection { IN, OUT }

sealed class MarkResult {
    data class Success(val title: String, val detail: String, val offline: Boolean) : MarkResult()
    data class Blocked(val title: String, val detail: String) : MarkResult()
    data class Rejected(val title: String, val detail: String) : MarkResult()
    data class Failure(val message: String, val retryable: Boolean) : MarkResult()
}

/**
 * Full port of the web app's mark(dir) function — see spec section 5. Every
 * business rule (duplicate prevention, night-shift midnight crossover,
 * supervisor check-out ownership, mandatory geofencing, blocked-attempt
 * logging) is reproduced exactly so behavior matches the existing PWA and its
 * Firestore data.
 */
class AttendanceEngine(
    private val attendanceRepo: AttendanceRepository,
    private val blockedRepo: BlockedRepository,
    private val locationProvider: LocationProvider,
) {
    suspend fun mark(
        dir: MarkDirection,
        worker: Worker,
        isAdmin: Boolean,
        currentEmail: String,
        sites: List<Site>,
        todayAttendance: List<Attendance>,
        isOnline: Boolean,
    ): MarkResult {
        val today = DateUtils.todayStrUtc()
        val existingToday = todayAttendance.find { it.workerId == worker.id }

        if (dir == MarkDirection.IN && existingToday?.hasIn == true) {
            return MarkResult.Rejected(
                "⚠️ ALREADY CHECKED IN",
                "${worker.name} has already checked in today at ${DateUtils.formatTimeHm(existingToday.checkIn)}."
            )
        }

        var recDate = today
        var openRecord: Attendance? = existingToday

        if (dir == MarkDirection.OUT) {
            if (openRecord?.hasIn != true) {
                // Night-shift midnight crossover: look for an unclosed shift from yesterday.
                val yesterday = DateUtils.yesterdayOf(today)
                val yestRecord = attendanceRepo.getRecord(yesterday, worker.id)
                if (yestRecord != null && yestRecord.hasIn && !yestRecord.hasOut) {
                    openRecord = yestRecord
                    recDate = yesterday
                } else {
                    return MarkResult.Rejected(
                        "🚫 NOT CHECKED IN",
                        "${worker.name} has not been checked in today (or on a still-open night shift)."
                    )
                }
            } else if (openRecord.hasOut) {
                val dayLabel = if (recDate == today) "today" else "on $recDate"
                return MarkResult.Rejected(
                    "⚠️ ALREADY CHECKED OUT",
                    "${worker.name} already checked out $dayLabel at ${DateUtils.formatTimeHm(openRecord.out)}."
                )
            }

            if (!isAdmin && !openRecord!!.markedBy.isNullOrBlank() && openRecord.markedBy != currentEmail) {
                return MarkResult.Rejected(
                    "🚫 NOT AUTHORIZED",
                    "${worker.name} was checked in by a different supervisor. Only the supervisor who checked them in (or an admin) can check them out."
                )
            }
        }

        val fix = try {
            locationProvider.getCurrentFix()
        } catch (e: GpsException) {
            return MarkResult.Failure("🚫 LOCATION REQUIRED — Please allow GPS access. (${e.message})", retryable = true)
        } catch (e: Exception) {
            return MarkResult.Failure("🚫 LOCATION REQUIRED — Please allow GPS access. (${e.message})", retryable = true)
        }

        if (sites.isEmpty()) {
            return MarkResult.Rejected(
                "⚠️ NO PROJECT SITES SET",
                "Ask the admin to add a project site first."
            )
        }

        val near = Geo.nearestSite(fix.toLatLng(), sites)!!
        if (!near.insideGeofence) {
            blockedRepo.log(
                Blocked(
                    workerId = worker.id,
                    name = worker.name,
                    date = today,
                    time = DateUtils.nowIso(),
                    gps = BlockedGps(fix.lat, fix.lng, fix.accuracyM.toDouble()),
                    nearestSite = near.site.code,
                    distance = near.distanceM,
                    action = dir.name,
                )
            )
            return MarkResult.Blocked(
                "🚫 ATTENDANCE DENIED — OUTSIDE SITE",
                "${worker.name} is not inside any project site. Nearest: ${near.site.name} (${near.site.code}) — ${Geo.formatDistance(near.distanceM)} away."
            )
        }

        val nowIso = DateUtils.nowIso()
        val shift = DateUtils.shiftFor()
        val gpsRounded = GpsPoint(round6(fix.lat), round6(fix.lng))

        val fields = mutableMapOf<String, Any?>(
            "workerId" to worker.id,
            "date" to recDate,
            "siteCode" to near.site.code,
            "siteName" to near.site.name,
            "lastAction" to nowIso,
            "markedBy" to currentEmail,
        )
        if (dir == MarkDirection.IN) {
            fields["shift"] = shift
            fields["in"] = nowIso
            fields["inGps"] = mapOf("lat" to gpsRounded.lat, "lng" to gpsRounded.lng)
            fields["inDist"] = near.distanceM
        } else {
            fields["out"] = nowIso
            fields["outGps"] = mapOf("lat" to gpsRounded.lat, "lng" to gpsRounded.lng)
            fields["outDist"] = near.distanceM
        }

        try {
            attendanceRepo.writeMark(recDate, worker.id, fields)
        } catch (e: Exception) {
            return MarkResult.Failure("⚠️ Save failed: ${e.message}", retryable = true)
        }

        val distanceLabel = Geo.formatDistance(near.distanceM)
        val timeLabel = DateUtils.formatTimeHm(nowIso)
        return if (!isOnline) {
            MarkResult.Success(
                "📴 SAVED OFFLINE",
                "${worker.name}'s ${dir.name} was stored on this phone and will sync automatically the moment signal returns. Do not clear app data or uninstall until it syncs.",
                offline = true
            )
        } else if (dir == MarkDirection.IN) {
            val shiftEmoji = if (shift == "Night") "🌙" else "☀️"
            MarkResult.Success(
                "✅ CHECK IN SUCCESS",
                "${worker.name} · ${near.site.name} · $shiftEmoji $shift · ${distanceLabel} from center · $timeLabel",
                offline = false
            )
        } else {
            val closedNote = if (recDate != today) "Closed out $recDate's night shift." else ""
            MarkResult.Success(
                "🏁 CHECK OUT SUCCESS",
                "${worker.name} · ${near.site.name} · ${distanceLabel} from center · $timeLabel. $closedNote".trim(),
                offline = false
            )
        }
    }

    private fun round6(v: Double): Double = (v * 1_000_000).roundToLong() / 1_000_000.0
}
