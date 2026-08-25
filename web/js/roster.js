import { getState, subscribe } from "./store.js";
import { submitArrival, decideArrival, addLeave, bulkAddLeaves, addHoliday, deleteHoliday, bulkAddHolidays, decideLeave } from "./data.js";
import { esc, toast, confirmDialog, openModal } from "./ui.js";
import * as DateUtils from "./dateUtils.js";
import * as WorkerSearch from "./workerSearch.js";
import { scanQr } from "./qr.js";
import { COMMON_DESIGNATIONS, LEAVE_TYPES, UAE_HOLIDAYS_MOHRE } from "./constants.js";

export function mount(root) {
  subscribe(() => render(root));
  render(root);
}

function render(root) {
  const { session } = getState();
  root.innerHTML = `<div id="rosterInner"></div>`;
  const inner = root.querySelector("#rosterInner");
  if (session.isAdmin) renderAdmin(inner);
  else renderSupervisor(inner);
}

// ---------------- Supervisor: report new arrival ----------------
let arrWorkerId = "", arrSite = "", arrName = "", arrDesig = "", arrStatus = "";

function renderSupervisor(root) {
  const { workers, sites, session } = getState();
  const existing = WorkerSearch.findExact(workers, arrWorkerId);
  root.innerHTML = `
    <div class="card">
      <h2>Report New Arrival</h2>
      <div class="field"><label>Project Site *</label><select id="arrSite">
        <option value="">Select Your Project Code</option>
        ${sites.map((s) => `<option value="${esc(s.code)}" ${s.code === arrSite ? "selected" : ""}>${esc(s.code)}</option>`).join("")}
      </select></div>
      <div class="field"><label>Worker ID *</label><input id="arrWorkerId" value="${esc(arrWorkerId)}" /></div>
      <button id="arrScan" class="btn btn-outline btn-block">📷 Scan QR / Barcode</button>
      ${arrWorkerId.trim() ? (existing
        ? `<p style="color:var(--green);font-weight:700;margin-top:8px">✅ ${esc(existing.name)} — ${esc(existing.designation)} (currently: ${esc(existing.site || "unassigned")})</p>`
        : `<p class="muted" style="margin-top:8px">🆕 New worker — ID not in the system yet.</p>`) : ""}
      ${arrWorkerId.trim() && !existing ? `
        <div class="field"><label>Full Name</label><input id="arrName" value="${esc(arrName)}" /></div>
        <div class="field"><label>Designation / Trade</label><input id="arrDesig" list="arrDesigList" value="${esc(arrDesig)}" />
          <datalist id="arrDesigList">${COMMON_DESIGNATIONS.map((d) => `<option value="${esc(d)}">`).join("")}</datalist>
        </div>` : ""}
      <button id="arrSubmit" class="btn btn-primary btn-block" style="margin-top:10px">Report New Arrival</button>
      ${arrStatus ? `<p class="muted" style="margin-top:8px">${esc(arrStatus)}</p>` : ""}
    </div>
  `;
  root.querySelector("#arrSite").addEventListener("change", (e) => (arrSite = e.target.value));
  root.querySelector("#arrWorkerId").addEventListener("input", (e) => { arrWorkerId = e.target.value; render2(root); });
  root.querySelector("#arrName")?.addEventListener("input", (e) => (arrName = e.target.value));
  root.querySelector("#arrDesig")?.addEventListener("input", (e) => (arrDesig = e.target.value));
  root.querySelector("#arrScan").addEventListener("click", async () => {
    const text = await scanQr();
    if (text) { arrWorkerId = text; render2(root); }
  });
  root.querySelector("#arrSubmit").addEventListener("click", async () => {
    const { session } = getState();
    if (!arrSite || !arrWorkerId.trim()) { arrStatus = "❌ Please select a site and enter a Worker ID."; return render2(root); }
    await submitArrival({
      site: arrSite, workerId: arrWorkerId.trim(), name: arrName.trim(), designation: arrDesig.trim(),
      requestedDate: DateUtils.todayStrUtc(), requestedBy: session.email, status: "pending", ts: DateUtils.nowIso(),
    });
    arrStatus = "✅ Reported — awaiting admin approval.";
    arrWorkerId = ""; arrName = ""; arrDesig = "";
    render2(root);
  });
  function render2(r) { renderSupervisor(r); }
}

// ---------------- Admin: arrivals, leave, bulk leave, holidays ----------------
let leaveWorkerId = "", leaveFrom = DateUtils.todayStrUtc(), leaveTo = DateUtils.todayStrUtc(), leaveType = "Annual", leaveReason = "";
let holidayDate = "", holidayName = "";

