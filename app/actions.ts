"use server";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";
import { randomInt } from "crypto";
import { db, hashPassword, jobSlug } from "@/lib/db";
import { currentUser, endSession, hashCode, myCompany, requireUser, sameHash, startSession, verifyPassword } from "@/lib/auth";
import { saveUpload } from "@/lib/files";
import { looksLikeScam, SCAM_MESSAGE } from "@/lib/safety";
import { notify, sendSms, smsConfigured } from "@/lib/notify";
import { DEMO } from "@/lib/env";
import { APP_STATUSES, CATEGORIES, DOC_KINDS, JOB_TYPES, LANGUAGES, slugify } from "@/lib/data";

export type State = { error?: string; ok?: string; sent?: boolean; phone?: string; devCode?: string } | undefined;
const s = (fd: FormData, k: string) => String(fd.get(k) ?? "").trim();
const n = (fd: FormData, k: string) => { const v = s(fd, k); return v === "" || isNaN(Number(v)) ? null : Math.max(0, Math.round(Number(v))); };
const flag = (fd: FormData, k: string) => (fd.get(k) ? 1 : 0);
const emailOk = (e: string) => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(e);

export async function normalizePhone(raw: string) {
  let p = raw.replace(/[\s()-]/g, "");
  if (p.startsWith("00")) p = "+" + p.slice(2);
  else if (/^0\d{9}$/.test(p)) p = "+971" + p.slice(1);
  else if (!p.startsWith("+")) p = "+" + p;
  return /^\+\d{8,15}$/.test(p) ? p : null;
}

export async function setLang(fd: FormData) {
  const code = s(fd, "lang");
  if (LANGUAGES.some((l) => l.code === code)) (await cookies()).set("lang", code, { path: "/", maxAge: 31536000, sameSite: "lax" });
  const u = await currentUser(); if (u) db().prepare("UPDATE users SET lang=? WHERE id=?").run(code, u.id);
  revalidatePath("/", "layout");
}

// ---------- Auth ----------
export async function requestOtp(_: State, fd: FormData): Promise<State> {
  const phone = await normalizePhone(s(fd, "phone"));
  if (!phone) return { error: "Enter a valid phone number, e.g. 050 123 4567 or +91 98765 43210." };
  const row = db().prepare("SELECT sent_at FROM otps WHERE phone=?").get(phone) as { sent_at: number } | undefined;
  if (row && Date.now() - row.sent_at < 60_000) return { error: "Please wait a minute before asking for another code.", phone, sent: true };
  const dev = !smsConfigured() && DEMO;
  if (!smsConfigured() && !dev) return { error: "SMS is not configured on this server." };
  const code = String(randomInt(100000, 1000000));
  db().prepare("INSERT OR REPLACE INTO otps(phone,code_hash,expires,attempts,sent_at) VALUES(?,?,?,0,?)").run(phone, hashCode(phone, code), Date.now() + 5 * 60_000, Date.now());
  await sendSms(phone, `Your Shughl code is ${code}. Never share it. Jobs are always free.`);
  return { sent: true, phone, devCode: dev ? code : undefined };
}

export async function verifyOtp(_: State, fd: FormData): Promise<State> {
  const phone = s(fd, "phone"), code = s(fd, "code");
  const row = db().prepare("SELECT * FROM otps WHERE phone=?").get(phone) as { code_hash: string; expires: number; attempts: number } | undefined;
  if (!row || row.expires < Date.now()) return { error: "Code expired. Ask for a new one.", sent: false };
  if (row.attempts >= 5) return { error: "Too many tries. Ask for a new code.", sent: false };
  db().prepare("UPDATE otps SET attempts=attempts+1 WHERE phone=?").run(phone);
  if (!sameHash(row.code_hash, hashCode(phone, code))) return { error: "Wrong code. Try again.", sent: true, phone };
  db().prepare("DELETE FROM otps WHERE phone=?").run(phone);
  let u = db().prepare("SELECT id,role,status FROM users WHERE phone=?").get(phone) as { id: number; role: string; status: string } | undefined;
  if (u && u.status !== "active") return { error: "This account is suspended." };
  if (u && u.role !== "jobseeker") return { error: "This number belongs to an employer account. Log in with email." };
  const isNew = !u;
  if (!u) {
    const lang = (await cookies()).get("lang")?.value || "en";
    const r = db().prepare("INSERT INTO users(role,phone,lang) VALUES('jobseeker',?,?)").run(phone, lang);
    db().prepare("INSERT INTO profiles(user_id,collar) VALUES(?,'blue')").run(r.lastInsertRowid);
    u = { id: Number(r.lastInsertRowid), role: "jobseeker", status: "active" };
  }
  await startSession(u.id);
  redirect(isNew ? "/profile" : "/jobs");
}

