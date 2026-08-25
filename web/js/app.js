import { isConfigured } from "./firebase-init.js";
import { watchSession, login, sendPasswordReset, logout } from "./auth.js";
import { getState, setState, subscribe } from "./store.js";
import {
  liveWorkers, liveSites, liveToday, liveHolidays, liveLatestAnnouncement,
  liveLeaves, liveBlocked, liveArrivals, getStaffLink,
} from "./data.js";
import { enablePush } from "./push.js";
import { esc } from "./ui.js";

import * as CheckIn from "./checkin.js";
import * as Dashboard from "./dashboard.js";
import * as Sites from "./sites.js";
import * as Workers from "./workers.js";
import * as AttendanceView from "./attendance.js";
import * as Roster from "./roster.js";
import * as OfficeStaff from "./officestaff.js";
import * as Backup from "./backup.js";

const TABS = {
  checkin: { label: "Check-In", mod: CheckIn, roles: ["isAdmin", "isSupervisor", "isOfficeStaff"] },
  dashboard: { label: "Dashboard", mod: Dashboard, roles: ["isAdmin"] },
  sites: { label: "Sites", mod: Sites, roles: ["isAdmin"] },
  workers: { label: "Workers", mod: Workers, roles: ["isAdmin"] },
  attendance: { label: "Attendance", mod: AttendanceView, roles: ["isAdmin"] },
  roster: { label: "Roster", mod: Roster, roles: ["isAdmin", "isSupervisor"] },
  officestaff: { label: "My Leave & Attendance", mod: OfficeStaff, roles: ["isOfficeStaff"] },
  backup: { label: "Backup", mod: Backup, roles: ["isAdmin"] },
};

let mountedViews = new Set();
let dataUnsubscribers = [];

function roleAllowsTab(session, tab) {
  return tab.roles.some((r) => session[r]);
}

function buildNav(session) {
  const nav = document.getElementById("tabNav");
  const visible = Object.entries(TABS).filter(([, t]) => roleAllowsTab(session, t));
  nav.innerHTML = visible.map(([key, t], i) =>
    `<button class="tab-btn ${i === 0 ? "active" : ""}" data-view="${key}">${esc(t.label)}</button>`
  ).join("");
  visible.forEach(([key], i) => {
    document.getElementById(`view-${key}`).classList.toggle("active", i === 0);
    if (i === 0) ensureMounted(key);
  });
  nav.querySelectorAll(".tab-btn").forEach((btn) => {
    btn.addEventListener("click", () => switchTab(btn.dataset.view));
  });
}

function switchTab(key) {
  document.querySelectorAll(".tab-btn").forEach((b) => b.classList.toggle("active", b.dataset.view === key));
  document.querySelectorAll(".view").forEach((v) => v.classList.toggle("active", v.id === `view-${key}`));
  ensureMounted(key);
}

function ensureMounted(key) {
  if (mountedViews.has(key)) return;
  mountedViews.add(key);
  const root = document.getElementById(`view-${key}`);
  TABS[key].mod.mount(root);
}

function stopDataListeners() {
  dataUnsubscribers.forEach((fn) => fn());
  dataUnsubscribers = [];
}

function startDataListeners(session) {
  stopDataListeners();
  dataUnsubscribers.push(liveWorkers((rows) => setState({ workers: rows })));
  dataUnsubscribers.push(liveSites((rows) => setState({ sites: rows })));
  dataUnsubscribers.push(liveToday((rows) => setState({ todayAttendance: rows })));
  dataUnsubscribers.push(liveHolidays((rows) => setState({ holidays: rows })));
  dataUnsubscribers.push(liveLatestAnnouncement((a) => setState({ announcement: a })));
  if (session.isAdmin) {
    dataUnsubscribers.push(liveLeaves((rows) => setState({ leaves: rows })));
    dataUnsubscribers.push(liveBlocked((rows) => setState({ blocked: rows })));
    dataUnsubscribers.push(liveArrivals((rows) => setState({ arrivals: rows })));
  }
  if (session.isOfficeStaff) {
    getStaffLink(session.email).then((link) => {
      if (link?.workerId) setState({ myLinkedWorkerId: link.workerId });
    });
  }
}

function renderAnnouncementBanner() {
  const { announcement } = getState();
  const el = document.getElementById("announcementBanner");
  if (announcement?.message) {
    el.textContent = `📢 ${announcement.message}`;
    el.classList.remove("hidden");
  } else {
    el.classList.add("hidden");
  }
}

function showApp(session) {
  document.getElementById("loginView").classList.add("hidden");
  document.getElementById("appShell").classList.remove("hidden");
  document.getElementById("userEmailLabel").textContent = session.email;
  mountedViews = new Set();
  buildNav(session);
  startDataListeners(session);
}

function showLogin() {
  document.getElementById("appShell").classList.add("hidden");
  document.getElementById("loginView").classList.remove("hidden");
  stopDataListeners();
  setState({
    workers: [], sites: [], todayAttendance: [], leaves: [], holidays: [], blocked: [], arrivals: [],
    announcement: null, myLinkedWorkerId: null,
  });
}

function init() {
  if (!isConfigured) {
    document.getElementById("configWarning").classList.remove("hidden");
    document.getElementById("loginForm").querySelector("button[type=submit]").disabled = true;
    return;
  }

  subscribe(renderAnnouncementBanner);

  document.getElementById("loginForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const email = document.getElementById("loginEmail").value;
    const password = document.getElementById("loginPassword").value;
    const statusEl = document.getElementById("loginStatus");
    statusEl.textContent = "Signing in…";
    try {
      await login(email, password);
      statusEl.textContent = "";
    } catch (err) {
      statusEl.textContent = `❌ ${err.message}`;
    }
  });

  document.getElementById("forgotPasswordBtn").addEventListener("click", async () => {
    const email = document.getElementById("loginEmail").value;
    const statusEl = document.getElementById("loginStatus");
    if (!email) { statusEl.textContent = "Enter your email above first, then tap Forgot password."; return; }
    try {
      await sendPasswordReset(email);
      statusEl.textContent = "✅ Password reset email sent.";
    } catch (err) {
      statusEl.textContent = `❌ ${err.message}`;
    }
  });

  document.getElementById("logoutBtn").addEventListener("click", () => logout());
  document.getElementById("enableNotifsBtn").addEventListener("click", () => enablePush(getState().session.email));

  let lastLoggedIn = false;
  watchSession((session) => {
    setState({ session });
    if (session.isLoggedIn && !lastLoggedIn) showApp(session);
    else if (!session.isLoggedIn && lastLoggedIn) showLogin();
    else if (session.isLoggedIn) {
      document.getElementById("userEmailLabel").textContent = session.email;
    }
    lastLoggedIn = session.isLoggedIn;
  });
}

init();
