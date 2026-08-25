import { getState, subscribe } from "./store.js";
import { saveWorker, deleteWorker, batchUpsertWorkers, deleteAllWorkers } from "./data.js";
import { esc, toast, openModal, confirmDialog, downloadBlob } from "./ui.js";
import { COMMON_DESIGNATIONS, WORKERS_PAGE_SIZE } from "./constants.js";
import { generateQrDataUrl, generateBulkQrPdf } from "./qr.js";

let query = "";
let siteFilter = "ALL";
let page = 1;

export function mount(root) {
  subscribe(() => render(root));
  render(root);
}

function filtered() {
  const { workers } = getState();
  return workers.filter((w) => {
    const q = query.trim().toLowerCase();
    const matchesQuery = !q || w.name.toLowerCase().includes(q) || w.id.includes(query.trim()) || w.designation.toLowerCase().includes(q);
    const matchesSite = siteFilter === "ALL" ? true : siteFilter === "NONE" ? !w.site : w.site === siteFilter;
    return matchesQuery && matchesSite;
  });
}

function render(root) {
  const { sites } = getState();
  const list = filtered();
  const totalPages = Math.max(1, Math.ceil(list.length / WORKERS_PAGE_SIZE));
  page = Math.min(page, totalPages);
  const pageItems = list.slice((page - 1) * WORKERS_PAGE_SIZE, page * WORKERS_PAGE_SIZE);

  root.innerHTML = `
    <div class="card">
      <h2>📊 Upload Workers from Excel</h2>
      <p class="muted">Required columns: Employee ID, Employee Name, Designation</p>
      <input type="file" id="xlWorkers" accept=".xlsx,.xls,.csv" />
      <div id="xlWorkersStatus" class="muted" style="margin-top:6px"></div>
    </div>
    <div class="card">
      <h2>🏢 Upload Outsource Manpower</h2>
      <p class="muted">Columns: Worker ID, Name, Company Name, Designation/Trade (optional)</p>
      <input type="file" id="xlOutsource" accept=".xlsx,.xls,.csv" />
      <div id="xlOutsourceStatus" class="muted" style="margin-top:6px"></div>
    </div>

    <div class="card">
      <button id="delAllBtn" class="btn btn-red btn-block">Delete All Workers</button>
      <input id="wQuery" placeholder="🔍 Search name / ID / trade…" value="${esc(query)}" style="margin-top:10px" />
      <select id="wSiteFilter" style="margin-top:8px">
        <option value="ALL">All Sites</option>
        <option value="NONE">Not Assigned</option>
        ${sites.map((s) => `<option value="${esc(s.code)}" ${siteFilter === s.code ? "selected" : ""}>${esc(s.code)}</option>`).join("")}
      </select>
      <button id="addWorkerBtn" class="btn btn-gold btn-block" style="margin-top:10px">Add Employee</button>
      <button id="bulkQrBtn" class="btn btn-outline btn-block" style="margin-top:8px">🖨️ Bulk Generate QR Badges (${list.length})</button>
      <div id="bulkQrStatus" class="muted" style="margin-top:6px"></div>
      <p class="muted" style="margin-top:8px">Showing ${pageItems.length} of ${list.length} • Page ${page}/${totalPages}</p>
    </div>

    ${pageItems.map((w) => `
      <div class="card">
        <b>${esc(w.name)}</b>
        <div class="muted">${esc(w.designation)} · ID ${esc(w.id)}</div>
        <div class="muted">${w.site ? `<span class="badge badge-green">${esc(w.site)}</span>` : ""} ${w.company ? esc(w.company) : ""}</div>
        <div class="row" style="margin-top:8px">
          <button class="btn btn-outline" data-edit="${esc(w.id)}">Edit</button>
          <button class="btn btn-outline" data-qr="${esc(w.id)}">📷 QR</button>
          <button class="btn btn-outline" data-del="${esc(w.id)}">Delete</button>
        </div>
      </div>
    `).join("")}

    ${totalPages > 1 ? `<div class="row">
      <button id="pgPrev" class="btn btn-outline" ${page <= 1 ? "disabled" : ""}>Prev</button>
      <button id="pgNext" class="btn btn-outline" ${page >= totalPages ? "disabled" : ""}>Next</button>
    </div>` : ""}
  `;

  root.querySelector("#wQuery").addEventListener("input", (e) => { query = e.target.value; page = 1; render(root); });
  root.querySelector("#wSiteFilter").addEventListener("change", (e) => { siteFilter = e.target.value; page = 1; render(root); });
  root.querySelector("#pgPrev")?.addEventListener("click", () => { page--; render(root); });
  root.querySelector("#pgNext")?.addEventListener("click", () => { page++; render(root); });
  root.querySelector("#addWorkerBtn").addEventListener("click", () => openWorkerForm(null));
  root.querySelectorAll("[data-edit]").forEach((b) => b.addEventListener("click", () => {
    openWorkerForm(getState().workers.find((w) => w.id === b.dataset.edit));
  }));
  root.querySelectorAll("[data-del]").forEach((b) => b.addEventListener("click", () => {
    confirmDialog(`Delete worker ${b.dataset.del}?`, async () => {
      await deleteWorker(b.dataset.del);
      toast("Worker deleted", "success");
    });
  }));
  root.querySelectorAll("[data-qr]").forEach((b) => b.addEventListener("click", () => {
    showWorkerQr(getState().workers.find((w) => w.id === b.dataset.qr));
  }));
  root.querySelector("#delAllBtn").addEventListener("click", () => {
    confirmDialog(`Delete ALL ${getState().workers.length} workers? This cannot be undone.`, async () => {
      await deleteAllWorkers(getState().workers.map((w) => w.id));
      toast("All workers deleted", "success");
    });
  });
  root.querySelector("#xlWorkers").addEventListener("change", (e) => handleUpload(e, false, root));
  root.querySelector("#xlOutsource").addEventListener("change", (e) => handleUpload(e, true, root));
  root.querySelector("#bulkQrBtn").addEventListener("click", async () => {
    const statusEl = root.querySelector("#bulkQrStatus");
    statusEl.textContent = "Generating…";
    try {
      const blob = await generateBulkQrPdf(list);
      downloadBlob(blob, `worker_qr_badges_${Date.now()}.pdf`);
      statusEl.textContent = `✅ ${list.length} badge(s) ready.`;
    } catch (e) {
      statusEl.textContent = `❌ ${e.message}`;
    }
  });
}

