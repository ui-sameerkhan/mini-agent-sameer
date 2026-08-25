import { getState, subscribe } from "./store.js";
import { generateFullBackupBlob } from "./attendanceReports.js";
import { batchUpsertWorkers, batchUpsertSites, batchUpsertAttendance, bulkAddHolidays } from "./data.js";
import { esc, toast, downloadBlob } from "./ui.js";

let exportStatus = "";
let restoreStatus = "";

export function mount(root) {
  subscribe(() => render(root));
  render(root);
}

function render(root) {
  root.innerHTML = `
    <div class="card">
      <h2>Full Data Backup (Admin Only)</h2>
      <p class="muted">One-tap export of every worker, site, attendance record, leave, and holiday — all time — into a single spreadsheet.</p>
      <button id="exportBtn" class="btn btn-primary btn-block">Export Full Backup</button>
      ${exportStatus ? `<p class="muted" style="margin-top:8px">${esc(exportStatus)}</p>` : ""}
    </div>

    <div class="card">
      <h2>Restore from Backup</h2>
      <p class="muted">Restores Workers, Sites, Attendance, and Holidays sheets by merging onto whatever's already there — existing data is never deleted. Leave requests are not restored (they use auto-generated IDs, so re-importing would create duplicates rather than update originals) — re-enter any missing leave records by hand.</p>
      <input type="file" id="restoreFile" accept=".xlsx,.xls" />
      ${restoreStatus ? `<p class="muted" style="margin-top:8px">${esc(restoreStatus)}</p>` : ""}
    </div>
  `;

  root.querySelector("#exportBtn").addEventListener("click", async () => {
    exportStatus = "Generating…"; render(root);
    try {
      const { workers, sites, leaves, holidays } = getState();
      const blob = await generateFullBackupBlob(workers, sites, leaves, holidays);
      downloadBlob(blob, `SitePulse_FullBackup_${Date.now()}.xlsx`);
      exportStatus = "✅ Backup downloaded.";
    } catch (e) {
      exportStatus = `❌ ${e.message}`;
    }
    render(root);
  });

  root.querySelector("#restoreFile").addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    restoreStatus = "Reading…"; render(root);
    try {
      const buf = await file.arrayBuffer();
      const wb = XLSX.read(buf, { type: "array" });
      let counts = [];

      const sheetRows = (name) => {
        const sheet = wb.Sheets[name];
        if (!sheet) return null;
        return XLSX.utils.sheet_to_json(sheet, { defval: "" });
      };

      const workersRows = sheetRows("WORKERS");
      if (workersRows) {
        const parsed = workersRows.map((r) => ({
          sno: Number(r["S.No"]) || 0, id: String(r["ID"]), name: String(r["Name"]),
          designation: String(r["Designation"] || "Worker"), company: r["Company"] || null,
          site: r["Site"] || null, alignedDate: r["Aligned Date"] || null, status: r["Status"] || "active",
          leftDate: r["Left Date"] || null, annualLeaveDays: Number(r["Annual Leave Days"]) || 30,
        })).filter((w) => w.id);
        await batchUpsertWorkers(parsed);
        counts.push(`${parsed.length} worker(s)`);
      }

      const sitesRows = sheetRows("SITES");
      if (sitesRows) {
        const parsed = sitesRows.map((r) => ({
          code: String(r["Code"]).toUpperCase(), name: String(r["Name"]), lat: Number(r["Lat"]), lng: Number(r["Lng"]),
          radius: Number(r["Radius"]) || 500, wifiSsid: r["WiFi SSID"] || null,
          nightStartHour: r["Night Start Hour"] === "" ? null : Number(r["Night Start Hour"]),
          dayStartHour: r["Day Start Hour"] === "" ? null : Number(r["Day Start Hour"]),
        })).filter((s) => s.code);
        await batchUpsertSites(parsed);
        counts.push(`${parsed.length} site(s)`);
      }

      const attRows = sheetRows("ATTENDANCE");
      if (attRows) {
        const parsed = attRows.map((r) => ({
          date: String(r["Date"]), workerId: String(r["Worker ID"]), siteCode: r["Site Code"] || "",
          siteName: r["Site Name"] || "", shift: r["Shift"] || null, in: r["In"] || null, out: r["Out"] || null,
          markedBy: r["Marked By"] || "", markedVia: r["Marked Via"] || "gps",
          corrected: r["Corrected"] === "YES", correctedBy: r["Corrected By"] || null, correctedAt: r["Corrected At"] || null,
        })).filter((a) => a.date && a.workerId);
        await batchUpsertAttendance(parsed);
        counts.push(`${parsed.length} attendance record(s)`);
      }

      const holRows = sheetRows("HOLIDAYS");
      if (holRows) {
        const parsed = holRows.map((r) => ({ date: String(r["Date"]), name: String(r["Name"]), addedBy: r["Added By"] || "" })).filter((h) => h.date);
        await bulkAddHolidays(parsed);
        counts.push(`${parsed.length} holiday(s)`);
      }

      restoreStatus = counts.length ? `✅ Restored: ${counts.join(", ")}.` : "⚠ No recognized sheets found in this file.";
      toast("Restore complete", "success");
    } catch (err) {
      restoreStatus = `❌ ${err.message}`;
      toast(`Restore failed: ${err.message}`, "error");
    } finally {
      e.target.value = "";
      render(root);
    }
  });
}
