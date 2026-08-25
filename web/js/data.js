// Firestore data layer — collection names, doc-ID schemes, and query shapes mirror the Android
// app's data/repo/*.kt files exactly (same Firestore project, same documents).
import { db } from "./firebase-init.js";
import {
  collection, doc, getDoc, getDocs, setDoc, deleteDoc, onSnapshot, query,
  where, orderBy, limit as fsLimit, writeBatch, addDoc,
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";
import { FIRESTORE_BATCH_LIMIT } from "./constants.js";
import { todayStrUtc, monthStart, monthEndExclusive } from "./dateUtils.js";

function chunk(arr, size) {
  const out = [];
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size));
  return out;
}

async function runBatched(items, fn) {
  for (const part of chunk(items, FIRESTORE_BATCH_LIMIT)) {
    const batch = writeBatch(db);
    part.forEach((item) => fn(batch, item));
    await batch.commit();
  }
}

// ---------- workers/{workerId} ----------
export function liveWorkers(cb) {
  return onSnapshot(collection(db, "workers"), (snap) => {
    const rows = snap.docs.map((d) => ({ docId: d.id, ...d.data() }));
    rows.sort((a, b) => (a.sno || 0) - (b.sno || 0));
    cb(rows);
  });
}
export async function getWorker(id) {
  const d = await getDoc(doc(db, "workers", id));
  return d.exists() ? { docId: d.id, ...d.data() } : null;
}
export async function saveWorker(worker) {
  await setDoc(doc(db, "workers", worker.id), worker, { merge: true });
}
export async function deleteWorker(id) {
  await deleteDoc(doc(db, "workers", id));
}
export async function batchUpsertWorkers(workers, onProgress = () => {}) {
  let done = 0;
  for (const part of chunk(workers, FIRESTORE_BATCH_LIMIT)) {
    const batch = writeBatch(db);
    part.forEach((w) => batch.set(doc(db, "workers", w.id), w, { merge: true }));
    await batch.commit();
    done += part.length;
    onProgress(done, workers.length);
  }
}
export async function deleteAllWorkers(ids, onProgress = () => {}) {
  let done = 0;
  for (const part of chunk(ids, FIRESTORE_BATCH_LIMIT)) {
    const batch = writeBatch(db);
    part.forEach((id) => batch.delete(doc(db, "workers", id)));
    await batch.commit();
    done += part.length;
    onProgress(done, ids.length);
  }
}

// ---------- sites/{code} ----------
export function liveSites(cb) {
  return onSnapshot(collection(db, "sites"), (snap) => cb(snap.docs.map((d) => ({ docId: d.id, ...d.data() }))));
}
export async function saveSite(site) {
  await setDoc(doc(db, "sites", site.code.toUpperCase()), { ...site, code: site.code.toUpperCase() }, { merge: true });
}
export async function deleteSite(code) {
  await deleteDoc(doc(db, "sites", code));
}
export async function batchUpsertSites(sites) {
  await runBatched(sites, (batch, s) => batch.set(doc(db, "sites", s.code.toUpperCase()), s, { merge: true }));
}

// ---------- attendance/{date}_{workerId} ----------
const attCol = () => collection(db, "attendance");
const attDocId = (date, workerId) => `${date}_${workerId}`;

export function liveToday(cb) {
  const today = todayStrUtc();
  return onSnapshot(query(attCol(), where("date", "==", today)), (snap) =>
    cb(snap.docs.map((d) => ({ docId: d.id, ...d.data() })))
  );
}
export async function getForDate(date) {
  const snap = await getDocs(query(attCol(), where("date", "==", date)));
  return snap.docs.map((d) => ({ docId: d.id, ...d.data() }));
}
export async function getForMonth(monthStr) {
  const start = monthStart(monthStr);
  const endExclusive = monthEndExclusive(monthStr);
  const snap = await getDocs(query(attCol(), where("date", ">=", start), where("date", "<", endExclusive)));
  return snap.docs.map((d) => ({ docId: d.id, ...d.data() }));
}
export async function getAllAttendance() {
  const snap = await getDocs(attCol());
  return snap.docs.map((d) => ({ docId: d.id, ...d.data() }));
}
export async function getRecord(date, workerId) {
  const d = await getDoc(doc(db, "attendance", attDocId(date, workerId)));
  return d.exists() ? { docId: d.id, ...d.data() } : null;
}
export async function getMarkedBy(email, limitN = 60) {
  const snap = await getDocs(query(attCol(), where("markedBy", "==", email)));
  const rows = snap.docs.map((d) => ({ docId: d.id, ...d.data() }));
  rows.sort((a, b) => (b.lastAction || "").localeCompare(a.lastAction || ""));
  return rows.slice(0, limitN);
}
export async function writeMark(date, workerId, fields) {
  await setDoc(doc(db, "attendance", attDocId(date, workerId)), fields, { merge: true });
}
export async function bulkWriteMark(date, workerIds, commonFields) {
  await runBatched(workerIds, (batch, workerId) => {
    batch.set(doc(db, "attendance", attDocId(date, workerId)), { ...commonFields, workerId, date }, { merge: true });
  });
}
export async function batchUpsertAttendance(records) {
  await runBatched(records, (batch, a) => batch.set(doc(db, "attendance", attDocId(a.date, a.workerId)), a, { merge: true }));
}
export async function acknowledgeDeviation(docId, by) {
  await setDoc(doc(db, "attendance", docId), {
    deviationReviewed: true, deviationReviewedBy: by, deviationReviewedAt: new Date().toISOString(),
  }, { merge: true });
}

