// Mirrors app/src/main/java/com/ktc/sitepulse/Constants.kt exactly — this web app and the
// Android app must agree on these values, since both write into the same Firestore project.
export const ADMIN_EMAILS = new Set(["admin@ktc-manpower.com"]);
export const OFFICE_STAFF_EMAIL_DOMAIN = "ktcco.net";
export const DEFAULT_GEOFENCE_RADIUS_M = 500;
export const FIRESTORE_BATCH_LIMIT = 450;
export const WORKERS_PAGE_SIZE = 50;

export const COMMON_DESIGNATIONS = [
  "Foreman", "Chargehand", "Site Engineer", "Supervisor", "Safety Officer",
  "Carpenter", "Mason", "Steel Fixer", "Electrician", "Plumber", "Painter",
  "Welder", "Helper", "Driver", "Operator", "Storekeeper",
];

export const LEAVE_TYPES = ["Annual", "Sick", "Unpaid", "Other"];

// Official / estimated UAE MOHRE public holidays, 2026-2027 — matches
// app/src/main/java/com/ktc/sitepulse/domain/UaeHolidaysMohre.kt. Islamic-calendar dates are
// moon-sighting dependent and are tagged "(unconfirmed)" until MOHRE gazettes them.
export const UAE_HOLIDAYS_MOHRE = [
  { date: "2026-01-01", name: "New Year's Day" },
  { date: "2026-02-17", name: "Eid al-Fitr Holiday (unconfirmed)" },
  { date: "2026-02-18", name: "Eid al-Fitr (unconfirmed)" },
  { date: "2026-02-19", name: "Eid al-Fitr Holiday (unconfirmed)" },
  { date: "2026-02-20", name: "Eid al-Fitr Holiday (unconfirmed)" },
  { date: "2026-04-26", name: "Arafat Day (unconfirmed)" },
  { date: "2026-04-27", name: "Eid al-Adha (unconfirmed)" },
  { date: "2026-04-28", name: "Eid al-Adha Holiday (unconfirmed)" },
  { date: "2026-04-29", name: "Eid al-Adha Holiday (unconfirmed)" },
  { date: "2026-05-16", name: "Islamic New Year (unconfirmed)" },
  { date: "2026-07-25", name: "Prophet Muhammad's Birthday (unconfirmed)" },
  { date: "2026-12-01", name: "Commemoration Day" },
  { date: "2026-12-02", name: "UAE National Day" },
  { date: "2026-12-03", name: "UAE National Day Holiday" },
  { date: "2027-01-01", name: "New Year's Day" },
  { date: "2027-02-06", name: "Eid al-Fitr Holiday (unconfirmed)" },
  { date: "2027-02-07", name: "Eid al-Fitr (unconfirmed)" },
  { date: "2027-02-08", name: "Eid al-Fitr Holiday (unconfirmed)" },
  { date: "2027-02-09", name: "Eid al-Fitr Holiday (unconfirmed)" },
  { date: "2027-04-15", name: "Arafat Day (unconfirmed)" },
  { date: "2027-04-16", name: "Eid al-Adha (unconfirmed)" },
  { date: "2027-04-17", name: "Eid al-Adha Holiday (unconfirmed)" },
  { date: "2027-04-18", name: "Eid al-Adha Holiday (unconfirmed)" },
  { date: "2027-05-06", name: "Islamic New Year (unconfirmed)" },
  { date: "2027-07-15", name: "Prophet Muhammad's Birthday (unconfirmed)" },
  { date: "2027-12-01", name: "Commemoration Day" },
  { date: "2027-12-02", name: "UAE National Day" },
  { date: "2027-12-03", name: "UAE National Day Holiday" },
];
