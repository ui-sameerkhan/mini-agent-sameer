import { getState, subscribe } from "./store.js";
import { addLeave, getMarkedBy, myLeaves } from "./data.js";
import { esc, toast } from "./ui.js";
import * as DateUtils from "./dateUtils.js";
import { LEAVE_TYPES } from "./constants.js";

let leaveFrom = DateUtils.todayStrUtc();
let leaveTo = DateUtils.todayStrUtc();
let leaveType = "Annual";
let leaveReason = "";
let history = [];
let historyLoaded = false;
// Firestore rules only let an office-staff account read leave docs it self-submitted
// (resource.data.requestedBy == their email) — not ones an admin marked directly — so this
// view keeps its own restricted fetch instead of the admin-only global `leaves` in the store.
let myLeaveRows = [];
let myLeavesLoaded = false;

function annualLeaveUsedDays(leaves, workerId, year) {
  let days = 0;
  leaves.filter((l) => l.workerId === workerId && l.status === "approved" && l.leaveType === "Annual").forEach((l) => {
    const from = new Date(`${l.fromDate}T00:00:00Z`);
    const to = new Date(`${l.toDate || l.fromDate}T00:00:00Z`);
    for (let d = new Date(from); d <= to; d.setUTCDate(d.getUTCDate() + 1)) {
      if (d.getUTCFullYear() === year) days++;
    }
  });
  return days;
}

export function mount(root) {
  subscribe(() => render(root));
  render(root);
  loadHistory(root);
  loadMyLeaves(root);
}

async function loadHistory(root) {
  const { session } = getState();
  history = await getMarkedBy(session.email, 60);
  historyLoaded = true;
  render(root);
}

async function loadMyLeaves(root) {
  const { session } = getState();
  myLeaveRows = await myLeaves(session.email);
  myLeavesLoaded = true;
  render(root);
}

function render(root) {
  const { session, workers } = getState();
  const myWorkerId = getState().myLinkedWorkerId;
  const myWorker = workers.find((w) => w.id === myWorkerId);
  const usedDays = myWorker ? annualLeaveUsedDays(myLeaveRows, myWorker.id, new Date().getFullYear()) : 0;
  const allowance = myWorker?.annualLeaveDays ?? 30;

  root.innerHTML = `
    ${myWorker ? `<div class="card">
      <h2>Annual Leave Balance</h2>
      <p>Annual Leave: <b>${usedDays}</b> of <b>${allowance}</b> days used this year</p>
    </div>` : ""}

    <div class="card">
      <h2>Apply for Leave</h2>
      <div class="row">
        <div class="field"><label>From *</label><input id="osFrom" type="date" value="${leaveFrom}" /></div>
        <div class="field"><label>To *</label><input id="osTo" type="date" value="${leaveTo}" /></div>
      </div>
      <div class="field"><label>Leave Type</label><select id="osType">${LEAVE_TYPES.map((t) => `<option ${t === leaveType ? "selected" : ""}>${t}</option>`).join("")}</select></div>
      <div class="field"><label>Reason</label><input id="osReason" value="${esc(leaveReason)}" /></div>
      <button id="osSubmit" class="btn btn-primary btn-block">Submit Leave Application</button>
    </div>

    <div class="card">
      <h2>My Leave Requests</h2>
      ${!myLeavesLoaded ? `<p class="muted">Loading…</p>` : myLeaveRows.length === 0 ? `<p class="muted">No requests submitted yet.</p>` : `
        <div class="table-wrap"><table><thead><tr><th>From</th><th>To</th><th>Type</th><th>Status</th></tr></thead><tbody>
          ${myLeaveRows.slice().sort((a, b) => (b.ts || "").localeCompare(a.ts || "")).map((l) => `
            <tr><td>${esc(l.fromDate)}</td><td>${esc(l.toDate)}</td><td>${esc(l.leaveType || "Annual")}</td>
            <td><span class="badge ${l.status === "approved" ? "badge-green" : l.status === "rejected" ? "badge-red" : "badge-amber"}">${esc(l.status)}</span></td></tr>
          `).join("")}
        </tbody></table></div>`}
    </div>

    <div class="card">
      <h2>My Attendance History</h2>
      ${!historyLoaded ? `<p class="muted">Loading…</p>` : history.length === 0 ? `<p class="muted">No history yet.</p>` : `
        <div class="table-wrap"><table><thead><tr><th>Date</th><th>Site</th><th>In</th><th>Out</th></tr></thead><tbody>
          ${history.map((a) => `<tr><td>${esc(a.date)}</td><td>${esc(a.siteCode)}</td><td>${esc(DateUtils.formatTimeHm(a.in))}</td><td>${esc(DateUtils.formatTimeHm(a.out))}</td></tr>`).join("")}
        </tbody></table></div>`}
    </div>
  `;

  root.querySelector("#osFrom").addEventListener("input", (e) => (leaveFrom = e.target.value));
  root.querySelector("#osTo").addEventListener("input", (e) => (leaveTo = e.target.value));
  root.querySelector("#osType").addEventListener("change", (e) => (leaveType = e.target.value));
  root.querySelector("#osReason").addEventListener("input", (e) => (leaveReason = e.target.value));
  root.querySelector("#osSubmit").addEventListener("click", async () => {
    if (!myWorker) return toast("Check in at least once before applying for leave, so your account is linked to a Worker ID.", "error");
    await addLeave({
      workerId: myWorker.id, site: myWorker.site || "", fromDate: leaveFrom, toDate: leaveTo,
      reason: leaveReason || null, leaveType, markedBy: session.email, ts: DateUtils.nowIso(),
      status: "pending", requestedBy: session.email,
    });
    toast("Leave application submitted", "success");
    leaveReason = "";
    loadMyLeaves(root);
  });
}
