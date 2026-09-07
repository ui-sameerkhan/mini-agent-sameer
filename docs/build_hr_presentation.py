"""Builds the SitePulse presentation PDF for KTC HR."""
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate, Frame, KeepTogether, PageBreak, PageTemplate, Paragraph,
    Spacer, Table, TableStyle,
)

OUT = "/home/user/mini-agent-sameer/docs/SitePulse_HR_Presentation.pdf"

# Brand palette, sampled from the app's own theme tokens (ui/theme/Color.kt).
NAVY = colors.HexColor("#00113D")
BLUE = colors.HexColor("#0B66D6")
BLUE_SOFT = colors.HexColor("#E8F1FC")
GOLD = colors.HexColor("#F9CB14")
GREEN = colors.HexColor("#17875E")
GREEN_SOFT = colors.HexColor("#E9F2ED")
RED = colors.HexColor("#B3261E")
AMBER = colors.HexColor("#B8842E")
AMBER_SOFT = colors.HexColor("#F6EEDD")
INK = colors.HexColor("#141F1A")
MUTED = colors.HexColor("#5B6B63")
LINE = colors.HexColor("#DEE6E1")
PAPER = colors.HexColor("#F5F7F5")

styles = getSampleStyleSheet()


def S(name, **kw):
    return ParagraphStyle(name, parent=styles["Normal"], **kw)


TITLE = S("t", fontName="Helvetica-Bold", fontSize=30, leading=34, textColor=NAVY, spaceAfter=2)
SUBTITLE = S("st", fontName="Helvetica", fontSize=13, leading=18, textColor=MUTED)
H1 = S("h1", fontName="Helvetica-Bold", fontSize=17, leading=21, textColor=NAVY, spaceBefore=2, spaceAfter=7)
H2 = S("h2", fontName="Helvetica-Bold", fontSize=11.5, leading=15, textColor=BLUE, spaceBefore=10, spaceAfter=4)
BODY = S("b", fontSize=10, leading=15, textColor=INK, spaceAfter=6)
BODY_M = S("bm", fontSize=9.5, leading=14, textColor=MUTED, spaceAfter=5)
BULLET = S("bu", fontSize=10, leading=14.5, textColor=INK, leftIndent=11, bulletIndent=2, spaceAfter=3.5)
CELL = S("c", fontSize=9, leading=12.5, textColor=INK)
CELL_B = S("cb", fontName="Helvetica-Bold", fontSize=9, leading=12.5, textColor=NAVY)
CELL_S = S("cs", fontSize=8.5, leading=11.5, textColor=MUTED)
QUOTE = S("q", fontSize=11, leading=16, textColor=NAVY, fontName="Helvetica-Oblique",
          leftIndent=10, rightIndent=10, spaceBefore=4, spaceAfter=4)
COVER_META = S("cm", fontSize=10, leading=15, textColor=MUTED, alignment=TA_CENTER)


def bullets(items, style=BULLET):
    return [Paragraph(t, style, bulletText="•") for t in items]


def panel(rows, bg, border, pad=9):
    """A single-cell tinted callout box."""
    t = Table([[rows]], colWidths=[168 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), bg),
        ("BOX", (0, 0), (-1, -1), 0.8, border),
        ("LEFTPADDING", (0, 0), (-1, -1), pad),
        ("RIGHTPADDING", (0, 0), (-1, -1), pad),
        ("TOPPADDING", (0, 0), (-1, -1), pad),
        ("BOTTOMPADDING", (0, 0), (-1, -1), pad),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ]))
    return t