export async function registerPro(_: State, fd: FormData): Promise<State> {
  const name = s(fd, "name"), email = s(fd, "email").toLowerCase(), pw = s(fd, "password");
  if (!name || !emailOk(email)) return { error: "Enter your name and a valid email." };
  if (pw.length < 8) return { error: "Password must be at least 8 characters." };
  if (db().prepare("SELECT 1 FROM users WHERE email=?").get(email)) return { error: "That email is already registered. Log in instead." };
  const r = db().prepare("INSERT INTO users(role,name,email,pass_hash) VALUES('jobseeker',?,?,?)").run(name, email, hashPassword(pw));
  db().prepare("INSERT INTO profiles(user_id,collar) VALUES(?,'white')").run(r.lastInsertRowid);
  await startSession(Number(r.lastInsertRowid));
  redirect("/profile");
}

export async function registerEmployer(_: State, fd: FormData): Promise<State> {
  const name = s(fd, "name"), email = s(fd, "email").toLowerCase(), pw = s(fd, "password"), company = s(fd, "company");
  const kind = s(fd, "kind") === "agency" ? "agency" : "employer";
  const licNo = s(fd, "trade_license_no"), agencyNo = s(fd, "agency_license_no");
  if (!name || !company || !emailOk(email)) return { error: "Fill in your name, company name and a valid email." };
  if (pw.length < 8) return { error: "Password must be at least 8 characters." };
  if (!licNo) return { error: "Trade license number is required for verification." };
  if (kind === "agency" && !agencyNo) return { error: "Recruitment agencies must enter their MoHRE agency license number." };
  const licFile = await saveUpload(fd.get("license_file"), "doc");
  if (!licFile) return { error: "Upload your trade license (PDF, JPG or PNG, max 5 MB)." };
  if (db().prepare("SELECT 1 FROM users WHERE email=?").get(email)) return { error: "That email is already registered. Log in instead." };
  const d = db();
  const u = d.prepare("INSERT INTO users(role,name,email,pass_hash) VALUES(?,?,?,?)").run(kind === "agency" ? "agency" : "employer", name, email, hashPassword(pw));
  const base = slugify(company) || "company";
  const slug = d.prepare("SELECT 1 FROM companies WHERE slug=?").get(base) ? `${base}-${u.lastInsertRowid}` : base;
  d.prepare("INSERT INTO companies(owner_id,kind,name,slug,city,trade_license_no,license_file,agency_license_no,description) VALUES(?,?,?,?,?,?,?,?,?)")
    .run(u.lastInsertRowid, kind, company, slug, s(fd, "city") || "Dubai", licNo, licFile, kind === "agency" ? agencyNo : null, s(fd, "description"));
  await startSession(Number(u.lastInsertRowid));
  redirect("/employer");
}

export async function loginEmail(_: State, fd: FormData): Promise<State> {
  const email = s(fd, "email").toLowerCase();
  const u = db().prepare("SELECT id,pass_hash,status,role FROM users WHERE email=?").get(email) as { id: number; pass_hash: string; status: string; role: string } | undefined;
  if (!u || !verifyPassword(s(fd, "password"), u.pass_hash)) return { error: "Wrong email or password." };
  if (u.status !== "active") return { error: "This account is suspended." };
  await startSession(u.id);
  redirect(u.role === "admin" ? "/admin" : u.role === "jobseeker" ? "/jobs" : "/employer");
}
export async function logout() { await endSession(); redirect("/"); }

