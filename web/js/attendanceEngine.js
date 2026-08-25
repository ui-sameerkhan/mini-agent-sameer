// Port of app/src/main/java/com/ktc/sitepulse/domain/AttendanceEngine.kt, itself "a full port of
// the web app's mark(dir) function" — every business rule (duplicate prevention, night-shift
// midnight crossover, supervisor check-out ownership, mandatory geofencing, blocked-attempt
// logging) is reproduced exactly so behavior matches the Android app and existing Firestore data.
//
// One real platform difference from the Android app: browsers have no API to detect a spoofed/
// mock GPS location (Android's Location.isFromMockProvider() has no web equivalent), so that
// specific safeguard cannot be replicated here — flagged clearly in the UI instead of faking it.
import * as Geo from "./geo.js";
import * as DateUtils from "./dateUtils.js";
import { getRecord, writeMark, logBlocked } from "./data.js";

export async function getGpsFix() {
  if (!navigator.geolocation) throw new Error("This browser has no geolocation support.");
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude, accuracyM: pos.coords.accuracy }),
      (err) => reject(new Error(err.message || "Location permission denied.")),
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
    );
  });
}

function round6(v) {
  return Math.round(v * 1e6) / 1e6;
}

/**
 * @param {"IN"|"OUT"} dir
 * @param {object} worker
 * @param {object} opts { isAdmin, currentEmail, sites, todayAttendance, lockedWorkerId }
 * @returns {Promise<{kind: "success"|"blocked"|"rejected"|"failure", title, detail, siteMismatch?}>}
 */
export async function mark(dir, worker, opts) {
  const { isAdmin, currentEmail, sites, todayAttendance, lockedWorkerId } = opts;

  if (lockedWorkerId && lockedWorkerId !== worker.id) {
    return { kind: "rejected", title: "🔒 WORKER ID LOCKED",
      detail: `This account is permanently linked to Worker ID ${lockedWorkerId} and cannot check in under a different ID.` };
  }

  const today = DateUtils.todayStrUtc();
  const existingToday = todayAttendance.find((a) => a.workerId === worker.id);

  if (dir === "IN" && existingToday?.in) {
    return { kind: "rejected", title: "⚠️ ALREADY CHECKED IN",
      detail: `${worker.name} has already checked in today at ${DateUtils.formatTimeHm(existingToday.in)}.` };
  }

  let recDate = today;
  let openRecord = existingToday || null;

  if (dir === "OUT") {
    if (!openRecord?.in) {
      const yesterday = DateUtils.yesterdayOf(today);
      const yestRecord = await getRecord(yesterday, worker.id);
      if (yestRecord?.in && !yestRecord?.out) {
        openRecord = yestRecord;
        recDate = yesterday;
      } else {
        return { kind: "rejected", title: "🚫 NOT CHECKED IN",
          detail: `${worker.name} has not been checked in today (or on a still-open night shift).` };
      }
    } else if (openRecord.out) {
      const dayLabel = recDate === today ? "today" : `on ${recDate}`;
      return { kind: "rejected", title: "⚠️ ALREADY CHECKED OUT",
        detail: `${worker.name} already checked out ${dayLabel} at ${DateUtils.formatTimeHm(openRecord.out)}.` };
    }

    if (!isAdmin && openRecord.markedBy && openRecord.markedBy !== currentEmail) {
      return { kind: "rejected", title: "🚫 NOT AUTHORIZED",
        detail: `${worker.name} was checked in by a different supervisor. Only the supervisor who checked them in (or an admin) can check them out.` };
    }
  }

  if (!sites.length) {
    return { kind: "rejected", title: "⚠️ NO PROJECT SITES SET", detail: "Ask the admin to add a project site first." };
  }

  let site, distanceM = null, gpsPoint = null, markedVia;

  let fix;
  try {
    fix = await getGpsFix();
  } catch (e) {
    return { kind: "failure", title: "⚠️ ERROR",
      detail: `🚫 LOCATION REQUIRED — Please allow GPS access. (${e.message})`, retryable: true };
  }

  const near = Geo.nearestSite({ lat: fix.lat, lng: fix.lng }, sites);
  if (!near.insideGeofence) {
    await logBlocked({
      workerId: worker.id, name: worker.name, date: today, time: DateUtils.nowIso(),
      gps: { lat: fix.lat, lng: fix.lng, acc: fix.accuracyM }, nearestSite: near.site.code,
      distance: near.distanceM, action: dir,
    });
    return { kind: "blocked", title: "🚫 ATTENDANCE DENIED — OUTSIDE SITE",
      detail: `${worker.name} is not inside any project site. Nearest: ${near.site.name} (${near.site.code}) — ${Geo.formatDistance(near.distanceM)} away.` };
  }
  site = near.site;
  distanceM = near.distanceM;
  gpsPoint = { lat: round6(fix.lat), lng: round6(fix.lng) };
  markedVia = "gps";

  const nowIso = DateUtils.nowIso();
  const shift = DateUtils.shiftFor(undefined, Number(site.nightStartHour ?? 18), Number(site.dayStartHour ?? 5));

  const alignedSite = (worker.site || "").trim() || null;
  const siteMismatch = !!alignedSite && alignedSite.toUpperCase() !== site.code.toUpperCase();

  const fields = {
    workerId: worker.id, date: recDate, siteCode: site.code, siteName: site.name,
    lastAction: nowIso, markedBy: currentEmail, markedVia, alignedSite, siteMismatch,
  };
  if (dir === "IN") {
    fields.shift = shift;
    fields.in = nowIso;
    fields.inGps = gpsPoint;
    fields.inDist = distanceM;
  } else {
    fields.out = nowIso;
    fields.outGps = gpsPoint;
    fields.outDist = distanceM;
  }

  try {
    await writeMark(recDate, worker.id, fields);
  } catch (e) {
    return { kind: "failure", title: "⚠️ ERROR", detail: `⚠️ Save failed: ${e.message}`, retryable: true };
  }

  const proximityLabel = `${Geo.formatDistance(distanceM)} from center`;
  const timeLabel = DateUtils.formatTimeHm(nowIso);
  const deviationNote = siteMismatch ? `\n⚠ Roster shows ${alignedSite} — flagged for admin review.` : "";

  if (dir === "IN") {
    const shiftEmoji = shift === "Night" ? "🌙" : "☀️";
    return { kind: "success", title: "✅ CHECK IN SUCCESS",
      detail: `${worker.name} · ${site.name} · ${shiftEmoji} ${shift} · ${proximityLabel} · ${timeLabel}${deviationNote}`,
      siteMismatch };
  }
  const closedNote = recDate !== today ? `Closed out ${recDate}'s night shift.` : "";
  return { kind: "success", title: "🏁 CHECK OUT SUCCESS",
    detail: `${worker.name} · ${site.name} · ${proximityLabel} · ${timeLabel}. ${closedNote}${deviationNote}`.trim(),
    siteMismatch };
}
