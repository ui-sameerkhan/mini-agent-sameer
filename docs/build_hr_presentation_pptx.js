const pptxgen = require("pptxgenjs");

// ---------------------------------------------------------------------------------------------
// SitePulse — HR presentation.
//
// Palette is taken from the app's own theme (ui/theme/Color.kt) rather than a stock deck palette,
// so the slides and the product a viewer opens afterwards look like the same thing.
// ---------------------------------------------------------------------------------------------
const NAVY   = "00113D"; // SpBrandBlueDark — dominant on dark slides
const BLUE   = "0378E2"; // SpBrandBlue
const BLUEMD = "0B66D6"; // SpBrandBlueMid
const BLUESF = "E8F1FC"; // SpBrandBlueSoft — card tint
const GOLD   = "F9CB14"; // SpBrandGold — the sharp accent
const GREEN  = "17875E"; // SpGreenMid
const AMBER  = "B8842E"; // SpAmber
const RED    = "B3261E"; // SpRed
const INK    = "141F1A"; // SpInk
const MUTED  = "5B6B63"; // SpMuted
const PAPER  = "F5F7F5"; // SpPaper
const WHITE  = "FFFFFF";
const LINE   = "DEE6E1"; // SpLine

const HEAD = "Cambria";  // safe-list serif
const BODY = "Calibri";  // safe-list sans

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9"; // 10" x 5.625"
pres.author = "SitePulse";
pres.company = "KTC International Contracting";
pres.title = "SitePulse — Workforce Attendance & Manpower Control";

const W = 10, H = 5.625, M = 0.5;

// ---- helpers ---------------------------------------------------------------------------------

function lightSlide() {
  const s = pres.addSlide();
  s.background = { color: WHITE };
  return s;
}

function darkSlide() {
  const s = pres.addSlide();
  s.background = { color: NAVY };
  return s;
}

/** Slide title. Kept at a single y so every content slide starts on the same line. */
function heading(s, text, opts = {}) {
  s.addText(text, {
    x: M, y: 0.34, w: W - M * 2, h: 0.62,
    fontFace: HEAD, fontSize: opts.size || 32, bold: true,
    color: opts.color || NAVY, align: "left", isTextBox: true, margin: 0,
  });
  if (opts.sub) {
    s.addText(opts.sub, {
      x: M, y: 1.02, w: W - M * 2, h: 0.34,
      fontFace: BODY, fontSize: 13, color: opts.subColor || MUTED,
      isTextBox: true, margin: 0,
    });
  }
}

/** Rounded card. The deck's one repeated motif: tinted card + a filled circle badge. */
function card(s, x, y, w, h, fill) {
  s.addShape(pres.ShapeType.roundRect, {
    x, y, w, h, rectRadius: 0.09,
    fill: { color: fill || PAPER },
    line: { color: LINE, width: 0.75 },
  });
}

/** Filled circle carrying a number or short label — the motif that repeats across every slide. */
function badge(s, x, y, d, text, fill, textColor) {
  s.addShape(pres.ShapeType.ellipse, { x, y, w: d, h: d, fill: { color: fill }, line: { color: fill } });
  s.addText(text, {
    x, y, w: d, h: d, fontFace: BODY, fontSize: d >= 0.5 ? 15 : 12, bold: true,
    color: textColor || WHITE, align: "center", valign: "middle", isTextBox: true, margin: 0,
  });
}

/** Big number + caption, for stat callouts. */
function stat(s, x, y, w, value, label, color) {
  s.addText(value, {
    x, y, w, h: 0.72, fontFace: HEAD, fontSize: 40, bold: true,
    color: color || NAVY, align: "left", isTextBox: true, margin: 0,
  });
  s.addText(label, {
    x, y: y + 0.7, w, h: 0.42, fontFace: BODY, fontSize: 12, color: MUTED,
    align: "left", isTextBox: true, margin: 0,
  });
}

function body(s, text, x, y, w, h, opts = {}) {
  s.addText(text, {
    x, y, w, h, fontFace: BODY, fontSize: opts.size || 13,
    color: opts.color || INK, isTextBox: true, margin: 0,
    valign: opts.valign || "top", align: opts.align || "left", bold: opts.bold || false,
    lineSpacingMultiple: opts.lsm || 1.1,
  });
}

