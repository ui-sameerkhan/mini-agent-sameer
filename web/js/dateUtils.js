// Mirrors app/src/main/java/com/ktc/sitepulse/domain/DateUtils.kt exactly, which itself mirrors
// the original web app's date conventions:
//  - "today" for attendance doc IDs is the UTC calendar date (new Date().toISOString().slice(0,10))
//    — NOT the device's local date.
//  - Night-shift classification uses the LOCAL device hour.
// Keep both conventions exactly as-is so data stays consistent with existing Firestore records.

export function todayStrUtc() {
  return new Date().toISOString().slice(0, 10);
}

export function nowIso() {
  return new Date().toISOString();
}

export function yesterdayOf(dateStr) {
  const d = new Date(`${dateStr}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() - 1);
  return d.toISOString().slice(0, 10);
}

export function localHour() {
  return new Date().getHours();
}

// nightStart/dayStart let a specific site override the company-wide 18:00/05:00 default.
export function shiftFor(hour = localHour(), nightStart = 18, dayStart = 5) {
  return hour >= nightStart || hour < dayStart ? "Night" : "Day";
}

export function formatTimeHm(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  if (isNaN(d)) return "";
  return d.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
}

export function formatDateTime(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  if (isNaN(d)) return "";
  return d.toLocaleString(undefined, {
    day: "numeric", month: "short", year: "numeric", hour: "numeric", minute: "2-digit",
  });
}

export function monthOf(dateStr) {
  return dateStr.slice(0, 7);
}

export function monthStart(monthStr) {
  return `${monthStr}-01`;
}

export function monthEndExclusive(monthStr) {
  const [y, m] = monthStr.split("-").map(Number);
  const next = m === 12 ? { y: y + 1, m: 1 } : { y, m: m + 1 };
  return `${next.y}-${String(next.m).padStart(2, "0")}-01`;
}

export function daysInMonth(monthStr) {
  const [y, m] = monthStr.split("-").map(Number);
  return new Date(Date.UTC(y, m, 0)).getUTCDate();
}

export function elapsedDaysFor(monthStr) {
  const currentMonth = monthOf(todayStrUtc());
  if (monthStr === currentMonth) return new Date().getUTCDate();
  return daysInMonth(monthStr);
}

// Hours between a check-in and check-out ISO instant, or null if either is missing/unparseable
// or out precedes in. Real elapsed time, so a night shift crossing midnight needs no extra logic.
export function hoursBetween(inIso, outIso) {
  if (!inIso || !outIso) return null;
  const inD = new Date(inIso), outD = new Date(outIso);
  if (isNaN(inD) || isNaN(outD)) return null;
  const seconds = (outD.getTime() - inD.getTime()) / 1000;
  return seconds < 0 ? null : seconds / 3600;
}

// Builds an ISO instant from a calendar date + a local HH:mm time — used by the admin's manual
// attendance correction dialog, where the picker only knows a clock time.
export function isoFromLocalTime(dateStr, hour, minute, plusDays = 0) {
  const [y, m, d] = dateStr.split("-").map(Number);
  const local = new Date(y, m - 1, d + plusDays, hour, minute, 0, 0);
  return local.toISOString();
}

// The inverse of isoFromLocalTime — local {hour, minute} to pre-fill an edit dialog, or null.
export function localHourMinute(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  if (isNaN(d)) return null;
  return { hour: d.getHours(), minute: d.getMinutes() };
}

export function isWithin(dateStr, fromDate, toDate) {
  return dateStr >= fromDate && dateStr <= toDate;
}