// ---------- blocked/{autoId} ----------
export async function logBlocked(entry) {
  await addDoc(collection(db, "blocked"), entry);
}
export function liveBlocked(cb) {
  return onSnapshot(collection(db, "blocked"), (snap) => cb(snap.docs.map((d) => ({ docId: d.id, ...d.data() }))));
}

// ---------- arrivalRequests/{autoId} ----------
export async function submitArrival(entry) {
  await addDoc(collection(db, "arrivalRequests"), entry);
}
export function liveArrivals(cb) {
  return onSnapshot(collection(db, "arrivalRequests"), (snap) => cb(snap.docs.map((d) => ({ docId: d.id, ...d.data() }))));
}
export async function decideArrival(id, approve, by) {
  const nowIso = new Date().toISOString();
  const fields = approve
    ? { status: "approved", approvedAt: nowIso, approvedBy: by }
    : { status: "rejected", rejectedAt: nowIso, rejectedBy: by };
  await setDoc(doc(db, "arrivalRequests", id), fields, { merge: true });
}

// ---------- leaves/{autoId} ----------
export async function addLeave(entry) {
  await addDoc(collection(db, "leaves"), entry);
}
export async function bulkAddLeaves(entries) {
  await runBatched(entries, (batch, entry) => batch.set(doc(collection(db, "leaves")), entry));
}
export function liveLeaves(cb) {
  return onSnapshot(collection(db, "leaves"), (snap) => cb(snap.docs.map((d) => ({ docId: d.id, ...d.data() }))));
}
export async function getAllLeaves() {
  const snap = await getDocs(collection(db, "leaves"));
  return snap.docs.map((d) => ({ docId: d.id, ...d.data() }));
}
export async function myLeaves(email) {
  const snap = await getDocs(query(collection(db, "leaves"), where("requestedBy", "==", email)));
  return snap.docs.map((d) => ({ docId: d.id, ...d.data() }));
}
export async function decideLeave(id, approve, by) {
  const nowIso = new Date().toISOString();
  const fields = approve
    ? { status: "approved", approvedAt: nowIso, approvedBy: by }
    : { status: "rejected", rejectedAt: nowIso, rejectedBy: by };
  await setDoc(doc(db, "leaves", id), fields, { merge: true });
}

// ---------- holidays/{date} ----------
export function liveHolidays(cb) {
  return onSnapshot(collection(db, "holidays"), (snap) => cb(snap.docs.map((d) => ({ docId: d.id, ...d.data() }))));
}
export async function getAllHolidays() {
  const snap = await getDocs(collection(db, "holidays"));
  return snap.docs.map((d) => ({ docId: d.id, ...d.data() }));
}
export async function addHoliday(date, name, addedBy) {
  await setDoc(doc(db, "holidays", date), { date, name, addedBy, addedAt: new Date().toISOString() });
}
export async function deleteHoliday(date) {
  await deleteDoc(doc(db, "holidays", date));
}
export async function bulkAddHolidays(entries) {
  await runBatched(entries, (batch, h) => batch.set(doc(db, "holidays", h.date), h, { merge: true }));
}

// ---------- staffWorkerLinks/{email} ----------
export async function getStaffLink(email) {
  const d = await getDoc(doc(db, "staffWorkerLinks", email));
  return d.exists() ? d.data() : null;
}
export async function setStaffLink(email, workerId) {
  await setDoc(doc(db, "staffWorkerLinks", email), { email, workerId, assignedAt: new Date().toISOString() });
}

// ---------- announcements/{autoId} ----------
export function liveLatestAnnouncement(cb) {
  const q = query(collection(db, "announcements"), orderBy("sentAt", "desc"), fsLimit(1));
  return onSnapshot(q, (snap) => cb(snap.empty ? null : { docId: snap.docs[0].id, ...snap.docs[0].data() }));
}
export async function postAnnouncement(message, sentBy) {
  await addDoc(collection(db, "announcements"), { message, sentBy, sentAt: new Date().toISOString() });
}

// ---------- pushTokens/{email} ----------
export async function registerPushToken(email, token) {
  await setDoc(doc(db, "pushTokens", email), { email, token, updatedAt: new Date().toISOString() });
}