/** Small caption, used to mark anything that is an estimate or an illustration. */
function caption(s, text, y, color) {
  s.addText(text, {
    x: M, y, w: W - M * 2, h: 0.3, fontFace: BODY, fontSize: 10, italic: true,
    color: color || MUTED, isTextBox: true, margin: 0,
  });
}

// =============================================================================================
// 1 — Title
// =============================================================================================
{
  const s = darkSlide();
  s.addShape(pres.ShapeType.ellipse, { x: 7.9, y: -1.5, w: 4.2, h: 4.2, fill: { color: "0A2050" }, line: { color: "0A2050" } });
  s.addShape(pres.ShapeType.ellipse, { x: 8.9, y: 3.4, w: 2.6, h: 2.6, fill: { color: "0A2050" }, line: { color: "0A2050" } });

  s.addText("SitePulse", {
    x: M, y: 1.55, w: 7.2, h: 1.05, fontFace: HEAD, fontSize: 60, bold: true,
    color: WHITE, isTextBox: true, margin: 0,
  });
  s.addText("Workforce Attendance & Manpower Control", {
    x: M, y: 2.62, w: 7.2, h: 0.45, fontFace: BODY, fontSize: 19, color: GOLD,
    isTextBox: true, margin: 0,
  });
  s.addText("GPS-verified attendance for every worker, on every project — captured at the gate and in head office at the same moment.", {
    x: M, y: 3.18, w: 6.9, h: 0.7, fontFace: BODY, fontSize: 13, color: "CADCFC",
    isTextBox: true, margin: 0, lineSpacingMultiple: 1.15,
  });

  badge(s, M, 4.45, 0.42, "K", GOLD, NAVY);
  s.addText("KTC International Contracting", {
    x: 1.02, y: 4.45, w: 4.5, h: 0.42, fontFace: BODY, fontSize: 13, bold: true,
    color: WHITE, valign: "middle", isTextBox: true, margin: 0,
  });
  s.addText("Prepared for Human Resources", {
    x: 5.6, y: 4.45, w: 3.9, h: 0.42, fontFace: BODY, fontSize: 12,
    color: "8FA8CC", align: "right", valign: "middle", isTextBox: true, margin: 0,
  });

  s.addNotes("SitePulse is a workforce attendance system built for KTC. Two apps — Android for site, web for office — sharing one live database. This deck covers what it does, what it proves, and what it costs.");
}

// =============================================================================================
// 2 — The problem
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "Where attendance breaks down", { sub: "The four failures SitePulse was built to remove" });

  const items = [
    ["1", "Paper and Excel", "A day's attendance is collected on paper, typed up later, and consolidated by hand. Mistakes only surface at payroll."],
    ["2", "No proof of presence", "A name written on a sheet does not prove the man was on the project. There is nothing to check the claim against."],
    ["3", "Transfers go stale", "A worker moves from Project A to Project B and keeps counting against the site he left, until head office reassigns him by hand."],
    ["4", "Month-end scramble", "Trade-wise manpower, absentee lists and hours are rebuilt from scratch every month, under time pressure."],
  ];

  let y = 1.5;
  items.forEach(([n, title, text], i) => {
    const x = i % 2 === 0 ? M : 5.15;
    const row = Math.floor(i / 2);
    const yy = y + row * 1.86;
    card(s, x, yy, 4.35, 1.62, i < 2 ? PAPER : PAPER);
    badge(s, x + 0.22, yy + 0.24, 0.44, n, i % 2 === 0 ? BLUEMD : NAVY);
    body(s, title, x + 0.8, yy + 0.26, 3.35, 0.32, { size: 14, bold: true });
    body(s, text, x + 0.8, yy + 0.62, 3.35, 0.9, { size: 11, color: MUTED });
  });

  s.addNotes("These are the four problems HR and site management raised. Every feature that follows exists to close one of them.");
}