// ---------- Jobseeker ----------
export async function saveProfile(_: State, fd: FormData): Promise<State> {
  const u = await requireUser(["jobseeker"]);
  const d = db();
  const cur = d.prepare("SELECT * FROM profiles WHERE user_id=?").get(u.id) as any;
  const photo = (await saveUpload(fd.get("photo"), "image", 3)) || cur?.photo || null;
  const voice = (await saveUpload(fd.get("voice"), "audio", 5)) || cur?.voice_file || null;
  const cv = (await saveUpload(fd.get("cv"), "doc", 5)) || cur?.cv_file || null;
  const langs = fd.getAll("languages").map(String).filter((x) => LANGUAGES.some((l) => l.name === x)).join(",");
  if (s(fd, "name")) d.prepare("UPDATE users SET name=? WHERE id=?").run(s(fd, "name"), u.id);
  d.prepare(`UPDATE profiles SET photo=?, voice_file=?, cv_file=?, nationality=?, age=?, languages=?, trade=?, years_exp=?, location=?, visa_status=?, license_type=?,
    available_from=?, summary=?, education=?, skills=?, certifications=?, expected_salary=?, notice_period=?, portfolio=?, experience=?, searchable=? WHERE user_id=?`)
    .run(photo, voice, cv, s(fd, "nationality"), n(fd, "age"), langs, s(fd, "trade"), n(fd, "years_exp"), s(fd, "location"), s(fd, "visa_status"), s(fd, "license_type"),
      s(fd, "available_from"), s(fd, "summary"), s(fd, "education"), s(fd, "skills"), s(fd, "certifications"), n(fd, "expected_salary"), s(fd, "notice_period"),
      s(fd, "portfolio"), s(fd, "experience"), flag(fd, "searchable"), u.id);
  for (const kind of DOC_KINDS) {
    const f = await saveUpload(fd.get(`doc_${kind}`), "doc");
    if (f) d.prepare("INSERT INTO documents(user_id,kind,file) VALUES(?,?,?)").run(u.id, kind, f);
  }
  revalidatePath("/profile");
  return { ok: "Saved." };
}

export async function applyJob(jobId: number, fd: FormData) {
  const u = await requireUser(["jobseeker"]);
  const d = db();
  const job = d.prepare("SELECT j.id,j.title,j.city,c.owner_id FROM jobs j JOIN companies c ON c.id=j.company_id WHERE j.id=? AND j.status='active' AND c.status='approved'").get(jobId) as any;
  if (!job) redirect("/jobs");
  const r = d.prepare("INSERT OR IGNORE INTO applications(job_id,user_id,cover_letter) VALUES(?,?,?)").run(jobId, u.id, s(fd, "cover_letter").slice(0, 3000));
  if (r.changes) notify(job.owner_id, `New applicant for ${job.title}`, `/employer/jobs/${jobId}`);
  revalidatePath("/jobs/" + jobSlug(job));
  redirect(`/applications?applied=${jobId}`);
}
export async function toggleSave(jobId: number) {
  const u = await requireUser(["jobseeker"]);
  const d = db();
  if (d.prepare("DELETE FROM saved_jobs WHERE user_id=? AND job_id=?").run(u.id, jobId).changes === 0) d.prepare("INSERT INTO saved_jobs VALUES(?,?)").run(u.id, jobId);
  revalidatePath("/saved");
}
export async function respondContact(id: number, approve: boolean) {
  const u = await requireUser(["jobseeker"]);
  const r = db().prepare("SELECT * FROM contact_requests WHERE id=? AND user_id=?").get(id, u.id) as any;
  if (!r) return;
  db().prepare("UPDATE contact_requests SET status=? WHERE id=?").run(approve ? "approved" : "declined", id);
  const c = db().prepare("SELECT owner_id,name FROM companies WHERE id=?").get(r.company_id) as any;
  if (approve && c) notify(c.owner_id, "A candidate approved your contact request", "/employer/candidates");
  revalidatePath("/applications");
}

