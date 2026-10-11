import Database from "better-sqlite3";
import fs from "fs";
import path from "path";
import { scryptSync, randomBytes } from "crypto";
import { slugify } from "./data";
import { DEMO } from "./env";

export const DATA_DIR = process.env.DATA_DIR || path.join(process.cwd(), "data");
fs.mkdirSync(path.join(DATA_DIR, "uploads"), { recursive: true });

const g = globalThis as unknown as { __db?: Database.Database };

export function hashPassword(pw: string) {
  const salt = randomBytes(16).toString("hex");
  return `${salt}:${scryptSync(pw, salt, 32).toString("hex")}`;
}

function init(db: Database.Database) {
  db.pragma("journal_mode = WAL");
  db.pragma("foreign_keys = ON");
  db.exec(`
CREATE TABLE IF NOT EXISTS users(
  id INTEGER PRIMARY KEY, role TEXT NOT NULL, name TEXT, phone TEXT UNIQUE, email TEXT UNIQUE,
  pass_hash TEXT, lang TEXT DEFAULT 'en', status TEXT DEFAULT 'active', created_at TEXT DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS otps(phone TEXT PRIMARY KEY, code_hash TEXT, expires INTEGER, attempts INTEGER DEFAULT 0, sent_at INTEGER);
CREATE TABLE IF NOT EXISTS profiles(
  user_id INTEGER PRIMARY KEY REFERENCES users(id), collar TEXT DEFAULT 'blue', photo TEXT, nationality TEXT, age INTEGER,
  languages TEXT, trade TEXT, years_exp INTEGER, location TEXT, visa_status TEXT, license_type TEXT, available_from TEXT,
  voice_file TEXT, summary TEXT, education TEXT, skills TEXT, certifications TEXT, expected_salary INTEGER, notice_period TEXT,
  portfolio TEXT, cv_file TEXT, experience TEXT, searchable INTEGER DEFAULT 1);
CREATE TABLE IF NOT EXISTS documents(id INTEGER PRIMARY KEY, user_id INTEGER REFERENCES users(id), kind TEXT, file TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS companies(
  id INTEGER PRIMARY KEY, owner_id INTEGER REFERENCES users(id), kind TEXT DEFAULT 'employer', name TEXT, slug TEXT, description TEXT,
  logo TEXT, city TEXT, website TEXT, trade_license_no TEXT, license_file TEXT, agency_license_no TEXT,
  status TEXT DEFAULT 'pending', verified INTEGER DEFAULT 0, plan TEXT DEFAULT 'free', created_at TEXT DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS jobs(
  id INTEGER PRIMARY KEY, company_id INTEGER REFERENCES companies(id), client_name TEXT, title TEXT, category TEXT, collar TEXT,
  city TEXT, area TEXT, job_type TEXT, salary_min INTEGER, salary_max INTEGER, salary_period TEXT DEFAULT 'month', negotiable INTEGER DEFAULT 0,
  experience INTEGER DEFAULT 0, languages TEXT, visa_provided INTEGER DEFAULT 0, accommodation INTEGER DEFAULT 0, transport INTEGER DEFAULT 0,
  food INTEGER DEFAULT 0, hours TEXT, vacancies INTEGER DEFAULT 1, deadline TEXT, description TEXT, urgent INTEGER DEFAULT 0,
  featured INTEGER DEFAULT 0, status TEXT DEFAULT 'pending', created_at TEXT DEFAULT CURRENT_TIMESTAMP);
CREATE INDEX IF NOT EXISTS jobs_search ON jobs(status, category, city, created_at);
CREATE TABLE IF NOT EXISTS applications(
  id INTEGER PRIMARY KEY, job_id INTEGER REFERENCES jobs(id), user_id INTEGER REFERENCES users(id), status TEXT DEFAULT 'applied',
  cover_letter TEXT, notes TEXT, interview_at TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP, UNIQUE(job_id, user_id));
CREATE TABLE IF NOT EXISTS messages(id INTEGER PRIMARY KEY, application_id INTEGER REFERENCES applications(id), sender_id INTEGER, body TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS reports(id INTEGER PRIMARY KEY, job_id INTEGER REFERENCES jobs(id), reporter_id INTEGER, reason TEXT, status TEXT DEFAULT 'open', created_at TEXT DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS saved_jobs(user_id INTEGER, job_id INTEGER, PRIMARY KEY(user_id, job_id));
CREATE TABLE IF NOT EXISTS contact_requests(id INTEGER PRIMARY KEY, company_id INTEGER, user_id INTEGER, status TEXT DEFAULT 'pending', created_at TEXT DEFAULT CURRENT_TIMESTAMP, UNIQUE(company_id, user_id));
CREATE TABLE IF NOT EXISTS notifications(id INTEGER PRIMARY KEY, user_id INTEGER, body TEXT, link TEXT, read INTEGER DEFAULT 0, created_at TEXT DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS payments(id INTEGER PRIMARY KEY, company_id INTEGER, plan TEXT, amount INTEGER, status TEXT DEFAULT 'pending', created_at TEXT DEFAULT CURRENT_TIMESTAMP);
`);
  if (!db.prepare("SELECT 1 FROM users LIMIT 1").get()) seed(db);
}

