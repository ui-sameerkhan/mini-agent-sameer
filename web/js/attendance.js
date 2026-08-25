import { getState, subscribe } from "./store.js";
import { generateReport } from "./attendanceReports.js";
import { writeMark, bulkWriteMark } from "./data.js";
import { esc, toast, openModal, downloadBlob } from "./ui.js";
import * as DateUtils from "./dateUtils.js";

let dlRange = "day";
let dlDate = DateUtils.todayStrUtc();
let dlSite = "ALL";
let downloading = false;
let downloadStatus = "";

export function mount(root) {
  subscribe(() => render(root));
  render(root);
}

function render(root) {
  const { sites, workers, todayAttendance, session } = getState();
  root.innerHTML = `
    <div class="card">
      <h2>Download Attendance Report</h2>
      <div class="row">
        <select id="dlRange"><option value="day" ${dlRange === "day" ? "selected" : ""}>Selected Date</option><option value="month" ${dlRange === "month" ? "selected" : ""}>Full Month</option></select>
        <select id="dlSite"><option value="ALL">All Projects</option>${sites.map((s) => `<option value="${esc(s.code)}" ${dlSite === s.code ? "selected" : ""}>${esc(s.code)}</option>`).join("")}</select>
      </div>
      <input id="dlDate" type="${dlRange === "day" ? "date" : "month"}" value="${dlRange === "day" ? dlDate : DateUtils.monthOf(dlDate)}" style="margin-top:8px" />
      <button id="dlGo" class="btn btn-primary btn-block" style="margin-top:10px" ${downloading ? "disabled" : ""}>${downloading ? "Generating…" : "Download Report"}</button>
      ${downloadStatus ? `<p class="muted" style="margin-top:8px">${esc(downloadStatus)}</p>` : ""}
    </div>

    <div class="card">
      <h2>Manual Attendance Correction (Admin)</h2>
      <p class="muted">Correct a missed or backdated record. Every correction is tagged with who made it and when.</p>
      <button id="addRecordBtn" class="btn btn-outline btn-block">+ Add / Edit Manual Attendance Record</button>
      <button id="bulkMarkBtn" class="btn btn-outline btn-block" style="margin-top:8px">📋 Bulk Mark Attendance (Multiple Employees)</button>
    </div>

    <div class="card">
      <h2>Today's Records</h2>
      <div class="table-wrap"><table><thead><tr><th>Worker</th><th>Site</th><th>In</th><th>Out</th><th></th></tr></thead><tbody>
        ${todayAttendance.map((a) => {
          const w = workers.find((x) => x.id === a.workerId);
          return `<tr><td>${esc(w?.name || a.workerId)}${a.corrected ? ' <span class="badge badge-amber">✏️ corrected</span>' : ""}</td>
            <td>${esc(a.siteCode)}</td><td>${esc(DateUtils.formatTimeHm(a.in))}</td><td>${esc(DateUtils.formatTimeHm(a.out))}</td>
            <td><button class="btn btn-outline" data-editrec="${esc(a.workerId)}">Edit</button></td></tr>`;
        }).join("")}
      </tbody></table></div>
    </div>
  `;

  root.querySelector("#dlRange").addEventListener("change", (e) => { dlRange = e.target.value; render(root); });
  root.querySelector("#dlSite").addEventListener("change", (e) => (dlSite = e.target.value));
  root.querySelector("#dlDate").addEventListener("input", (e) => {
    dlDate = dlRange === "day" ? e.target.value : `${e.target.value}-01`;
  });
  root.querySelector("#dlGo").addEventListener("click", async () => {
    downloading = true; downloadStatus = "Generating…"; render(root);
    try {
      const { workers, sites, leaves, holidays } = getState();
      const dateOrMonth = dlRange === "day" ? dlDate : DateUtils.monthOf(dlDate);
      const blob = await generateReport({ dateOrMonth, range: dlRange, siteScope: dlSite, generatedBy: session.email }, workers, sites, leaves, holidays);
      const scopeLabel = dlSite === "ALL" ? "AllProjects" : dlSite;
      downloadBlob(blob, `SitePulse_${scopeLabel}_${dateOrMonth}.xlsx`);
      downloadStatus = "✅ Report ready.";
    } catch (e) {
      downloadStatus = `❌ ${e.message}`;
    } finally {
      downloading = false; render(root);
    }
  });

  root.querySelector("#addRecordBtn").addEventListener("click", () => openCorrectionForm(null));
  root.querySelector("#bulkMarkBtn").addEventListener("click", () => openBulkCorrectionForm());
  root.querySelectorAll("[data-editrec]").forEach((b) => b.addEventListener("click", () => {
    const rec = getState().todayAttendance.find((a) => a.workerId === b.dataset.editrec);
    openCorrectionForm(rec);
  }));
}