def datatable(header, rows, widths, header_bg=NAVY):
    data = [[Paragraph(h, S("th", fontName="Helvetica-Bold", fontSize=8.5,
                            leading=11, textColor=colors.white)) for h in header]]
    for r in rows:
        data.append([c if hasattr(c, "wrap") else Paragraph(str(c), CELL) for c in r])
    t = Table(data, colWidths=widths, repeatRows=1)
    style = [
        ("BACKGROUND", (0, 0), (-1, 0), header_bg),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.4, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    for i in range(1, len(data)):
        if i % 2 == 0:
            style.append(("BACKGROUND", (0, i), (-1, i), PAPER))
    t.setStyle(TableStyle(style))
    return t


# ---------------------------------------------------------------- page frame
def on_page(canvas, doc):
    canvas.saveState()
    w, h = A4
    if doc.page > 1:
        canvas.setFillColor(NAVY)
        canvas.rect(0, h - 16 * mm, w, 16 * mm, fill=1, stroke=0)
        canvas.setFillColor(GOLD)
        canvas.rect(0, h - 16.9 * mm, w, 0.9 * mm, fill=1, stroke=0)
        canvas.setFont("Helvetica-Bold", 10)
        canvas.setFillColor(colors.white)
        canvas.drawString(21 * mm, h - 10.6 * mm, "SitePulse")
        canvas.setFont("Helvetica", 8.5)
        canvas.setFillColor(colors.HexColor("#AFC4E8"))
        canvas.drawString(43 * mm, h - 10.6 * mm, "KTC International Contracting")
        canvas.setFont("Helvetica", 8)
        canvas.drawRightString(w - 21 * mm, h - 10.6 * mm, "Workforce Attendance System")

        canvas.setStrokeColor(LINE)
        canvas.setLineWidth(0.5)
        canvas.line(21 * mm, 14 * mm, w - 21 * mm, 14 * mm)
        canvas.setFont("Helvetica", 7.5)
        canvas.setFillColor(MUTED)
        canvas.drawString(21 * mm, 9.5 * mm, "Internal document — prepared for HR review")
        canvas.setFont("Helvetica-Bold", 7.5)
        canvas.drawRightString(w - 21 * mm, 9.5 * mm, f"Page {doc.page - 1}")
    canvas.restoreState()


doc = BaseDocTemplate(
    OUT, pagesize=A4,
    leftMargin=21 * mm, rightMargin=21 * mm, topMargin=24 * mm, bottomMargin=19 * mm,
    title="SitePulse — Workforce Attendance System",
    author="Sameer Khan", subject="Workforce attendance and manpower tracking system for KTC International Contracting",
)
frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="f")
doc.addPageTemplates([PageTemplate(id="main", frames=[frame], onPage=on_page)])

s = []

# ============================================================ COVER
s.append(Spacer(1, 18 * mm))
cover = Table([[Paragraph("SitePulse", S("ct", fontName="Helvetica-Bold", fontSize=44,
                                         leading=48, textColor=colors.white))],
               [Paragraph("Workforce Attendance &amp; Manpower Tracking",
                          S("cs2", fontSize=14, leading=19, textColor=GOLD))],
               [Paragraph("KTC International Contracting",
                          S("cs3", fontSize=11, leading=16,
                            textColor=colors.HexColor("#AFC4E8")))]],
              colWidths=[168 * mm])
cover.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (-1, -1), NAVY),
    ("LEFTPADDING", (0, 0), (-1, -1), 20), ("RIGHTPADDING", (0, 0), (-1, -1), 20),
    ("TOPPADDING", (0, 0), (0, 0), 26), ("BOTTOMPADDING", (0, 0), (0, 0), 2),
    ("TOPPADDING", (0, 1), (0, 1), 0), ("BOTTOMPADDING", (0, 1), (0, 1), 3),
    ("TOPPADDING", (0, 2), (0, 2), 0), ("BOTTOMPADDING", (0, 2), (0, 2), 26),
]))
s.append(cover)
s.append(Spacer(1, 12 * mm))
s.append(Paragraph(
    "GPS-verified site attendance, real-time manpower visibility,<br/>and automated payroll-ready reporting.",
    S("tag", fontSize=12.5, leading=19, textColor=INK, alignment=TA_CENTER)))
s.append(Spacer(1, 16 * mm))

HL = S("hl", fontName="Helvetica-Bold", fontSize=10.5, leading=14, textColor=NAVY,
       alignment=TA_CENTER, spaceAfter=2)
HLB = S("hlb", fontSize=9, leading=12.5, textColor=MUTED, alignment=TA_CENTER)

highlights = Table([[
    [Paragraph("Verified", HL), Paragraph("Attendance proven by GPS at the site, not written on paper", HLB)],
    [Paragraph("Visible", HL), Paragraph("Live manpower by trade, project and supplier", HLB)],
    [Paragraph("Automatic", HL), Paragraph("Payroll-ready Excel reports generated from the records", HLB)],
]], colWidths=[56 * mm] * 3)
highlights.setStyle(TableStyle([
    ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ("LEFTPADDING", (0, 0), (-1, -1), 8), ("RIGHTPADDING", (0, 0), (-1, -1), 8),
    ("TOPPADDING", (0, 0), (-1, -1), 12), ("BOTTOMPADDING", (0, 0), (-1, -1), 12),
    ("LINEBEFORE", (1, 0), (2, 0), 0.6, LINE),
    ("BACKGROUND", (0, 0), (-1, -1), PAPER),
    ("BOX", (0, 0), (-1, -1), 0.6, LINE),
]))
s.append(highlights)
s.append(Spacer(1, 10 * mm))