// ---------- Messages ----------
export async function sendMessage(appId: number, fd: FormData) {
  const u = await requireUser();
  const a = db().prepare(`SELECT a.id,a.user_id,c.owner_id,j.title,j.id jid FROM applications a JOIN jobs j ON j.id=a.job_id JOIN companies c ON c.id=j.company_id WHERE a.id=?`).get(appId) as any;
  const body = s(fd, "body").slice(0, 2000);
  if (!a || !body || (u.id !== a.user_id && u.id !== a.owner_id)) return;
  db().prepare("INSERT INTO messages(application_id,sender_id,body) VALUES(?,?,?)").run(appId, u.id, body);
  if (u.id === a.user_id) notify(a.owner_id, `New message about ${a.title}`, `/employer/jobs/${a.jid}`);
  else notify(a.user_id, `New message about ${a.title}`, `/applications`);
  revalidatePath("/applications"); revalidatePath(`/employer/jobs/${a.jid}`);
}

// ---------- Employer ----------
export async function postJob(_: State, fd: FormData): Promise<State> {
  const u = await requireUser(["employer", "agency"]);
  const c = myCompany(u.id);
  if (!c) return { error: "No company profile." };
  if (c.status === "rejected") return { error: "Your company was rejected. Contact support." };
  const title = s(fd, "title"), desc = s(fd, "description"), cat = s(fd, "category");
  const category = CATEGORIES.find((x) => x.id === cat);
  if (!title || !desc || !category) return { error: "Title, category and description are required." };
  if (!JOB_TYPES.includes(s(fd, "job_type"))) return { error: "Choose a job type." };
  if (looksLikeScam(title, desc, s(fd, "hours"), s(fd, "area"))) return { error: SCAM_MESSAGE };
  if (c.kind === "agency" && !s(fd, "client_name")) return { error: "Agencies must name the client company for each job." };
  const min = n(fd, "salary_min"), max = n(fd, "salary_max");
  if (min && max && min > max) return { error: "Minimum salary is higher than maximum." };
  if (c.plan === "free") {
    const live = (db().prepare("SELECT COUNT(*) n FROM jobs WHERE company_id=? AND status IN ('pending','active','paused')").get(c.id) as { n: number }).n;
    if (live >= 1) return { error: "The free plan allows 1 live job. Close an existing job or see Pricing." };
  }
  const r = db().prepare(`INSERT INTO jobs(company_id,client_name,title,category,collar,city,area,job_type,salary_min,salary_max,salary_period,negotiable,experience,languages,
    visa_provided,accommodation,transport,food,hours,vacancies,deadline,description,urgent,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'pending')`)
    .run(c.id, s(fd, "client_name") || null, title, category.id, category.collar, s(fd, "city") || "Dubai", s(fd, "area"), s(fd, "job_type"), min, max,
      ["day", "month", "year"].includes(s(fd, "salary_period")) ? s(fd, "salary_period") : "month", flag(fd, "negotiable"), n(fd, "experience") ?? 0,
      fd.getAll("languages").map(String).join(","), flag(fd, "visa_provided"), flag(fd, "accommodation"), flag(fd, "transport"), flag(fd, "food"),
      s(fd, "hours"), Math.max(1, n(fd, "vacancies") ?? 1), s(fd, "deadline") || null, desc, flag(fd, "urgent"));
  const admin = db().prepare("SELECT id FROM users WHERE role='admin'").all() as { id: number }[];
  admin.forEach((a) => notify(a.id, `Job awaiting approval: ${title}`, "/admin"));
  redirect(`/employer?posted=${r.lastInsertRowid}`);
}