function seed(db: Database.Database) {
  const adminEmail = process.env.ADMIN_EMAIL || "admin@shughl.local";
  const adminPw = process.env.ADMIN_PASSWORD || "admin12345";
  db.prepare("INSERT INTO users(role,name,email,pass_hash) VALUES('admin','Admin',?,?)").run(adminEmail, hashPassword(adminPw));
  if (!process.env.ADMIN_PASSWORD) console.warn(`[shughl] Seeded admin ${adminEmail} with the DEV password. Set ADMIN_PASSWORD before deploying.`);
  if (!DEMO) return;

  const u = db.prepare("INSERT INTO users(role,name,email,pass_hash) VALUES('employer','Demo Employer','employer@shughl.local',?)").run(hashPassword("employer12345"));
  const c = db.prepare("INSERT INTO companies(owner_id,name,slug,description,city,trade_license_no,status,verified) VALUES(?,?,?,?,?,?,'approved',1)")
    .run(u.lastInsertRowid, "Gulf Star Services LLC", "gulf-star-services", "Facilities, logistics and hospitality staffing across the UAE since 2009.", "Dubai", "DED-123456");
  const ins = db.prepare(`INSERT INTO jobs(company_id,title,category,collar,city,area,job_type,salary_min,salary_max,salary_period,experience,languages,visa_provided,accommodation,transport,food,hours,vacancies,description,urgent,status)
    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, 'active')`);
  const demo: any[][] = [
    ["Light Vehicle Driver", "driver", "blue", "Dubai", "Deira", "full-time", 2200, 2800, "month", 2, "English,Hindi,Urdu", 1, 1, 1, 0, "9 hrs, 6 days", 10, "Valid UAE licence required. Deliver staff and light cargo within Dubai.", 1],
    ["Construction Labourer", "construction", "blue", "Abu Dhabi", "Mussafah", "full-time", 1200, 1500, "month", 0, "Hindi,Urdu,Bengali,Nepali", 1, 1, 1, 1, "8 hrs, 6 days", 50, "Bulk hiring for a tower project. Safety training provided.", 1],
    ["Office Cleaner", "cleaning", "blue", "Sharjah", "Al Majaz", "full-time", 1100, 1400, "month", 1, "English,Tagalog,Hindi", 1, 1, 0, 1, "8 hrs", 6, "Clean offices and common areas. Uniform provided.", 0],
    ["Security Guard", "security", "blue", "Dubai", "Business Bay", "full-time", 1800, 2200, "month", 1, "English,Hindi,Urdu", 1, 1, 1, 0, "12 hrs shifts", 8, "SIRA card preferred. Night and day shifts.", 0],
    ["Food Delivery Rider", "delivery", "blue", "Dubai", "Marina", "daily wage", 180, 250, "day", 0, "English,Arabic,Hindi", 0, 0, 0, 1, "Flexible", 30, "Own motorcycle licence needed. Bike provided.", 1],
    ["Electrician", "technician", "blue", "Ajman", "Industrial Area", "contract", 2500, 3200, "month", 3, "English,Hindi,Malayalam", 1, 1, 1, 0, "8 hrs", 4, "MEP maintenance electrician with trade certificate.", 0],
    ["Accountant", "finance", "white", "Dubai", "JLT", "full-time", 6000, 9000, "month", 3, "English,Arabic", 1, 0, 0, 0, "9-6, Mon-Fri", 1, "AP/AR, VAT returns, month-end close. Tally or Zoho Books.", 0],
    ["Full-Stack Developer", "it", "white", "Abu Dhabi", "Al Reem", "full-time", 12000, 18000, "month", 4, "English", 1, 0, 0, 0, "Hybrid", 2, "React + Node.js. Build internal logistics tools.", 0],
  ];
  for (const d of demo) ins.run(c.lastInsertRowid, ...d);
}

export function db(): Database.Database {
  if (!g.__db) { g.__db = new Database(path.join(DATA_DIR, "shughl.db")); init(g.__db); }
  return g.__db;
}

export const jobSlug = (j: { id: number; title: string; city: string }) => `${slugify(j.title)}-${slugify(j.city)}-${j.id}`;
export const jobIdFromSlug = (s: string) => { const m = s.match(/-(\d+)$/); return m ? Number(m[1]) : NaN; };