s.append(panel(Paragraph(
    "<b>Status:</b> Working system, ready for a supervised pilot on one project.<br/>"
    "<b>Platforms:</b> Native Android app and browser-based web app, sharing one live database.<br/>"
    "<b>Prepared by:</b> Sameer Khan", COVER_META), BLUE_SOFT, BLUE, pad=12))
s.append(PageBreak())

# ============================================================ 1. THE PROBLEM
s.append(Paragraph("1. The problem we are solving", H1))
s.append(Paragraph(
    "Daily manpower attendance across our projects is currently recorded by hand and relayed "
    "informally. That creates four costs the company absorbs every single month:", BODY))
s.extend(bullets([
    "<b>Attendance cannot be verified.</b> A name written on a sheet is not evidence that the "
    "person was on that site, on that day, at that time.",
    "<b>Manpower is not visible until it is too late.</b> Knowing today's headcount by trade and "
    "by project usually means phoning site supervisors one by one.",
    "<b>Monthly reporting is manual.</b> Attendance sheets are re-typed into spreadsheets, which "
    "is slow and introduces errors into payroll inputs.",
    "<b>There is no audit trail.</b> When a figure is disputed weeks later, there is little to "
    "check it against.",
]))
s.append(Spacer(1, 6))
s.append(panel(Paragraph(
    "SitePulse replaces the paper step with a GPS-verified check-in taken on a phone at the site "
    "itself — then produces the manpower report automatically from those records.", QUOTE),
    GREEN_SOFT, GREEN))

s.append(Paragraph("2. What the system does", H1))
s.append(Paragraph(
    "A supervisor or timekeeper opens the app at site and checks a worker in — by scanning the QR "
    "code on their ID badge, or by entering their Employee ID. The app captures the worker's "
    "location at that moment and confirms they are physically inside the project boundary before "
    "accepting the record.", BODY))
s.append(Paragraph(
    "From those verified records, everything else follows automatically: the live dashboard, the "
    "trade-wise and project-wise manpower counts, the absent report, and the monthly Excel export.",
    BODY))

s.append(Paragraph("The check-in, step by step", H2))
s.append(datatable(
    ["", "Step", "What the system does"],
    [[Paragraph("<b>1</b>", CELL_B), Paragraph("<b>Identify the worker</b>", CELL),
      "Scan the QR code on their ID badge, or type the Employee ID. Names appear as you type."],
     [Paragraph("<b>2</b>", CELL_B), Paragraph("<b>Verify the location</b>", CELL),
      "GPS position is read and measured against the project's registered coordinates and its "
      "allowed radius."],
     [Paragraph("<b>3</b>", CELL_B), Paragraph("<b>Accept or refuse</b>", CELL),
      "Inside the boundary, the check-in is recorded. Outside it, the check-in is refused and the "
      "attempt is logged for review with the distance measured."],
     [Paragraph("<b>4</b>", CELL_B), Paragraph("<b>Record the detail</b>", CELL),
      "Time, GPS coordinates, distance from site centre, day or night shift, and who marked it are "
      "all stored against the record."],
     [Paragraph("<b>5</b>", CELL_B), Paragraph("<b>Check out</b>", CELL),
      "The same process at the end of the shift. Hours worked are calculated automatically, "
      "including night shifts that run past midnight."]],
    [10 * mm, 40 * mm, 118 * mm]))
s.append(PageBreak())

# ============================================================ 3. CONTROLS
s.append(Paragraph("3. Controls that protect the attendance record", H1))
s.append(Paragraph(
    "These are the safeguards that make the record trustworthy enough to use as a payroll input.",
    BODY))