export async function setMyJobStatus(jobId: number, to: "paused" | "active" | "closed") {
  const u = await requireUser(["employer", "agency"]);
  const j = db().prepare("SELECT j.status FROM jobs j JOIN companies c ON c.id=j.company_id WHERE j.id=? AND c.owner_id=?").get(jobId, u.id) as { status: string } | undefined;
  if (!j) return;
  const ok = (to === "paused" && j.status === "active") || (to === "active" && j.status === "paused") || (to === "closed" && ["active", "paused", "pending"].includes(j.status));
  if (ok) db().prepare("UPDATE jobs SET status=? WHERE id=?").run(to, jobId);
  revalidatePath("/employer");
}

export async function updateApplication(appId: number, fd: FormData) {
  const u = await requireUser(["employer", "agency"]);
  const a = db().prepare(`SELECT a.*, j.title, j.id jid FROM applications a JOIN jobs j ON j.id=a.job_id JOIN companies c ON c.id=j.company_id WHERE a.id=? AND c.owner_id=?`).get(appId, u.id) as any;
  if (!a) return;
  const status = APP_STATUSES.includes(s(fd, "status")) ? s(fd, "status") : a.status;
  db().prepare("UPDATE applications SET status=?, notes=?, interview_at=? WHERE id=?").run(status, s(fd, "notes").slice(0, 2000), s(fd, "interview_at") || null, appId);
  if (status !== a.status || (s(fd, "interview_at") && s(fd, "interview_at") !== a.interview_at))
    notify(a.user_id, `Your application for ${a.title} is now: ${status}${s(fd, "interview_at") ? ` (interview ${s(fd, "interview_at").replace("T", " ")})` : ""}`, "/applications");
  revalidatePath(`/employer/jobs/${a.jid}`);
}

export async function saveCompany(_: State, fd: FormData): Promise<State> {
  const u = await requireUser(["employer", "agency"]);
  const c = myCompany(u.id);
  const logo = (await saveUpload(fd.get("logo"), "image", 2)) || c.logo;
  db().prepare("UPDATE companies SET description=?, city=?, website=?, logo=? WHERE id=?").run(s(fd, "description"), s(fd, "city"), s(fd, "website"), logo, c.id);
  revalidatePath("/employer");
  return { ok: "Saved." };
}

export async function requestContact(userId: number) {
  const u = await requireUser(["employer", "agency"]);
  const c = myCompany(u.id);
  if (!c || c.status !== "approved" || c.plan !== "cvdb") return;
  const r = db().prepare("INSERT OR IGNORE INTO contact_requests(company_id,user_id) VALUES(?,?)").run(c.id, userId);
  if (r.changes) notify(userId, `${c.name} would like to contact you. Approve to share your phone number.`, "/applications");
  revalidatePath("/employer/candidates");
}

/** Agency bulk upload. CSV columns: name,phone,nationality,trade,years_exp,location,visa_status */
export async function bulkUploadCandidates(_: State, fd: FormData): Promise<State> {
  const u = await requireUser(["agency"]);
  const f = fd.get("csv");
  if (!(f instanceof File) || f.size === 0 || f.size > 2_000_000) return { error: "Choose a CSV file under 2 MB." };
  const lines = (await f.text()).split(/\r?\n/).filter((l) => l.trim());
  const head = lines.shift()?.split(",").map((h) => h.trim().toLowerCase()) ?? [];
  if (!head.includes("phone") || !head.includes("name")) return { error: "CSV needs at least 'name' and 'phone' columns." };
  let added = 0, skipped = 0;
  for (const line of lines.slice(0, 1000)) {
    const cells = line.split(",").map((x) => x.trim().replace(/^"|"$/g, ""));
    const row = Object.fromEntries(head.map((h, i) => [h, cells[i] ?? ""]));
    const phone = await normalizePhone(row.phone || "");
    if (!phone || !row.name || db().prepare("SELECT 1 FROM users WHERE phone=?").get(phone)) { skipped++; continue; }
    const r = db().prepare("INSERT INTO users(role,name,phone) VALUES('jobseeker',?,?)").run(row.name, phone);
    db().prepare("INSERT INTO profiles(user_id,collar,nationality,trade,years_exp,location,visa_status) VALUES(?,'blue',?,?,?,?,?)")
      .run(r.lastInsertRowid, row.nationality || null, row.trade || null, Number(row.years_exp) || 0, row.location || null, row.visa_status || null);
    added++;
  }
  return { ok: `Added ${added} candidates, skipped ${skipped} (duplicates or invalid rows). Candidates can claim their profile by logging in with that phone number.` };
}