function siteOptions(sites, selected) {
  return sites.map((s) => `<option value="${esc(s.code)}" ${s.code === selected ? "selected" : ""}>${esc(s.code)} — ${esc(s.name)}</option>`).join("");
}

function openCorrectionForm(existing) {
  const { workers, sites, session } = getState();
  const date = existing?.date || DateUtils.todayStrUtc();
  const inHm = DateUtils.localHourMinute(existing?.in);
  const outHm = DateUtils.localHourMinute(existing?.out);
  openModal(`
    <h3>${existing ? "Edit" : "Add"} Attendance Record</h3>
    <div class="field"><label>Worker ID *</label><input id="cWorkerId" value="${esc(existing?.workerId || "")}" ${existing ? "readonly" : ""} /></div>
    <div class="field"><label>Date *</label><input id="cDate" type="date" value="${date}" /></div>
    <div class="field"><label>Site *</label><select id="cSite">${siteOptions(sites, existing?.siteCode)}</select></div>
    <div class="field"><label>Shift</label><select id="cShift"><option value="Day" ${existing?.shift === "Day" ? "selected" : ""}>Day</option><option value="Night" ${existing?.shift === "Night" ? "selected" : ""}>Night</option></select></div>
    <div class="row">
      <div class="field"><label>Check In</label><input id="cInTime" type="time" value="${inHm ? `${String(inHm.hour).padStart(2, "0")}:${String(inHm.minute).padStart(2, "0")}` : ""}" /></div>
      <div class="field"><label>Check Out</label><input id="cOutTime" type="time" value="${outHm ? `${String(outHm.hour).padStart(2, "0")}:${String(outHm.minute).padStart(2, "0")}` : ""}" /></div>
    </div>
    <label style="display:flex;align-items:center;gap:8px;font-size:13px"><input type="checkbox" id="cNextDay" /> Check-out is on the next calendar day (night shift)</label>
    <div class="modal-actions">
      <button class="btn btn-outline" data-act="cancel">Cancel</button>
      <button class="btn btn-primary" data-act="save">Save</button>
    </div>
  `, (modal, close) => {
    modal.querySelector('[data-act="cancel"]').onclick = close;
    modal.querySelector('[data-act="save"]').onclick = async () => {
      const workerId = modal.querySelector("#cWorkerId").value.trim();
      const d = modal.querySelector("#cDate").value;
      const siteCode = modal.querySelector("#cSite").value;
      const site = sites.find((s) => s.code === siteCode);
      const w = workers.find((x) => x.id === workerId);
      if (!workerId || !d || !site) return toast("Please fill in all required fields", "error");
      const inTime = modal.querySelector("#cInTime").value;
      const outTime = modal.querySelector("#cOutTime").value;
      const nextDay = modal.querySelector("#cNextDay").checked;
      const fields = {
        workerId, date: d, siteCode: site.code, siteName: site.name,
        shift: modal.querySelector("#cShift").value,
        markedBy: session.email, markedVia: "gps", lastAction: new Date().toISOString(),
        alignedSite: w?.site || null, siteMismatch: !!(w?.site && w.site !== site.code),
        corrected: true, correctedBy: session.email, correctedAt: new Date().toISOString(),
      };
      if (inTime) { const [h, m] = inTime.split(":").map(Number); fields.in = DateUtils.isoFromLocalTime(d, h, m); }
      if (outTime) { const [h, m] = outTime.split(":").map(Number); fields.out = DateUtils.isoFromLocalTime(d, h, m, nextDay ? 1 : 0); }
      await writeMark(d, workerId, fields);
      toast("Attendance record saved", "success");
      close();
    };
  });
}