s.append(datatable(
    ["Control", "What it prevents"],
    [[Paragraph("<b>Geofence per project</b>", CELL),
      "Checking in from anywhere other than the site. Each project has its own coordinates and "
      "allowed radius, set by the administrator."],
     [Paragraph("<b>Fake-GPS detection</b>", CELL),
      "Defeating the geofence with a location-spoofing app. Android reports when a position came "
      "from a mock provider; SitePulse refuses those check-ins outright."],
     [Paragraph("<b>Roster deviation flag</b>", CELL),
      "A worker being marked at a project they are not assigned to. The check-in is still "
      "recorded, but flagged for the administrator to review."],
     [Paragraph("<b>Blocked-attempt log</b>", CELL),
      "Repeated attempts going unnoticed. Every refused check-in is logged with the worker, the "
      "nearest project and the distance from it."],
     [Paragraph("<b>Duplicate protection</b>", CELL),
      "The same worker being counted twice on the same day. One attendance record exists per "
      "worker per date."],
     [Paragraph("<b>Correction audit trail</b>", CELL),
      "Silent edits. An administrator can correct a missed check-in, but the record is permanently "
      "marked as corrected, by whom, and when."],
     [Paragraph("<b>Server-enforced access</b>", CELL),
      "A user reaching data their role does not permit. Permissions are enforced on the server, "
      "not merely hidden in the app."]],
    [42 * mm, 126 * mm]))

s.append(Paragraph("4. Who uses it, and what they can see", H1))
s.append(Paragraph(
    "Four roles, each limited to what that job actually requires.", BODY))
s.append(datatable(
    ["Role", "Purpose", "Access"],
    [[Paragraph("<b>Administrator</b>", CELL), "HR / management oversight",
      "Full access — employees, projects, attendance, leave, reports, backups, and user management."],
     [Paragraph("<b>Timekeeper</b>", CELL), "Daily attendance duty",
      "Attendance records, corrections and reports. Cannot alter employees or project settings."],
     [Paragraph("<b>Supervisor</b>", CELL), "Site-level use",
      "Checks workers in and out, and submits new-arrival requests for approval."],
     [Paragraph("<b>Office Staff</b>", CELL), "Self-service",
      "Marks their own attendance, applies for leave, and views their own history only."]],
    [30 * mm, 40 * mm, 98 * mm]))
s.append(Spacer(1, 5))
s.append(panel(Paragraph(
    "<b>Timekeeper accounts are created inside the app.</b> The administrator can issue a login "
    "and password for a new timekeeper directly from the admin panel, and withdraw that access at "
    "any time — no technical assistance required.", CELL), BLUE_SOFT, BLUE))
s.append(PageBreak())

# ============================================================ 5. MANAGEMENT
s.append(Paragraph("5. What management sees", H1))
s.append(Paragraph("The daily dashboard", H2))
s.append(Paragraph(
    "One screen answers the question the company asks every morning — how many people do we have, "
    "where, and doing what:", BODY))
s.extend(bullets([
    "Present, absent, on leave, late arrivals, and checked out — as live counts",
    "KTC's own manpower shown separately from supplier and subcontractor manpower",
    "Active projects against total projects",
    "<b>Trade-wise manpower</b> — every trade listed separately, with present, absent and leave "
    "against each",
    "<b>Project-wise manpower</b> — the same breakdown for each project",
    "<b>Supplier-wise manpower</b> — the same breakdown for each supplier",
    "<b>Any past date, any single project</b> — the same figures for any day already recorded",
    "<b>Worker locator</b> — search any Employee ID on any date to see where and when they checked in",
]))
s.append(Spacer(1, 4))
s.append(panel(Paragraph(
    "Trades are kept strictly separate. Assistant Carpenter is never merged into Carpenter, and "
    "Watchmen are counted on their own — the categories match how manpower is actually planned "
    "and costed.", CELL), AMBER_SOFT, AMBER))

s.append(Paragraph("Reporting", H2))
s.append(Paragraph(
    "Reports are generated as formatted Excel workbooks and can be shared straight from the phone. "
    "A single export can contain:", BODY))
s.append(datatable(
    ["Sheet", "Contents"],
    [[Paragraph("<b>Daily Manpower Report</b>", CELL),
      "Total, present, absent and on-leave — broken down by trade, by project and by supplier."],
     [Paragraph("<b>Attendance detail</b>", CELL),
      "Every check-in and check-out with times, hours worked, GPS coordinates, distance from site, "
      "shift, and who marked it. One sheet per project."],
     [Paragraph("<b>Project summary</b>", CELL), "Headcount checked in and out, per project."],
     [Paragraph("<b>Trade summary</b>", CELL), "Manpower present by trade."],
     [Paragraph("<b>Monthly hours</b>", CELL),
      "Days present, total hours and average hours per day, per worker — for monthly runs."],
     [Paragraph("<b>Absent report</b>", CELL),
      "Who was absent, accounting for approved leave and public holidays so neither is counted as "
      "absence."]],
    [42 * mm, 126 * mm]))
