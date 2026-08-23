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
import com.ktc.sitepulse.data.repo.MockLocationException
import com.ktc.sitepulse.data.repo.WifiProvider
import com.ktc.sitepulse.data.repo.toLatLng
import kotlin.math.roundToLong

enum class MarkDirection { IN, OUT }

sealed class MarkResult {
    data class Success(val title: String, val detail: String, val offline: Boolean, val siteMismatch: Boolean = false) : MarkResult()
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
    private val wifiProvider: WifiProvider,
) {
    suspend fun mark(
        dir: MarkDirection,
        worker: Worker,
        isAdmin: Boolean,
        currentEmail: String,
        sites: List<Site>,
        todayAttendance: List<Attendance>,
        isOnline: Boolean,
        lockedWorkerId: String? = null,
    ): MarkResult {
        if (lockedWorkerId != null && lockedWorkerId != worker.id) {
            return MarkResult.Rejected(
                "🔒 WORKER ID LOCKED",
                "This account is permanently linked to Worker ID $lockedWorkerId and cannot check in under a different ID."
            )
        }

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

        if (sites.isEmpty()) {
            return MarkResult.Rejected(
                "⚠️ NO PROJECT SITES SET",
                "Ask the admin to add a project site first."
            )
        }

        // Office WiFi match takes priority over GPS: if the phone is connected to a site's
        // configured SSID, that's sufficient proof of presence — no GPS fix needed at all,
        // which matters indoors where GPS is often slow or unavailable.
        val currentSsid = wifiProvider.currentSsid()
        val wifiSite = currentSsid?.let { ssid -> sites.find { it.wifiSsid?.equals(ssid, ignoreCase = true) == true } }

        val site: Site
        val distanceM: Long?
        val gpsPoint: GpsPoint?
        val markedVia: String

        if (wifiSite != null) {
            site = wifiSite
            distanceM = null
            gpsPoint = null
            markedVia = "wifi"
        } else {
            val fix = try {
                locationProvider.getCurrentFix()
            } catch (e: MockLocationException) {
                blockedRepo.log(
                    Blocked(
                        workerId = worker.id, name = worker.name, date = today, time = DateUtils.nowIso(),
                        gps = null, nearestSite = "MOCK_LOCATION", distance = -1, action = dir.name,
                    )
                )
                return MarkResult.Blocked(
                    "🚫 GPS SPOOFING DETECTED",
                    "${worker.name}'s device is reporting a fake/mock location. Disable any fake-GPS app or developer mock-location setting and try again."
                )
            } catch (e: GpsException) {
                return MarkResult.Failure("🚫 LOCATION REQUIRED — Please allow GPS access, or connect to an office WiFi network. (${e.message})", retryable = true)
            } catch (e: Exception) {
                return MarkResult.Failure("🚫 LOCATION REQUIRED — Please allow GPS access, or connect to an office WiFi network. (${e.message})", retryable = true)
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
                    "${worker.name} is not inside any project site or office WiFi. Nearest: ${near.site.name} (${near.site.code}) — ${Geo.formatDistance(near.distanceM)} away."
                )
            }
            site = near.site
            distanceM = near.distanceM
            gpsPoint = GpsPoint(round6(fix.lat), round6(fix.lng))
            markedVia = "gps"
        }

        val nowIso = DateUtils.nowIso()
        val shift = DateUtils.shiftFor()

        // The ERP/biometric roster's "aligned site" is informational, not a hard gate — a
        // worker can genuinely be sent to cover a different site for a day. Rather than
        // blocking that (or silently losing track of it), record it as-is and flag the
        // mismatch for admin review, per operations' request.
        val alignedSite = worker.site?.trim()?.takeIf { it.isNotBlank() }
        val siteMismatch = alignedSite != null && !alignedSite.equals(site.code, ignoreCase = true)

        val fields = mutableMapOf<String, Any?>(
            "workerId" to worker.id,
            "date" to recDate,
            "siteCode" to site.code,
            "siteName" to site.name,
            "lastAction" to nowIso,
            "markedBy" to currentEmail,
            "markedVia" to markedVia,
            "alignedSite" to alignedSite,
            "siteMismatch" to siteMismatch,
        )
        if (dir == MarkDirection.IN) {
            fields["shift"] = shift
            fields["in"] = nowIso
            fields["inGps"] = gpsPoint?.let { mapOf("lat" to it.lat, "lng" to it.lng) }
            fields["inDist"] = distanceM
        } else {
            fields["out"] = nowIso
            fields["outGps"] = gpsPoint?.let { mapOf("lat" to it.lat, "lng" to it.lng) }
            fields["outDist"] = distanceM
        }

        try {
            attendanceRepo.writeMark(recDate, worker.id, fields)
        } catch (e: Exception) {
            return MarkResult.Failure("⚠️ Save failed: ${e.message}", retryable = true)
        }

        val proximityLabel = if (markedVia == "wifi") "via office WiFi" else "${Geo.formatDistance(distanceM!!)} from center"
        val timeLabel = DateUtils.formatTimeHm(nowIso)
        val deviationNote = if (siteMismatch) "\n⚠ Roster shows $alignedSite — flagged for admin review." else ""
        return if (!isOnline) {
            MarkResult.Success(
                "📴 SAVED OFFLINE",
                "${worker.name}'s ${dir.name} was stored on this phone and will sync automatically the moment signal returns. Do not clear app data or uninstall until it syncs.",
                offline = true,
                siteMismatch = false
            )
        } else if (dir == MarkDirection.IN) {
            val shiftEmoji = if (shift == "Night") "🌙" else "☀️"
            MarkResult.Success(
                "✅ CHECK IN SUCCESS",
                "${worker.name} · ${site.name} · $shiftEmoji $shift · $proximityLabel · $timeLabel$deviationNote",
                offline = false,
                siteMismatch = siteMismatch
            )
        } else {
            val closedNote = if (recDate != today) "Closed out $recDate's night shift." else ""
            MarkResult.Success(
                "🏁 CHECK OUT SUCCESS",
                "${worker.name} · ${site.name} · $proximityLabel · $timeLabel. $closedNote$deviationNote".trim(),
                offline = false,
                siteMismatch = siteMismatch
            )
        }
    }

    private fun round6(v: Double): Double = (v * 1_000_000).roundToLong() / 1_000_000.0
}