function findHeaderIndex(headerRow, candidates) {
  const lower = headerRow.map((h) => String(h || "").trim().toLowerCase());
  for (const c of candidates) {
    const idx = lower.findIndex((h) => h === c);
    if (idx >= 0) return idx;
  }
  for (const c of candidates) {
    const idx = lower.findIndex((h) => h.includes(c));
    if (idx >= 0) return idx;
  }
  return -1;
}

async function handleUpload(e, isOutsource, root) {
  const file = e.target.files[0];
  if (!file) return;
  const statusEl = root.querySelector(isOutsource ? "#xlOutsourceStatus" : "#xlWorkersStatus");
  statusEl.textContent = "Reading…";
  try {
    const buf = await file.arrayBuffer();
    const wb = XLSX.read(buf, { type: "array" });
    const sheet = wb.Sheets[wb.SheetNames[0]];
    const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: "" });
    if (!rows.length) throw new Error("Empty file");
    const header = rows[0];
    const idIdx = findHeaderIndex(header, isOutsource ? ["worker id", "employee id", "id"] : ["employee id", "worker id", "id"]);
    const nameIdx = findHeaderIndex(header, isOutsource ? ["name", "employee name"] : ["employee name", "name"]);
    const desigIdx = findHeaderIndex(header, ["designation", "designation/trade", "trade"]);
    const companyIdx = findHeaderIndex(header, ["company name", "company"]);
    if (idIdx < 0 || nameIdx < 0) throw new Error("Could not find ID/Name columns in the header row.");

    const { workers } = getState();
    let sno = (workers.reduce((m, w) => Math.max(m, w.sno || 0), 0)) + 1;
    const parsed = [];
    for (let i = 1; i < rows.length; i++) {
      const r = rows[i];
      const id = String(r[idIdx] ?? "").trim();
      const name = String(r[nameIdx] ?? "").trim();
      if (!id || !name) continue;
      const existing = workers.find((w) => w.id === id);
      parsed.push({
        sno: existing?.sno ?? sno++,
        id, name,
        designation: (desigIdx >= 0 ? String(r[desigIdx] ?? "").trim() : "") || existing?.designation || "Worker",
        company: isOutsource ? (companyIdx >= 0 ? String(r[companyIdx] ?? "").trim() || null : null) : (existing?.company ?? null),
        site: existing?.site ?? null,
        status: existing?.status ?? "active",
        annualLeaveDays: existing?.annualLeaveDays ?? 30,
      });
    }
    if (!parsed.length) throw new Error("No valid rows found.");
    statusEl.textContent = `Uploading ${parsed.length} worker(s)…`;
    await batchUpsertWorkers(parsed, (done, total) => { statusEl.textContent = `Uploading ${done}/${total}…`; });
    statusEl.textContent = `✅ ${parsed.length} worker(s) uploaded.`;
    toast("Upload complete", "success");
  } catch (err) {
    statusEl.textContent = `❌ ${err.message}`;
    toast(`Upload failed: ${err.message}`, "error");
  } finally {
    e.target.value = "";
  }
}