// =============================================================================================
// 3 — What SitePulse is
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "Two apps, one live database", { sub: "The same data, whether you are standing at the gate or sitting in head office" });

  card(s, M, 1.5, 4.35, 1.45, BLUESF);
  badge(s, M + 0.24, 1.72, 0.44, "A", BLUEMD);
  body(s, "Android app — for site", M + 0.82, 1.74, 3.3, 0.3, { size: 14, bold: true });
  body(s, "Installed on the timekeeper's or foreman's phone. GPS, badge scanning, works on a normal site phone.", M + 0.82, 2.1, 3.3, 0.75, { size: 11, color: MUTED });

  card(s, 5.15, 1.5, 4.35, 1.45, BLUESF);
  badge(s, 5.39, 1.72, 0.44, "W", NAVY);
  body(s, "Web app — for office", 5.97, 1.74, 3.3, 0.3, { size: 14, bold: true });
  body(s, "Opens in any browser, no install. Dashboards, reports, roster and user management.", 5.97, 2.1, 3.3, 0.75, { size: 11, color: MUTED });

  card(s, M, 3.12, 9, 1.05, PAPER);
  body(s, "Both run on Google Firebase — the same infrastructure Google runs its own products on. Data is replicated and backed up by Google; a lost or broken site phone loses nothing.", M + 0.28, 3.32, 8.44, 0.7, { size: 12 });

  stat(s, M, 4.2, 2.2, "5", "user roles, each with its own access", NAVY);
  stat(s, 3.15, 4.2, 2.4, "2", "apps, one shared database", BLUEMD);
  stat(s, 6.05, 4.2, 3.4, "0", "paper attendance sheets required", GREEN);

  s.addNotes("Nothing here is a prototype. Both apps are built, tested and running against the live Firebase project.");
}

// =============================================================================================
// 4 — How a man is marked present
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "How a worker is marked present", { sub: "Three steps, roughly five seconds per man" });

  const steps = [
    ["1", "Identify", "Scan the QR or barcode on the worker's ID badge, or search him by name or employee number.", BLUEMD],
    ["2", "Verify location", "The phone's GPS is checked against the project's geofence before anything is saved.", NAVY],
    ["3", "Record", "IN or OUT is stored with the time, shift, project, and the name of the person who marked it.", GREEN],
  ];

  steps.forEach(([n, title, text, col], i) => {
    const x = M + i * 3.09;
    card(s, x, 1.55, 2.82, 2.1, PAPER);
    badge(s, x + 0.24, 1.8, 0.5, n, col);
    body(s, title, x + 0.24, 2.45, 2.36, 0.32, { size: 15, bold: true });
    body(s, text, x + 0.24, 2.82, 2.36, 0.75, { size: 11, color: MUTED });
    if (i < 2) {
      s.addText("›", { x: x + 2.82, y: 2.2, w: 0.26, h: 0.5, fontFace: BODY, fontSize: 26, bold: true, color: LINE, align: "center", isTextBox: true, margin: 0 });
    }
  });

  card(s, M, 3.85, 9, 1.18, "FDF3F2");
  badge(s, M + 0.28, 4.12, 0.46, "!", RED);
  body(s, "If the phone is outside the project's geofence, the check-in is refused", M + 0.9, 4.08, 8, 0.3, { size: 13, bold: true, color: RED });
  body(s, "The refused attempt is still recorded — who tried, where they were, and how far off site — so a pattern of attempts is visible rather than lost.", M + 0.9, 4.42, 8, 0.5, { size: 11, color: MUTED });

  s.addNotes("The refusal is the point. A blocked attempt is written to its own record, so nothing is silently discarded.");
}

// =============================================================================================
// 5 — ID badges and scanning
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "ID badges and scanning", { sub: "The worker's employee number, printed as a code the phone can read" });

  const steps = [
    ["1", "Generate", "Every worker's employee number becomes a QR code. Nothing to type in, nothing to look up.", BLUEMD],
    ["2", "Print", "Twelve badges to an A4 page, each with the worker's name and ID underneath, as one printable file.", NAVY],
    ["3", "Scan", "Point the phone at the badge. The worker is identified in under a second, with no chance of a mistyped ID.", GREEN],
  ];

  steps.forEach(([n, title, text, col], i) => {
    const x = M + i * 3.09;
    card(s, x, 1.5, 2.82, 1.95, PAPER);
    badge(s, x + 0.24, 1.74, 0.5, n, col);
    body(s, title, x + 0.24, 2.38, 2.36, 0.32, { size: 15, bold: true });
    body(s, text, x + 0.24, 2.74, 2.36, 0.9, { size: 10.5, color: MUTED });
  });

  card(s, M, 3.62, 4.35, 1.45, BLUESF);
  badge(s, M + 0.24, 3.86, 0.42, "1", BLUEMD);
  body(s, "Existing badges still work", M + 0.78, 3.84, 3.3, 0.3, { size: 13, bold: true });
  body(s, "The scanner reads ordinary barcodes as well as QR, so badges already issued do not have to be replaced.", M + 0.78, 4.16, 3.3, 0.78, { size: 10.5, color: MUTED });

  card(s, 5.15, 3.62, 4.35, 1.45, PAPER);
  badge(s, 5.39, 3.86, 0.42, "2", NAVY);
  body(s, "A badge is never required", 5.93, 3.84, 3.3, 0.3, { size: 13, bold: true });
  body(s, "Lost his badge, or a new man with none yet? Search by name or employee number and mark him as normal.", 5.93, 4.16, 3.3, 0.78, { size: 10.5, color: MUTED });

  s.addNotes("Badges are a speed-up, not a dependency — every check-in that works with a scan also works by search, so a lost badge never stops a man being marked.\n\nThe scanner accepts all common barcode formats, so if KTC already issues ID cards with a barcode, those can be used from day one and no reprint is needed.");
}