// ---------- Reports & admin ----------
export async function reportJob(jobId: number, fd: FormData) {
  const u = await currentUser();
  const reason = s(fd, "reason").slice(0, 1000);
  if (!reason) return;
  db().prepare("INSERT INTO reports(job_id,reporter_id,reason) VALUES(?,?,?)").run(jobId, u?.id ?? null, reason);
  redirect("/contact?reported=1");
}
export async function adminCompany(id: number, action: "approve" | "reject" | "verify") {
  await requireUser(["admin"]);
  const c = db().prepare("SELECT owner_id,name FROM companies WHERE id=?").get(id) as any; if (!c) return;
  if (action === "approve") db().prepare("UPDATE companies SET status='approved', verified=1 WHERE id=?").run(id);
  if (action === "reject") db().prepare("UPDATE companies SET status='rejected', verified=0 WHERE id=?").run(id);
  if (action === "verify") db().prepare("UPDATE companies SET verified=1-verified WHERE id=?").run(id);
  notify(c.owner_id, `Your company was ${action === "reject" ? "rejected" : "approved"} by Shughl`, "/employer");
  revalidatePath("/admin");
}
export async function adminJob(id: number, action: "approve" | "reject" | "remove") {
  await requireUser(["admin"]);
  const j = db().prepare("SELECT j.title,c.owner_id FROM jobs j JOIN companies c ON c.id=j.company_id WHERE j.id=?").get(id) as any; if (!j) return;
  db().prepare("UPDATE jobs SET status=? WHERE id=?").run(action === "approve" ? "active" : "rejected", id);
  notify(j.owner_id, `Your job "${j.title}" was ${action === "approve" ? "approved and is live" : "not approved"}`, "/employer");
  revalidatePath("/admin");
}
export async function adminReport(id: number, action: "dismiss" | "remove") {
  await requireUser(["admin"]);
  const r = db().prepare("SELECT job_id FROM reports WHERE id=?").get(id) as any; if (!r) return;
  if (action === "remove") { db().prepare("UPDATE jobs SET status='rejected' WHERE id=?").run(r.job_id); db().prepare("UPDATE reports SET status='actioned' WHERE job_id=?").run(r.job_id); }
  else db().prepare("UPDATE reports SET status='dismissed' WHERE id=?").run(id);
  revalidatePath("/admin");
}
export async function adminUser(id: number, status: "active" | "suspended") {
  const a = await requireUser(["admin"]);
  if (id !== a.id) db().prepare("UPDATE users SET status=? WHERE id=?").run(status, id);
  revalidatePath("/admin");
}
export async function adminPlan(companyId: number, plan: string) {
  await requireUser(["admin"]);
  if (["free", "featured", "cvdb"].includes(plan)) db().prepare("UPDATE companies SET plan=? WHERE id=?").run(plan, companyId);
  revalidatePath("/admin");
}
export async function adminFeature(jobId: number) {
  await requireUser(["admin"]);
  db().prepare("UPDATE jobs SET featured=1-featured WHERE id=?").run(jobId);
  revalidatePath("/admin");
}
export async function markNotificationsRead() {
  const u = await currentUser(); if (u) db().prepare("UPDATE notifications SET read=1 WHERE user_id=?").run(u.id);
  revalidatePath("/", "layout");
}