s.append(PageBreak())

# ============================================================ 6. HR FUNCTIONS
s.append(Paragraph("6. Functions that matter to HR", H1))
s.append(Paragraph("Employee records", H2))
s.extend(bullets([
    "Employee ID, name, trade, company or supplier, assigned project, and status",
    "Bulk import from Excel — existing records do not need re-entering",
    "Search by ID, name, trade, supplier or project; filter and sort by any of them",
    "Duplicate Employee IDs are rejected before they can overwrite an existing record",
    "Printable QR ID badges, individually or as one PDF for the whole workforce",
]))

s.append(Paragraph("Leave management", H2))
s.extend(bullets([
    "Annual, Sick, Unpaid and Other leave types",
    "Office staff apply through the app; the administrator approves or rejects",
    "Annual leave balance tracked per employee against their entitlement",
    "Whole crews can be placed on leave at once — for a rain day or a site shutdown",
    "Approved leave automatically excludes a worker from the absent report",
]))

s.append(Paragraph("UAE public holidays", H2))
s.append(Paragraph(
    "The MOHRE public holiday calendar is built in. On a listed holiday a worker with no "
    "attendance is reported as <b>Holiday</b>, not as absent, and holidays are excluded from "
    "attendance-percentage calculations. Dates that depend on moon sighting are marked "
    "unconfirmed so they can be checked against the current MOHRE circular.", BODY))

s.append(Paragraph("Working offline", H2))
s.append(Paragraph(
    "Site connectivity is unreliable, so a check-in taken without signal is stored on the phone "
    "and synced automatically when signal returns. The person marking attendance is told clearly "
    "that the record is held offline and not yet synced.", BODY))

s.append(Paragraph("Data protection", H2))
s.extend(bullets([
    "All data is held in Google Firebase, access-controlled per role and enforced on the server",
    "A complete backup of every record can be exported to Excel at any time",
    "A backup can be restored without duplicating existing records",
    "Attendance corrections are permanently attributed to the person who made them",
]))
s.append(PageBreak())

# ============================================================ 7. STATUS
s.append(Paragraph("7. Current status — an honest assessment", H1))
s.append(Paragraph(
    "Everything listed below as <b>Live</b> is built and working today. The remaining items are "
    "stated plainly so this can be judged accurately.", BODY))
s.append(datatable(
    ["Capability", "Status"],
    [["GPS-verified check-in and check-out with geofencing",
      Paragraph("<b>Live</b>", S("ok", fontName="Helvetica-Bold", fontSize=9, textColor=GREEN))],
     ["QR ID badge scanning and badge printing",
      Paragraph("<b>Live</b>", S("ok2", fontName="Helvetica-Bold", fontSize=9, textColor=GREEN))],
     ["Fake-GPS detection and blocked-attempt logging",
      Paragraph("<b>Live</b>", S("ok3", fontName="Helvetica-Bold", fontSize=9, textColor=GREEN))],
     ["Dashboard, manpower breakdowns and historical view",
      Paragraph("<b>Live</b>", S("ok4", fontName="Helvetica-Bold", fontSize=9, textColor=GREEN))],
     ["Employee management and Excel import",
      Paragraph("<b>Live</b>", S("ok5", fontName="Helvetica-Bold", fontSize=9, textColor=GREEN))],
     ["Leave management and UAE holiday calendar",
      Paragraph("<b>Live</b>", S("ok6", fontName="Helvetica-Bold", fontSize=9, textColor=GREEN))],
     ["Excel reporting and manual backup",
      Paragraph("<b>Live</b>", S("ok7", fontName="Helvetica-Bold", fontSize=9, textColor=GREEN))],
     ["Role-based access for four roles",
      Paragraph("<b>Live</b>", S("ok8", fontName="Helvetica-Bold", fontSize=9, textColor=GREEN))],
     ["Offline check-in with automatic sync",
      Paragraph("<b>Live</b>", S("ok9", fontName="Helvetica-Bold", fontSize=9, textColor=GREEN))],
     ["Announcements to all users (in-app)",
      Paragraph("<b>Live</b>", S("ok10", fontName="Helvetica-Bold", fontSize=9, textColor=GREEN))],
     ["Push notifications to phones",
      Paragraph("<b>Built — pending final deployment</b>",
                S("pend", fontName="Helvetica-Bold", fontSize=9, textColor=AMBER))],
     ["Automated weekly backup",
      Paragraph("<b>Built — pending final deployment</b>",
                S("pend2", fontName="Helvetica-Bold", fontSize=9, textColor=AMBER))],
     ["Distribution via Google Play Store",
      Paragraph("<b>Not started</b>", S("no", fontName="Helvetica-Bold", fontSize=9, textColor=MUTED))]],
    [118 * mm, 50 * mm]))