function renderAdmin(root) {
  const { workers, arrivals, leaves, holidays, session } = getState();
  const pending = arrivals.filter((a) => a.status === "pending");
  const w = WorkerSearch.findExact(workers, leaveWorkerId);

  root.innerHTML = `
    <div class="card">
      <h2>Pending Arrivals <span class="badge badge-amber">${pending.length}</span></h2>
      ${pending.length === 0 ? `<p class="muted">Nothing pending.</p>` : pending.map((a) => `
        <div class="card" style="background:var(--light-bg)">
          <b>${esc(a.name || a.workerId)}</b> — ${esc(a.designation || "")} · Site ${esc(a.site)}
          <div class="muted">Requested by ${esc(a.requestedBy)} on ${esc(a.requestedDate)}</div>
          <div class="row" style="margin-top:8px">
            <button class="btn btn-green" data-approve="${esc(a.docId)}">Approve</button>
            <button class="btn btn-red" data-reject="${esc(a.docId)}">Reject</button>
          </div>
        </div>
      `).join("")}
    </div>

    <div class="card">
      <h2>Mark Worker On Leave</h2>
      <div class="field"><label>Worker ID *</label><input id="lvWorkerId" value="${esc(leaveWorkerId)}" /></div>
      ${leaveWorkerId.trim() ? (w ? `<p style="color:var(--green)">✅ ${esc(w.name)}</p>` : `<p style="color:var(--red)">❌ Worker not found</p>`) : ""}
      <div class="row">
        <div class="field"><label>From *</label><input id="lvFrom" type="date" value="${leaveFrom}" /></div>
        <div class="field"><label>To *</label><input id="lvTo" type="date" value="${leaveTo}" /></div>
      </div>
      <div class="field"><label>Leave Type</label><select id="lvType">${LEAVE_TYPES.map((t) => `<option ${t === leaveType ? "selected" : ""}>${t}</option>`).join("")}</select></div>
      <div class="field"><label>Reason</label><input id="lvReason" value="${esc(leaveReason)}" /></div>
      <button id="lvSubmit" class="btn btn-primary btn-block">Mark On Leave</button>
    </div>

    <div class="card">
      <button id="bulkLeaveBtn" class="btn btn-outline btn-block">👥 Bulk Mark Leave (Multiple Employees)</button>
    </div>

    <div class="card">
      <h2>Company Holidays</h2>
      <div class="row">
        <input id="hDate" type="date" value="${holidayDate}" />
        <input id="hName" placeholder="Holiday name" value="${esc(holidayName)}" />
      </div>
      <button id="hAdd" class="btn btn-outline btn-block" style="margin-top:8px">Add Holiday</button>
      <button id="hSeed" class="btn btn-gold btn-block" style="margin-top:8px">📅 Load UAE MOHRE Holidays (2026–2027)</button>
      <p class="muted" style="margin-top:6px">⚠ Islamic-calendar dates are moon-sighting dependent and marked "(unconfirmed)" until MOHRE officially gazettes them.</p>
      <div class="table-wrap" style="margin-top:10px"><table><thead><tr><th>Date</th><th>Name</th><th></th></tr></thead><tbody>
        ${holidays.slice().sort((a, b) => a.date.localeCompare(b.date)).map((h) => `
          <tr><td>${esc(h.date)}</td><td>${esc(h.name)}</td><td><button class="btn btn-outline" data-delh="${esc(h.date)}">Delete</button></td></tr>
        `).join("")}
      </tbody></table></div>
    </div>

    <div class="card">
      <h2>Leave History</h2>
      <div class="table-wrap"><table><thead><tr><th>Worker</th><th>From</th><th>To</th><th>Type</th><th>Status</th></tr></thead><tbody>
        ${leaves.slice().sort((a, b) => (b.ts || "").localeCompare(a.ts || "")).slice(0, 100).map((l) => `
          <tr><td>${esc(l.workerId)}</td><td>${esc(l.fromDate)}</td><td>${esc(l.toDate)}</td><td>${esc(l.leaveType || "Annual")}</td>
          <td><span class="badge ${l.status === "approved" ? "badge-green" : l.status === "rejected" ? "badge-red" : "badge-amber"}">${esc(l.status)}</span>
          ${l.status === "pending" ? ` <button class="btn btn-outline" data-lvapprove="${esc(l.docId)}">Approve</button> <button class="btn btn-outline" data-lvreject="${esc(l.docId)}">Reject</button>` : ""}
          </td></tr>
        `).join("")}
      </tbody></table></div>
    </div>
  `;

  root.querySelectorAll("[data-approve]").forEach((b) => b.addEventListener("click", async () => {
    await decideArrival(b.dataset.approve, true, session.email);
    toast("Arrival approved", "success");
  }));
  root.querySelectorAll("[data-reject]").forEach((b) => b.addEventListener("click", async () => {
    await decideArrival(b.dataset.reject, false, session.email);
    toast("Arrival rejected", "success");
  }));

  root.querySelector("#lvWorkerId").addEventListener("input", (e) => { leaveWorkerId = e.target.value; renderAdmin(root); });
  root.querySelector("#lvFrom").addEventListener("input", (e) => (leaveFrom = e.target.value));
  root.querySelector("#lvTo").addEventListener("input", (e) => (leaveTo = e.target.value));
  root.querySelector("#lvType").addEventListener("change", (e) => (leaveType = e.target.value));
  root.querySelector("#lvReason").addEventListener("input", (e) => (leaveReason = e.target.value));
  root.querySelector("#lvSubmit").addEventListener("click", async () => {
    const w = WorkerSearch.findExact(getState().workers, leaveWorkerId);
    if (!w) return toast("Worker not found", "error");
    await addLeave({
      workerId: w.id, site: w.site || "", fromDate: leaveFrom, toDate: leaveTo, reason: leaveReason || null,
      leaveType, markedBy: session.email, ts: DateUtils.nowIso(), status: "approved",
    });
    toast("Leave marked", "success");
    leaveWorkerId = ""; leaveReason = "";
    renderAdmin(root);
  });

  root.querySelector("#bulkLeaveBtn").addEventListener("click", () => openBulkLeaveForm(root));

  root.querySelector("#hDate").addEventListener("input", (e) => (holidayDate = e.target.value));
  root.querySelector("#hName").addEventListener("input", (e) => (holidayName = e.target.value));
  root.querySelector("#hAdd").addEventListener("click", async () => {
    if (!holidayDate || !holidayName.trim()) return toast("Enter both a date and a name", "error");
    await addHoliday(holidayDate, holidayName.trim(), session.email);
    toast("Holiday added", "success");
    holidayDate = ""; holidayName = "";
    renderAdmin(root);
  });
  root.querySelector("#hSeed").addEventListener("click", async () => {
    await bulkAddHolidays(UAE_HOLIDAYS_MOHRE.map((h) => ({ ...h, addedBy: session.email, addedAt: DateUtils.nowIso() })));
    toast("UAE MOHRE holidays loaded", "success");
  });
  root.querySelectorAll("[data-delh]").forEach((b) => b.addEventListener("click", () => {
    confirmDialog(`Delete holiday ${b.dataset.delh}?`, async () => {
      await deleteHoliday(b.dataset.delh);
      toast("Holiday deleted", "success");
    });
  }));

  root.querySelectorAll("[data-lvapprove]").forEach((b) => b.addEventListener("click", async () => {
    await decideLeave(b.dataset.lvapprove, true, session.email);
    toast("Leave approved", "success");
  }));
  root.querySelectorAll("[data-lvreject]").forEach((b) => b.addEventListener("click", async () => {
    await decideLeave(b.dataset.lvreject, false, session.email);
    toast("Leave rejected", "success");
  }));
}