function openBulkCorrectionForm() {
  const { workers, sites, session } = getState();
  let filterQ = "";
  const selected = new Set();
  openModal(`
    <h3>Bulk Mark Attendance</h3>
    <div class="field"><label>Date *</label><input id="bDate" type="date" value="${DateUtils.todayStrUtc()}" /></div>
    <div class="field"><label>Site *</label><select id="bSite">${siteOptions(sites)}</select></div>
    <div class="field"><label>Shift</label><select id="bShift"><option value="Day">Day</option><option value="Night">Night</option></select></div>
    <div class="row">
      <div class="field"><label>Check In</label><input id="bInTime" type="time" value="08:00" /></div>
      <div class="field"><label>Check Out</label><input id="bOutTime" type="time" value="17:00" /></div>
    </div>
    <div class="field"><label>Search employees</label><input id="bSearch" placeholder="Filter by name/ID…" /></div>
    <div class="checklist" id="bChecklist"></div>
    <p class="muted" id="bCount">0 selected</p>
    <div class="modal-actions">
      <button class="btn btn-outline" data-act="cancel">Cancel</button>
      <button class="btn btn-primary" data-act="save">Mark Selected</button>
    </div>
  `, (modal, close) => {
    const listEl = modal.querySelector("#bChecklist");
    const countEl = modal.querySelector("#bCount");
    function renderList() {
      const q = filterQ.trim().toLowerCase();
      const rows = workers.filter((w) => !q || w.name.toLowerCase().includes(q) || w.id.includes(filterQ.trim())).slice(0, 200);
      listEl.innerHTML = rows.map((w) => `<label><input type="checkbox" data-wid="${esc(w.id)}" ${selected.has(w.id) ? "checked" : ""} /> ${esc(w.name)} — ${esc(w.id)}</label>`).join("");
      listEl.querySelectorAll("input[type=checkbox]").forEach((cb) => {
        cb.addEventListener("change", () => {
          if (cb.checked) selected.add(cb.dataset.wid); else selected.delete(cb.dataset.wid);
          countEl.textContent = `${selected.size} selected`;
        });
      });
    }
    modal.querySelector("#bSearch").addEventListener("input", (e) => { filterQ = e.target.value; renderList(); });
    renderList();

    modal.querySelector('[data-act="cancel"]').onclick = close;
    modal.querySelector('[data-act="save"]').onclick = async () => {
      if (!selected.size) return toast("Select at least one employee", "error");
      const d = modal.querySelector("#bDate").value;
      const siteCode = modal.querySelector("#bSite").value;
      const site = sites.find((s) => s.code === siteCode);
      const inTime = modal.querySelector("#bInTime").value;
      const outTime = modal.querySelector("#bOutTime").value;
      const commonFields = {
        siteCode: site.code, siteName: site.name, shift: modal.querySelector("#bShift").value,
        markedBy: session.email, markedVia: "gps", lastAction: new Date().toISOString(),
        corrected: true, correctedBy: session.email, correctedAt: new Date().toISOString(),
      };
      if (inTime) { const [h, m] = inTime.split(":").map(Number); commonFields.in = DateUtils.isoFromLocalTime(d, h, m); }
      if (outTime) { const [h, m] = outTime.split(":").map(Number); commonFields.out = DateUtils.isoFromLocalTime(d, h, m); }
      await bulkWriteMark(d, [...selected], commonFields);
      toast(`${selected.size} record(s) marked`, "success");
      close();
    };
  });
}
