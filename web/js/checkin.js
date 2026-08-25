import { getState, subscribe } from "./store.js";
import * as WorkerSearch from "./workerSearch.js";
import * as Engine from "./attendanceEngine.js";
import { toast, esc } from "./ui.js";
import { scanQr } from "./qr.js";
import { getStaffLink, setStaffLink } from "./data.js";

let query = "";
let lastResult = null;
let inFlight = false;

export function mount(root) {
  subscribe(() => render(root));
  render(root);
}

async function ensureLinkedWorkerId() {
  const { session } = getState();
  if (!session.isOfficeStaff) return null;
  if (getState().myLinkedWorkerId) return getState().myLinkedWorkerId;
  const link = await getStaffLink(session.email);
  if (link?.workerId) {
    query = link.workerId;
    return link.workerId;
  }
  return null;
}

function render(root) {
  const { workers, sites, todayAttendance, session } = getState();
  const isLocked = session.isOfficeStaff && !!getState().myLinkedWorkerId;
  const selected = WorkerSearch.findExact(workers, query);
  const suggestions = !selected ? WorkerSearch.suggest(workers, query) : [];

  root.innerHTML = `
    <div class="card" style="text-align:center">
      <div style="font-size:34px">👷</div>
      <h2>Enter Worker ID or Name</h2>
      <input id="ciQuery" type="text" placeholder="ID or Name" value="${esc(query)}"
        style="width:100%;text-align:center;font-size:18px;padding:12px;border-radius:10px;border:1px solid var(--line)"
        ${isLocked ? "readonly" : ""} />
      ${isLocked ? `<p class="muted" style="margin-top:8px">🔒 This account is permanently linked to Worker ID ${esc(query)}</p>` : ""}
      ${!isLocked ? `<button id="ciScanBtn" class="btn btn-outline btn-block" style="margin-top:8px">📷 Scan QR / Barcode</button>` : ""}
      ${!selected && suggestions.length ? `<div style="margin-top:8px;text-align:left">${suggestions.map((w) =>
        `<div class="suggest-item" data-id="${esc(w.id)}">${esc(w.name)} — ID ${esc(w.id)}</div>`).join("")}</div>` : ""}
      ${query.trim() ? `<p style="margin-top:10px;font-weight:700;color:${selected ? "var(--green)" : "var(--red)"}">
        ${selected ? `✅ ${esc(selected.name)} — ${esc(selected.designation)} (ID ${esc(selected.id)})` : suggestions.length ? "" : `❌ No worker found matching "${esc(query)}"`}
      </p>` : ""}
      <div class="row" style="margin-top:10px">
        <button id="ciIn" class="btn btn-green btn-block" ${!selected || inFlight ? "disabled" : ""}>CHECK IN</button>
        <button id="ciOut" class="btn btn-red btn-block" ${!selected || inFlight ? "disabled" : ""}>CHECK OUT</button>
      </div>
      ${inFlight ? `<p class="muted" style="margin-top:10px">📡 Checking location…</p>` : ""}
      ${lastResult ? `<div class="result-panel result-${resultClass(lastResult.kind)}">
        <div class="title">${esc(lastResult.title)}</div><div class="detail">${esc(lastResult.detail)}</div>
      </div>` : ""}
    </div>
    <button id="ciReportArrival" class="btn btn-outline btn-block" style="margin-top:10px">📋 Report a New Worker Arrival</button>
    ${session.isOfficeStaff ? `<button id="ciMyLeave" class="btn btn-outline btn-block" style="margin-top:8px">🗓️ My Leave & Attendance</button>` : ""}
  `;

  root.querySelector("#ciQuery")?.addEventListener("input", (e) => { query = e.target.value; render(root); });
  root.querySelectorAll(".suggest-item").forEach((el) => {
    el.addEventListener("click", () => { query = el.dataset.id; render(root); });
  });
  root.querySelector("#ciScanBtn")?.addEventListener("click", async () => {
    const text = await scanQr();
    if (text) { query = text; render(root); }
  });
  root.querySelector("#ciIn")?.addEventListener("click", () => doMark("IN", root));
  root.querySelector("#ciOut")?.addEventListener("click", () => doMark("OUT", root));
  root.querySelector("#ciReportArrival")?.addEventListener("click", () => {
    document.querySelector('.tab-btn[data-view="roster"]')?.click();
  });
  root.querySelector("#ciMyLeave")?.addEventListener("click", () => {
    document.querySelector('.tab-btn[data-view="officestaff"]')?.click();
  });

  ensureLinkedWorkerId().then((id) => { if (id) render(root); });
}

function resultClass(kind) {
  if (kind === "success") return "success";
  if (kind === "blocked") return "blocked";
  if (kind === "failure") return "failure";
  return "rejected";
}

async function doMark(dir, root) {
  const { workers, sites, todayAttendance, session } = getState();
  const worker = WorkerSearch.findExact(workers, query);
  if (!worker) return;
  inFlight = true;
  render(root);
  try {
    const lockedWorkerId = session.isOfficeStaff ? getState().myLinkedWorkerId : null;
    const result = await Engine.mark(dir, worker, {
      isAdmin: session.isAdmin,
      currentEmail: session.email,
      sites,
      todayAttendance,
      lockedWorkerId,
    });
    lastResult = result;
    if (result.kind === "success") {
      if (session.isOfficeStaff && !getState().myLinkedWorkerId) {
        await setStaffLink(session.email, worker.id);
      }
      query = "";
      toast(result.title, "success");
    } else if (result.kind === "blocked" || result.kind === "failure") {
      toast(result.title, "error");
    }
  } catch (e) {
    lastResult = { kind: "failure", title: "⚠️ ERROR", detail: e.message };
  } finally {
    inFlight = false;
    render(root);
  }
}