// =============================================================================================
// 6 — Proof of location
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "Proof the man was there", { sub: "Location is checked by the system, not asserted by a person" });

  const rows = [
    ["Every project carries its own geofence", "Super Admin sets each project's coordinates and radius. A tight compound and a spread-out site get different limits."],
    ["Distance is recorded, not just pass or fail", "Each record keeps how far from the site centre the phone actually was, so a borderline check-in can be reviewed later."],
    ["Off-roster arrivals are flagged", "If a worker is marked at a project other than the one his roster shows, the record is flagged as a site deviation for review."],
    ["Office staff have their own wider radius", "Set separately per site, so office staff are not held to a gate-level fence while the crew is."],
  ];

  let y = 1.5;
  rows.forEach(([t, d], i) => {
    badge(s, M, y + 0.06, 0.34, String(i + 1), i % 2 === 0 ? BLUEMD : NAVY);
    body(s, t, M + 0.56, y, 8.44, 0.3, { size: 13.5, bold: true });
    body(s, d, M + 0.56, y + 0.32, 8.44, 0.44, { size: 11, color: MUTED });
    y += 0.94;
  });

  s.addNotes("This slide answers the first question HR always asks: how do we know the man was on site? The answer is that the phone had to be inside the fence before the record could be written at all.");
}

// =============================================================================================
// 7 — Six roles
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "Five roles, scoped by project", { sub: "Access is decided by role plus the projects that person is assigned to" });

  const roles = [
    ["Super Admin", "Full access. Creates users, sets roles, configures projects and geofences.", NAVY, "SA"],
    ["Admin", "Manages the workforce, attendance and reports across their assigned projects.", BLUEMD, "A"],
    ["Timekeeper", "Owns their site's roster and attendance. Approves incoming worker transfers.", BLUE, "TK"],
    // Foreman was merged into Supervisor: identical permissions, and the same job at KTC.
    ["Supervisor", "Marks attendance, reads manpower and raises transfers for their projects.", GREEN, "SV"],
    ["Staff", "Office staff. Marks only their own attendance and applies for their own leave.", MUTED, "ST"],
  ];

  roles.forEach(([name, duty, col, initials], i) => {
    const x = M + (i % 3) * 3.09;
    const yy = 1.5 + Math.floor(i / 3) * 1.72;
    card(s, x, yy, 2.82, 1.5, PAPER);
    badge(s, x + 0.24, yy + 0.22, 0.42, initials, col);
    body(s, name, x + 0.76, yy + 0.26, 1.92, 0.3, { size: 13, bold: true });
    body(s, duty, x + 0.24, yy + 0.76, 2.36, 0.62, { size: 10.5, color: MUTED });
  });

  // The vacated sixth slot carries the merge note — it balances the grid and answers the
  // "wasn't there a Foreman?" question before anyone has to ask it.
  {
    const x = M + 2 * 3.09, yy = 1.5 + 1.72;
    card(s, x, yy, 2.82, 1.5, BLUESF);
    body(s, "Foreman", x + 0.24, yy + 0.22, 2.36, 0.28, { size: 13, bold: true, color: NAVY });
    body(s, "Merged into Supervisor — same job at KTC. Logins created as Foreman still work, and now read as Supervisor.",
      x + 0.24, yy + 0.56, 2.36, 0.82, { size: 10, color: MUTED });
  }

  caption(s, "A person cannot see, mark or export data for a project they are not assigned to.", 4.92);

  s.addNotes("Site assignment is the second half of the permission. A timekeeper on Project A cannot open Project B's attendance at all.\n\nThere used to be a sixth role, Foreman, carrying permissions identical to Supervisor. It was merged in, since at KTC it is the same job. Logins created as Foreman before the merge keep working unchanged and now read as Supervisor.");
}