s.append(Spacer(1, 7))
s.append(panel(Paragraph(
    "<b>What has not been done yet:</b> the system has not been trialled with our real workforce "
    "on a live site. It has been built and checked, but not proven in daily use. That is exactly "
    "what the pilot below is for, and it is the reason this is presented as a pilot rather than a "
    "finished rollout.", CELL), AMBER_SOFT, AMBER))

s.append(PageBreak())
s.append(Paragraph("8. Proposed next step — a one-project pilot", H1))
s.append(Paragraph(
    "Rather than a company-wide switch, we propose proving the system on a single project first.",
    BODY))
s.append(datatable(
    ["#", "Activity", "Duration"],
    [[Paragraph("<b>1</b>", CELL_B), "Select one project. Load its employees, register the site "
                                     "coordinates and radius, and print QR ID badges.", "2–3 days"],
     [Paragraph("<b>2</b>", CELL_B), "Brief the supervisor and timekeeper. Install the app on "
                                     "their phones.", "1 day"],
     [Paragraph("<b>3</b>", CELL_B), "Run SitePulse alongside the existing paper method and "
                                     "compare the two daily.", "2 weeks"],
     [Paragraph("<b>4</b>", CELL_B), "Review together: accuracy against paper, time saved, and "
                                     "issues raised by site staff.", "1 day"],
     [Paragraph("<b>5</b>", CELL_B), "Decide whether to extend to further projects.", "—"]],
    [12 * mm, 126 * mm, 30 * mm]))
s.append(Spacer(1, 6))
s.append(Paragraph(
    "Running both methods in parallel means the pilot carries no risk to payroll: the paper record "
    "remains the official one until the system has demonstrated it matches.", BODY))

s.append(Spacer(1, 8))
s.append(panel(Paragraph(
    "<b>What we would need from HR</b><br/><br/>"
    "1.&nbsp;&nbsp;One project nominated for the pilot<br/>"
    "2.&nbsp;&nbsp;The employee list for that project<br/>"
    "3.&nbsp;&nbsp;Agreement from its supervisor and timekeeper to take part<br/>"
    "4.&nbsp;&nbsp;A review meeting at the end of the two weeks",
    CELL), BLUE_SOFT, BLUE))

s.append(Paragraph("9. Why this is worth trialling", H1))
s.append(datatable(
    ["Today", "With SitePulse"],
    [["Attendance is written on paper and cannot be verified afterwards.",
      "Every check-in carries GPS coordinates, distance from site and a timestamp."],
     ["Headcount by trade means phoning supervisors one by one.",
      "Live on one screen — by trade, by project, by supplier."],
     ["Monthly reports are re-typed from sheets into spreadsheets.",
      "Generated as a formatted Excel workbook in seconds."],
     ["A disputed figure weeks later has little to check against.",
      "Full record for any past date, with every correction attributed."],
     ["Nothing stops a worker being marked present while off site.",
      "Check-ins outside the boundary are refused and logged."]],
    [82 * mm, 86 * mm]))

s.append(Spacer(1, 12))
s.append(panel(Paragraph(
    "The system is built and working. What it has not yet had is two weeks of real use on a real "
    "site — which is the only thing that will show whether it holds up. That is what we are "
    "asking for.", QUOTE), GREEN_SOFT, GREEN))

s.append(Spacer(1, 14))
s.append(Paragraph(
    "Prepared by Sameer Khan · SitePulse for KTC International Contracting",
    S("foot", fontSize=8.5, textColor=MUTED, alignment=TA_CENTER)))

doc.build(s)
print("built:", OUT)
