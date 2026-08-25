import { getState, subscribe } from "./store.js";
import { saveSite, deleteSite } from "./data.js";
import { esc, toast, openModal, confirmDialog } from "./ui.js";

export function mount(root) {
  subscribe(() => render(root));
  render(root);
}

function render(root) {
  const { sites } = getState();
  root.innerHTML = `
    <div class="card">
      <h2>Project Sites</h2>
      <button id="addSiteBtn" class="btn btn-gold btn-block">+ Add Project Site</button>
    </div>
    ${sites.map((s) => `
      <div class="card">
        <div style="display:flex;justify-content:space-between;align-items:start">
          <div>
            <b>${esc(s.code)}</b> — ${esc(s.name)}
            <div class="muted">Radius: ${s.radius}m ${s.wifiSsid ? `· WiFi: ${esc(s.wifiSsid)}` : ""}</div>
            ${s.nightStartHour != null || s.dayStartHour != null ? `<div class="muted">🕒 Custom shift: Night ${s.nightStartHour ?? 18}:00 · Day ${s.dayStartHour ?? 5}:00</div>` : ""}
            <div class="muted">${s.lat.toFixed(5)}, ${s.lng.toFixed(5)}</div>
          </div>
        </div>
        <div class="row" style="margin-top:10px">
          <button class="btn btn-outline" data-edit="${esc(s.code)}">Edit</button>
          <button class="btn btn-outline" data-del="${esc(s.code)}">Delete</button>
        </div>
      </div>
    `).join("")}
  `;

  root.querySelector("#addSiteBtn")?.addEventListener("click", () => openSiteForm(null));
  root.querySelectorAll("[data-edit]").forEach((b) => b.addEventListener("click", () => {
    openSiteForm(sites.find((s) => s.code === b.dataset.edit));
  }));
  root.querySelectorAll("[data-del]").forEach((b) => b.addEventListener("click", () => {
    confirmDialog(`Delete site ${b.dataset.del}?`, async () => {
      await deleteSite(b.dataset.del);
      toast("Site deleted", "success");
    });
  }));
}

function openSiteForm(existing) {
  const isEdit = !!existing;
  openModal(`
    <h3>${isEdit ? "Edit" : "Add"} Project Site</h3>
    <div class="field"><label>Project Code *</label><input id="sCode" value="${esc(existing?.code || "")}" ${isEdit ? "readonly" : ""} /></div>
    <div class="field"><label>Site Name *</label><input id="sName" value="${esc(existing?.name || "")}" /></div>
    <div class="row">
      <div class="field"><label>Latitude *</label><input id="sLat" type="number" step="any" value="${existing?.lat ?? ""}" /></div>
      <div class="field"><label>Longitude *</label><input id="sLng" type="number" step="any" value="${existing?.lng ?? ""}" /></div>
    </div>
    <button id="sUseGps" class="btn btn-outline btn-block">📍 Use My Current Location</button>
    <div class="field" style="margin-top:10px"><label>Geofence Radius (meters)</label><input id="sRadius" type="number" value="${existing?.radius ?? 500}" /></div>
    <div class="field"><label>Office WiFi Network Name (optional)</label><input id="sWifi" value="${esc(existing?.wifiSsid || "")}" />
      <span class="muted">Note: browsers cannot read the connected WiFi name for privacy reasons, so WiFi punch-in only works from the Android app — this value still syncs to it.</span></div>
    <div class="row">
      <div class="field"><label>Night Starts (0-23)</label><input id="sNight" type="number" min="0" max="23" value="${existing?.nightStartHour ?? ""}" placeholder="18" /></div>
      <div class="field"><label>Day Starts (0-23)</label><input id="sDay" type="number" min="0" max="23" value="${existing?.dayStartHour ?? ""}" placeholder="5" /></div>
    </div>
    <div class="modal-actions">
      <button class="btn btn-outline" data-act="cancel">Cancel</button>
      <button class="btn btn-primary" data-act="save">Save</button>
    </div>
  `, (modal, close) => {
    modal.querySelector('[data-act="cancel"]').onclick = close;
    modal.querySelector("#sUseGps").onclick = () => {
      if (!navigator.geolocation) return toast("Geolocation not supported by this browser", "error");
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          modal.querySelector("#sLat").value = pos.coords.latitude;
          modal.querySelector("#sLng").value = pos.coords.longitude;
        },
        (err) => toast(`Location failed: ${err.message}`, "error"),
        { enableHighAccuracy: true }
      );
    };
    modal.querySelector('[data-act="save"]').onclick = async () => {
      const code = modal.querySelector("#sCode").value.trim().toUpperCase();
      const name = modal.querySelector("#sName").value.trim();
      const lat = parseFloat(modal.querySelector("#sLat").value);
      const lng = parseFloat(modal.querySelector("#sLng").value);
      if (!code || !name || isNaN(lat) || isNaN(lng)) return toast("Please fill in all required fields", "error");
      const radius = parseInt(modal.querySelector("#sRadius").value, 10) || 500;
      const wifiSsid = modal.querySelector("#sWifi").value.trim() || null;
      const nightRaw = modal.querySelector("#sNight").value;
      const dayRaw = modal.querySelector("#sDay").value;
      const nightStartHour = nightRaw === "" ? null : Math.min(23, Math.max(0, parseInt(nightRaw, 10)));
      const dayStartHour = dayRaw === "" ? null : Math.min(23, Math.max(0, parseInt(dayRaw, 10)));
      await saveSite({ code, name, lat, lng, radius, wifiSsid, nightStartHour, dayStartHour });
      toast("Site saved", "success");
      close();
    };
  });
}