// =============================================================================================
// 8 — Site transfers
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "Worker moves from Project A to B", { sub: "Fixed at the gate by the people who can see him, not by head office" });

  card(s, M, 1.5, 4.35, 1.62, "FDF3F2");
  body(s, "BEFORE", M + 0.26, 1.68, 3.8, 0.26, { size: 10.5, bold: true, color: RED });
  body(s, "The man could be marked present at Project B, but his roster still said Project A. He kept counting against the site he had left until head office reassigned him by hand — often weeks later.", M + 0.26, 2.0, 3.85, 1.0, { size: 11, color: MUTED });

  card(s, 5.15, 1.5, 4.35, 1.62, "E9F2ED");
  body(s, "NOW", 5.41, 1.68, 3.8, 0.26, { size: 10.5, bold: true, color: GREEN });
  body(s, "The foreman who is looking at him raises a transfer. Project B's own timekeeper approves it in one tap and the roster moves across. Head office is not in the loop.", 5.41, 2.0, 3.85, 1.0, { size: 11, color: MUTED });

  const flow = [
    ["Foreman", "raises the transfer", BLUEMD],
    ["Timekeeper", "approves it", NAVY],
    ["Roster", "moves to Project B", GREEN],
  ];
  flow.forEach(([who, what, col], i) => {
    const x = M + i * 3.09;
    card(s, x, 3.38, 2.82, 1.05, PAPER);
    badge(s, x + 0.24, 3.62, 0.42, String(i + 1), col);
    body(s, who, x + 0.78, 3.6, 1.86, 0.28, { size: 13, bold: true });
    body(s, what, x + 0.78, 3.88, 1.86, 0.28, { size: 10.5, color: MUTED });
    if (i < 2) {
      s.addText("›", { x: x + 2.82, y: 3.63, w: 0.26, h: 0.5, fontFace: BODY, fontSize: 24, bold: true, color: LINE, align: "center", isTextBox: true, margin: 0 });
    }
  });

  caption(s, "A foreman can only pull a worker towards a project he is assigned to, and cannot approve his own request.", 4.6);

  s.addNotes("This was the last operational gap. Attendance was never blocked for a transferred worker, but the roster stayed wrong, so the headcount reports were wrong.");
}

// =============================================================================================
// 9 — Reports
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "Month-end in one tap", { sub: "Every report below is generated as a single Excel workbook, ready to send" });

  const sheets = [
    ["Attendance", "One sheet per project — every IN and OUT with time and shift"],
    ["Project Summary", "Headcount and attendance by project, side by side"],
    ["Trade-wise Summary", "Manpower by trade, per project or across all projects"],
    ["Monthly Hours", "Hours worked per worker across the month"],
    ["Daily Manpower", "Present, absent, on leave and late, by project and trade"],
    ["Absent Report", "Who was absent, with approved leave and public holidays excluded"],
  ];

  sheets.forEach(([name, desc], i) => {
    const x = M + (i % 2) * 4.65;
    const yy = 1.5 + Math.floor(i / 2) * 1.05;
    badge(s, x, yy + 0.08, 0.34, String(i + 1), i % 2 === 0 ? BLUEMD : NAVY);
    body(s, name, x + 0.5, yy, 3.85, 0.28, { size: 13, bold: true });
    body(s, desc, x + 0.5, yy + 0.3, 3.85, 0.56, { size: 10.5, color: MUTED });
  });

  card(s, M, 4.72, 9, 0.66, BLUESF);
  body(s, "Reports respect the same access rules — a timekeeper's export contains their projects only, never the whole company.", M + 0.28, 4.88, 8.44, 0.36, { size: 11, bold: true, color: NAVY });

  s.addNotes("The Absent Report is the one HR cares about most: approved leave and public holidays are already excluded, so an absence in that sheet is a real absence.");
}