function openWorkerForm(existing) {
  const isEdit = !!existing;
  openModal(`
    <h3>${isEdit ? "Edit" : "Add"} Employee</h3>
    <div class="field"><label>Employee ID *</label><input id="wId" value="${esc(existing?.id || "")}" ${isEdit ? "readonly" : ""} /></div>
    <div class="field"><label>Full Name *</label><input id="wName" value="${esc(existing?.name || "")}" /></div>
    <div class="field"><label>Designation *</label>
      <input id="wDesig" list="desigList" value="${esc(existing?.designation || "")}" />
      <datalist id="desigList">${COMMON_DESIGNATIONS.map((d) => `<option value="${esc(d)}">`).join("")}</datalist>
    </div>
    <div class="field"><label>Company (optional)</label><input id="wCompany" value="${esc(existing?.company || "")}" /></div>
    <div class="field"><label>Annual Leave Days</label><input id="wLeaveDays" type="number" value="${existing?.annualLeaveDays ?? 30}" /></div>
    <div class="modal-actions">
      <button class="btn btn-outline" data-act="cancel">Cancel</button>
      <button class="btn btn-primary" data-act="save">Save</button>
    </div>
  `, (modal, close) => {
    modal.querySelector('[data-act="cancel"]').onclick = close;
    modal.querySelector('[data-act="save"]').onclick = async () => {
      const id = modal.querySelector("#wId").value.trim();
      const name = modal.querySelector("#wName").value.trim();
      const designation = modal.querySelector("#wDesig").value.trim();
      if (!id || !name || !designation) return toast("Please fill in all required fields", "error");
      const { workers } = getState();
      const sno = existing?.sno ?? ((workers.reduce((m, w) => Math.max(m, w.sno || 0), 0)) + 1);
      await saveWorker({
        sno, id, name, designation,
        company: modal.querySelector("#wCompany").value.trim() || null,
        site: existing?.site ?? null,
        alignedDate: existing?.alignedDate ?? null,
        status: existing?.status ?? "active",
        leftDate: existing?.leftDate ?? null,
        annualLeaveDays: parseInt(modal.querySelector("#wLeaveDays").value, 10) || 30,
      });
      toast("Worker saved", "success");
      close();
    };
  });
}

async function showWorkerQr(worker) {
  if (!worker) return;
  const dataUrl = await generateQrDataUrl(worker.id, 260);
  openModal(`
    <h3>ID Badge QR — ${esc(worker.name)}</h3>
    <div style="text-align:center">
      <img src="${dataUrl}" width="220" height="220" alt="QR code for ${esc(worker.id)}" />
      <p class="muted">Worker ID: ${esc(worker.id)}</p>
      <p class="muted">Scan this at check-in instead of typing the ID.</p>
    </div>
    <div class="modal-actions">
      <button class="btn btn-outline" data-act="close">Close</button>
      <button class="btn btn-primary" data-act="dl">Download</button>
    </div>
  `, (modal, close) => {
    modal.querySelector('[data-act="close"]').onclick = close;
    modal.querySelector('[data-act="dl"]').onclick = () => {
      const a = document.createElement("a");
      a.href = dataUrl; a.download = `worker_${worker.id}_qr.png`;
      document.body.appendChild(a); a.click(); a.remove();
    };
  });
}