function openBulkLeaveForm(root) {
  {
    const { workers, session } = getState();
    let filterQ = "";
    const selected = new Set();
    openModal(`
      <h3>Bulk Mark Leave</h3>
      <div class="row">
        <div class="field"><label>From *</label><input id="blFrom" type="date" value="${DateUtils.todayStrUtc()}" /></div>
        <div class="field"><label>To *</label><input id="blTo" type="date" value="${DateUtils.todayStrUtc()}" /></div>
      </div>
      <div class="field"><label>Leave Type</label><select id="blType">${LEAVE_TYPES.map((t) => `<option>${t}</option>`).join("")}</select></div>
      <div class="field"><label>Reason</label><input id="blReason" /></div>
      <div class="field"><label>Search employees</label><input id="blSearch" placeholder="Filter by name/ID…" /></div>
      <div class="checklist" id="blChecklist"></div>
      <p class="muted" id="blCount">0 selected</p>
      <div class="modal-actions">
        <button class="btn btn-outline" data-act="cancel">Cancel</button>
        <button class="btn btn-primary" data-act="save">Mark Selected On Leave</button>
      </div>
    `, (modal, close) => {
      const listEl = modal.querySelector("#blChecklist");
      const countEl = modal.querySelector("#blCount");
      function renderList() {
        const q = filterQ.trim().toLowerCase();
        const rows = workers.filter((w) => !q || w.name.toLowerCase().includes(q) || w.id.includes(filterQ.trim())).slice(0, 200);
        listEl.innerHTML = rows.map((w) => `<label><input type="checkbox" data-wid="${esc(w.id)}" ${selected.has(w.id) ? "checked" : ""} /> ${esc(w.name)} — ${esc(w.id)}</label>`).join("");
        listEl.querySelectorAll("input[type=checkbox]").forEach((cb) => cb.addEventListener("change", () => {
          if (cb.checked) selected.add(cb.dataset.wid); else selected.delete(cb.dataset.wid);
          countEl.textContent = `${selected.size} selected`;
        }));
      }
      modal.querySelector("#blSearch").addEventListener("input", (e) => { filterQ = e.target.value; renderList(); });
      renderList();
      modal.querySelector('[data-act="cancel"]').onclick = close;
      modal.querySelector('[data-act="save"]').onclick = async () => {
        if (!selected.size) return toast("Select at least one employee", "error");
        const fromDate = modal.querySelector("#blFrom").value;
        const toDate = modal.querySelector("#blTo").value;
        const type = modal.querySelector("#blType").value;
        const reason = modal.querySelector("#blReason").value || null;
        const entries = [...selected].map((workerId) => {
          const w = workers.find((x) => x.id === workerId);
          return { workerId, site: w?.site || "", fromDate, toDate, reason, leaveType: type, markedBy: session.email, ts: DateUtils.nowIso(), status: "approved" };
        });
        await bulkAddLeaves(entries);
        toast(`${entries.length} worker(s) marked on leave`, "success");
        close();
      };
    });
  }
}