// =============================================================================================
// 10 — Dashboard
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "The whole workforce on one screen", { sub: "Live figures for any date and any project, without waiting for a report" });

  const tiles = [
    ["Present", GREEN], ["Absent", RED], ["On Leave", BLUE],
    ["Late", AMBER], ["Checked Out", MUTED], ["Blocked", RED],
    ["Total Staff", NAVY], ["KTC", GREEN], ["Supplier", AMBER],
    ["Active Projects", GREEN], ["Total Projects", MUTED],
  ];

  tiles.slice(0, 6).forEach(([label, col], i) => {
    const x = M + i * 1.52;
    card(s, x, 1.52, 1.4, 0.86, PAPER);
    s.addShape(pres.ShapeType.ellipse, { x: x + 0.14, y: 1.68, w: 0.16, h: 0.16, fill: { color: col }, line: { color: col } });
    body(s, label, x + 0.14, 2.02, 1.15, 0.26, { size: 9.5, color: MUTED });
  });
  tiles.slice(6).forEach(([label, col], i) => {
    const x = M + i * 1.52;
    card(s, x, 2.5, 1.4, 0.86, PAPER);
    s.addShape(pres.ShapeType.ellipse, { x: x + 0.14, y: 2.66, w: 0.16, h: 0.16, fill: { color: col }, line: { color: col } });
    body(s, label, x + 0.14, 3.0, 1.15, 0.26, { size: 9.5, color: MUTED });
  });

  caption(s, "Eleven live figures, illustrated above. Values update as attendance is marked.", 3.42);

  card(s, M, 3.92, 4.35, 1.28, BLUESF);
  badge(s, M + 0.24, 4.16, 0.42, "?", NAVY);
  body(s, "Worker Locator", M + 0.78, 4.14, 3.3, 0.28, { size: 13, bold: true });
  body(s, "Type a name or ID and see which project he was on for any chosen date.", M + 0.78, 4.46, 3.3, 0.6, { size: 10.5, color: MUTED });

  card(s, 5.15, 3.92, 4.35, 1.28, "FDF7E8");
  badge(s, 5.39, 4.16, 0.42, "!", AMBER);
  body(s, "Site Deviations", 5.93, 4.14, 3.3, 0.28, { size: 13, bold: true });
  body(s, "Anyone marked at a project other than their roster, listed for review the same day.", 5.93, 4.46, 3.3, 0.6, { size: 10.5, color: MUTED });

  s.addNotes("The date and project scope at the top of the dashboard drives everything below it, including the worker locator.");
}

// =============================================================================================
// 11 — Security (dark)
// =============================================================================================
{
  const s = darkSlide();
  heading(s, "Access is enforced by the database", { color: WHITE, sub: "Hiding a button is a convenience. It is never the security boundary.", subColor: "8FA8CC" });

  const points = [
    ["Rules live on Google's servers", "Even a modified app cannot read another project's attendance — the request is refused before it reaches the data."],
    ["Nobody can promote themselves", "A user cannot write their own role or site list. Only Super Admin can, and every change is stamped with who made it."],
    ["Attendance is never deleted", "Corrections are recorded as edits, so the original entry and the correction both survive for audit."],
  ];

  let y = 1.62;
  points.forEach(([t, d], i) => {
    badge(s, M, y + 0.04, 0.36, String(i + 1), GOLD, NAVY);
    body(s, t, M + 0.58, y, 5.6, 0.3, { size: 14, bold: true, color: WHITE });
    body(s, d, M + 0.58, y + 0.33, 5.6, 0.62, { size: 11, color: "B9C7DE" });
    y += 1.06;
  });

  card(s, 6.62, 1.62, 2.88, 2.62, "0A2050");
  s.addShape(pres.ShapeType.roundRect, {
    x: 6.62, y: 1.62, w: 2.88, h: 2.62, rectRadius: 0.09,
    fill: { color: "0A2050" }, line: { color: "1B3468", width: 1 },
  });
  s.addText("48", {
    x: 6.62, y: 2.0, w: 2.88, h: 1.0, fontFace: HEAD, fontSize: 60, bold: true,
    color: GOLD, align: "center", isTextBox: true, margin: 0,
  });
  s.addText("automated security tests", {
    x: 6.72, y: 2.96, w: 2.68, h: 0.3, fontFace: BODY, fontSize: 12, bold: true,
    color: WHITE, align: "center", isTextBox: true, margin: 0,
  });
  s.addText("Each one checks a permission that must be granted — and one that must be refused. All 48 pass.", {
    x: 6.86, y: 3.3, w: 2.4, h: 0.8, fontFace: BODY, fontSize: 10, color: "B9C7DE",
    align: "center", isTextBox: true, margin: 0, lineSpacingMultiple: 1.1,
  });

  caption(s, "Tests are re-run against a Firebase emulator before every release.", 4.8, "8FA8CC");

  s.addNotes("The 44 tests are the strongest thing in this deck. They are not claims — they run, and they fail loudly if a permission drifts.");
}

