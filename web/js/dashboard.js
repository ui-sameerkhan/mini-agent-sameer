import { getState, subscribe } from "./store.js";
import { getRecord, acknowledgeDeviation, postAnnouncement } from "./data.js";
import { esc, toast } from "./ui.js";
import { formatTimeHm, formatDateTime, todayStrUtc } from "./dateUtils.js";

let locatorId = "";
let locatorDate = todayStrUtc();
let locatorResult = null;
let announceText = "";

export function mount(root) {
  subscribe(() => render(root));
  render(root);
}

function render(root) {
  const { workers, sites, todayAttendance, blocked } = getState();
  const present = todayAttendance.filter((a) => a.in && !a.out).length;
  const checkedOut = todayAttendance.filter((a) => a.out).length;
  const blockedToday = blocked.filter((b) => b.date === todayStrUtc()).length;
  const bySite = {};
  todayAttendance.forEach((a) => {
    (bySite[a.siteCode] = bySite[a.siteCode] || []).push(a);
  });
  const deviations = todayAttendance.filter((a) => a.siteMismatch && !a.deviationReviewed);

  root.innerHTML = `
    <div class="card">
      <h2>Admin Dashboard</h2>
      <div class="kpi-grid">
        <div class="kpi-tile"><div class="num">${present}</div><div class="lbl">Present On-Site</div></div>
        <div class="kpi-tile"><div class="num">${checkedOut}</div><div class="lbl">Checked Out</div></div>
        <div class="kpi-tile"><div class="num">${blockedToday}</div><div class="lbl">Blocked Today</div></div>
        <div class="kpi-tile"><div class="num">${sites.length}</div><div class="lbl">Active Sites</div></div>
      </div>
    </div>

    <div class="card">
      <h2>Post Announcement</h2>
      <p class="muted">Broadcast a message to every signed-in user (shown as a banner + push notification).</p>
      <div class="row"><input id="announceText" placeholder="Message…" value="${esc(announceText)}" /><button id="announceSend" class="btn btn-gold">Send</button></div>
    </div>

    <div class="card">
      <h2>Worker Locator</h2>
      <div class="row">
        <input id="locId" placeholder="Worker ID" value="${esc(locatorId)}" />
        <input id="locDate" type="date" value="${esc(locatorDate)}" />
        <button id="locGo" class="btn btn-primary">Look Up</button>
      </div>
      ${locatorResult !== null ? renderLocatorResult(locatorResult) : ""}
    </div>

    <div class="card">
      <h2>Today's Attendance by Project</h2>
      ${Object.keys(bySite).length === 0 ? `<p class="muted">No check-ins yet today.</p>` : Object.entries(bySite).map(([code, rows]) => `
        <h3>${esc(code)} (${rows.length})</h3>
        <div class="table-wrap"><table><thead><tr><th>Worker</th><th>In</th><th>Out</th><th>Shift</th></tr></thead><tbody>
          ${rows.map((a) => {
            const w = workers.find((x) => x.id === a.workerId);
            return `<tr><td>${esc(w?.name || a.workerId)}</td><td>${esc(formatTimeHm(a.in))}</td><td>${esc(formatTimeHm(a.out))}</td><td>${esc(a.shift || "")}</td></tr>`;
          }).join("")}
        </tbody></table></div>
      `).join("")}
    </div>

    <div class="card">
      <h2>Site Deviations <span class="badge badge-amber">${deviations.length}</span></h2>
      ${deviations.length === 0 ? `<p class="muted">No unreviewed deviations.</p>` : `
        <div class="table-wrap"><table><thead><tr><th>Worker</th><th>Roster Site</th><th>Actual Site</th><th></th></tr></thead><tbody>
          ${deviations.map((a) => {
            const w = workers.find((x) => x.id === a.workerId);
            return `<tr><td>${esc(w?.name || a.workerId)}</td><td>${esc(a.alignedSite || "")}</td><td>${esc(a.siteCode)}</td>
              <td><button class="btn btn-outline" data-ack="${esc(a.docId)}">Acknowledge</button></td></tr>`;
          }).join("")}
        </tbody></table></div>`}
    </div>

    <div class="card">
      <h2>Blocked Attempts Log</h2>
      ${blocked.length === 0 ? `<p class="muted">No blocked attempts recorded.</p>` : `
        <div class="table-wrap"><table><thead><tr><th>Worker</th><th>When</th><th>Nearest Site</th><th>Distance</th><th>Action</th></tr></thead><tbody>
          ${blocked.slice().sort((a, b) => (b.time || "").localeCompare(a.time || "")).slice(0, 50).map((b) => `
            <tr><td>${esc(b.name)}</td><td>${esc(formatDateTime(b.time))}</td>
            <td>${esc(b.nearestSite === "MOCK_LOCATION" ? "🚫 Fake GPS" : b.nearestSite)}</td>
            <td>${b.distance >= 0 ? b.distance + "m" : "—"}</td><td>${esc(b.action)}</td></tr>
          `).join("")}
        </tbody></table></div>`}
    </div>
  `;

  root.querySelector("#announceText").addEventListener("input", (e) => (announceText = e.target.value));
  root.querySelector("#announceSend").addEventListener("click", async () => {
    if (!announceText.trim()) return;
    const { session } = getState();
    await postAnnouncement(announceText.trim(), session.email);
    announceText = "";
    toast("Announcement posted", "success");
    render(root);
  });
  root.querySelector("#locId")?.addEventListener("input", (e) => (locatorId = e.target.value));
  root.querySelector("#locDate")?.addEventListener("input", (e) => (locatorDate = e.target.value));
  root.querySelector("#locGo")?.addEventListener("click", async () => {
    if (!locatorId.trim()) return;
    const rec = await getRecord(locatorDate, locatorId.trim());
    locatorResult = rec || false;
    render(root);
  });
  root.querySelectorAll("[data-ack]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const { session } = getState();
      await acknowledgeDeviation(btn.dataset.ack, session.email);
      toast("Deviation acknowledged", "success");
    });
  });
}

function renderLocatorResult(rec) {
  if (rec === false) return `<p class="muted" style="margin-top:10px">No record for that worker on that date.</p>`;
  return `<div class="card" style="margin-top:10px;background:var(--light-bg)">
    <div><b>Site:</b> ${esc(rec.siteName)} (${esc(rec.siteCode)})</div>
    <div><b>In:</b> ${esc(formatDateTime(rec.in))} ${rec.markedVia === "wifi" ? "(WiFi)" : ""}</div>
    <div><b>Out:</b> ${esc(formatDateTime(rec.out))}</div>
    <div><b>Marked By:</b> ${esc(rec.markedBy)}</div>
    ${rec.corrected ? `<div class="badge badge-amber" style="margin-top:6px">⚠ Manually corrected by ${esc(rec.correctedBy)}</div>` : ""}
  </div>`;
}