// =============================================================================================
// 12 — Scale, with the measured chart
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "Measured for 4,000 workers", { sub: "Report generation timed on the standard build, not estimated" });

  s.addChart(
    pres.ChartType.bar,
    [{
      name: "Seconds",
      labels: ["Whole company,\none day\n4,000 rows", "One project,\nfull month\n10,400 rows"],
      values: [1.9, 8.6],
    }],
    {
      x: M, y: 1.45, w: 5.3, h: 3.1,
      barDir: "col",
      chartColors: [BLUEMD, NAVY],
      varyColors: true,
      showTitle: true,
      title: "Excel report generation (seconds)",
      titleFontFace: BODY, titleFontSize: 12, titleColor: MUTED,
      showValue: true, dataLabelPosition: "outEnd", dataLabelFormatCode: "0.0",
      dataLabelFontFace: BODY, dataLabelFontSize: 11, dataLabelColor: INK,
      showLegend: false,
      catAxisLabelColor: MUTED, catAxisLabelFontSize: 9, catAxisLabelFontFace: BODY,
      valAxisLabelColor: MUTED, valAxisLabelFontSize: 9, valAxisLabelFontFace: BODY,
      valGridLine: { color: LINE, size: 0.75 },
      catGridLine: { style: "none" },
      valAxisMaxVal: 12,
      barGapWidthPct: 90,
    }
  );

  const notes = [
    ["Attendance is written per project, per day", "The system never has to load the whole company at once, so adding projects does not slow the existing ones down."],
    ["Every phone loads only its own projects", "A foreman's phone fetches his site's crew, not 4,000 records."],
    ["Growth is priced per record, not per user", "Adding a site or a supervisor costs nothing extra by itself."],
  ];
  let y = 1.62;
  notes.forEach(([t, d], i) => {
    badge(s, 6.1, y + 0.04, 0.34, String(i + 1), i === 1 ? NAVY : BLUEMD);
    body(s, t, 6.6, y, 2.9, 0.44, { size: 11.5, bold: true });
    body(s, d, 6.6, y + 0.46, 2.9, 0.6, { size: 10, color: MUTED });
    y += 1.12;
  });

  caption(s, "Measured on the release build with an Android-sized memory limit. Both reports completed well inside it.", 5.0);

  s.addNotes("4,000 is the figure HR gave. The daily company-wide report — the one run every day — finishes in under two seconds.");
}

// =============================================================================================
// 13 — Benefits for HR
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "What this changes for HR", { sub: "Six outcomes, in the order HR feels them" });

  const bens = [
    ["Payroll you can defend", "Every paid day traces back to a GPS-verified record naming the person who marked it.", GREEN],
    ["Month-end in minutes", "Trade-wise, absentee and hours reports generate on demand instead of being rebuilt by hand.", BLUEMD],
    ["Accurate cost allocation", "Transfers move the roster the same day, so a worker is charged to the project he actually worked on.", NAVY],
    ["Leave handled in the system", "Staff apply, admin approves, and approved leave is automatically excluded from absence.", BLUE],
    ["Disputes settled by record", "Worker Locator answers 'where was he on the 14th?' in seconds, for any past date.", AMBER],
    ["Nothing lost with a phone", "Data lives in Google's cloud, not on the device. A broken or stolen phone loses no attendance.", MUTED],
  ];

  bens.forEach(([t, d, col], i) => {
    const x = M + (i % 3) * 3.09;
    const yy = 1.5 + Math.floor(i / 3) * 1.72;
    card(s, x, yy, 2.82, 1.5, PAPER);
    badge(s, x + 0.24, yy + 0.22, 0.42, String(i + 1), col);
    body(s, t, x + 0.76, yy + 0.2, 1.92, 0.5, { size: 12, bold: true });
    body(s, d, x + 0.24, yy + 0.78, 2.36, 0.62, { size: 10, color: MUTED });
  });

  s.addNotes("If only one slide is remembered, this is the one.");
}

// =============================================================================================
// 14 — Cost and what's next
// =============================================================================================
{
  const s = lightSlide();
  heading(s, "Running cost and what comes next", { sub: "No licence fees, no per-user charge, no server to buy" });

  card(s, M, 1.5, 4.35, 1.95, "E9F2ED");
  body(s, "COST", M + 0.26, 1.68, 3.8, 0.26, { size: 10.5, bold: true, color: GREEN });
  body(s, "There is no software licence and no per-user fee. The only cost is Google's charge for data stored and read, which scales with how much the system is actually used.", M + 0.26, 2.0, 3.85, 0.85, { size: 11, color: MUTED });
  body(s, "At 4,000 workers this is projected to sit well inside a USD 100 per month budget.", M + 0.26, 2.88, 3.85, 0.42, { size: 11, bold: true, color: GREEN });

  const next = [
    ["Push notifications", "Alerts for transfers, arrivals and leave — needs one server function switched on."],
    ["Photo on check-in", "An optional photo stored with the attendance record."],
    ["Power BI / Excel reporting", "Live connection for HR's own analysis, without touching the app."],
    ["ERP roster sync", "Roster arriving automatically from the ERP instead of an Excel upload."],
  ];

  body(s, "ALREADY PLANNED", 5.15, 1.68, 4.2, 0.26, { size: 10.5, bold: true, color: NAVY });
  let y = 2.02;
  next.forEach(([t, d], i) => {
    badge(s, 5.15, y + 0.03, 0.3, String(i + 1), i % 2 === 0 ? BLUEMD : NAVY);
    body(s, t, 5.6, y, 3.9, 0.26, { size: 11.5, bold: true });
    body(s, d, 5.6, y + 0.28, 3.9, 0.34, { size: 10, color: MUTED });
    y += 0.74;
  });

  card(s, M, 3.68, 4.35, 1.35, PAPER);
  body(s, "Already live today", M + 0.26, 3.86, 3.85, 0.28, { size: 12, bold: true, color: NAVY });
  body(s, "GPS check-in, QR badges, five roles, site scoping, transfers, leave, holidays, dashboards, Excel reports, and full backup and restore.", M + 0.26, 4.18, 3.85, 0.72, { size: 10.5, color: MUTED });

  caption(s, "Cost figure is a projection based on expected usage, not a quoted price.", 5.0);

  s.addNotes("Be straight about the cost line: it is a projection. The point is the shape — no licence, no per-user fee, cost follows usage.");
}

// =============================================================================================
// 15 — Close (dark)
// =============================================================================================
{
  const s = darkSlide();
  s.addShape(pres.ShapeType.ellipse, { x: -1.4, y: 3.2, w: 3.6, h: 3.6, fill: { color: "0A2050" }, line: { color: "0A2050" } });
  s.addShape(pres.ShapeType.ellipse, { x: 8.4, y: -1.2, w: 3.2, h: 3.2, fill: { color: "0A2050" }, line: { color: "0A2050" } });

  s.addText("Built, tested, ready to roll out", {
    x: M, y: 1.28, w: 9.0, h: 1.4, fontFace: HEAD, fontSize: 40, bold: true,
    color: WHITE, isTextBox: true, margin: 0,
  });
  s.addText("Both apps are complete and running against the live database. What remains is a pilot on one project, then a staged rollout across the rest.", {
    x: M, y: 2.76, w: 7.4, h: 0.72, fontFace: BODY, fontSize: 14, color: "CADCFC",
    isTextBox: true, margin: 0, lineSpacingMultiple: 1.15,
  });

  const marks = [["4,000", "workers supported"], ["5", "roles"], ["48", "security tests passing"]];
  marks.forEach(([v, l], i) => {
    const x = M + i * 3.05;
    s.addText(v, { x, y: 3.62, w: 2.8, h: 0.6, fontFace: HEAD, fontSize: 32, bold: true, color: GOLD, isTextBox: true, margin: 0 });
    s.addText(l, { x, y: 4.2, w: 2.8, h: 0.36, fontFace: BODY, fontSize: 11, color: "8FA8CC", isTextBox: true, margin: 0 });
  });

  s.addText("SitePulse  ·  KTC International Contracting", {
    x: M, y: 4.85, w: 9, h: 0.34, fontFace: BODY, fontSize: 11, color: "6B84AB",
    isTextBox: true, margin: 0,
  });

  s.addNotes("Ask for a pilot project and a date. A single site for two weeks proves it in front of real payroll.");
}

pres.writeFile({ fileName: "SitePulse_HR_Presentation.pptx" }).then(f => console.log("wrote", f));
