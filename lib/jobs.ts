import { db } from "./db";

export type JobRow = {
  id: number; company_id: number; title: string; category: string; collar: string; city: string; area: string | null; job_type: string;
  salary_min: number | null; salary_max: number | null; salary_period: string; negotiable: number; experience: number; languages: string | null;
  visa_provided: number; accommodation: number; transport: number; food: number; hours: string | null; vacancies: number; deadline: string | null;
  description: string; urgent: number; featured: number; status: string; created_at: string; client_name: string | null;
  company_name: string; company_slug: string; company_verified: number; company_logo: string | null; company_kind: string;
};

const BASE = `SELECT j.*, c.name AS company_name, c.slug AS company_slug, c.verified AS company_verified, c.logo AS company_logo, c.kind AS company_kind
  FROM jobs j JOIN companies c ON c.id=j.company_id`;

export type Filters = { q?: string; city?: string; category?: string; collar?: string; type?: string; min?: string; period?: string; benefits?: string[]; posted?: string; page?: number };
export const PAGE_SIZE = 12;

export function searchJobs(f: Filters) {
  const where = ["j.status='active'", "c.status='approved'", "(j.deadline IS NULL OR j.deadline='' OR j.deadline>=date('now'))"];
  const args: (string | number)[] = [];
  if (f.q) { where.push("(j.title LIKE ? OR j.description LIKE ? OR c.name LIKE ?)"); const q = `%${f.q}%`; args.push(q, q, q); }
  if (f.city) { where.push("j.city=?"); args.push(f.city); }
  if (f.category) { where.push("j.category=?"); args.push(f.category); }
  if (f.collar === "blue" || f.collar === "white") { where.push("j.collar=?"); args.push(f.collar); }
  if (f.type) { where.push("j.job_type=?"); args.push(f.type); }
  if (f.min && Number(f.min) > 0) { where.push("COALESCE(j.salary_max,j.salary_min,0)>=?"); args.push(Number(f.min)); if (f.period) { where.push("j.salary_period=?"); args.push(f.period); } }
  for (const b of f.benefits || []) if (["visa_provided", "accommodation", "transport", "food", "urgent"].includes(b)) where.push(`j.${b}=1`);
  const days = Number(f.posted); if (days > 0) { where.push("j.created_at>=datetime('now', ?)"); args.push(`-${days} days`); }
  const page = Math.max(1, f.page || 1);
  const total = (db().prepare(`SELECT COUNT(*) n FROM jobs j JOIN companies c ON c.id=j.company_id WHERE ${where.join(" AND ")}`).get(...args) as { n: number }).n;
  const rows = db().prepare(`${BASE} WHERE ${where.join(" AND ")} ORDER BY j.featured DESC, j.urgent DESC, j.created_at DESC, j.id DESC LIMIT ? OFFSET ?`)
    .all(...args, PAGE_SIZE, (page - 1) * PAGE_SIZE) as JobRow[];
  return { rows, total, page, pages: Math.max(1, Math.ceil(total / PAGE_SIZE)) };
}
export const getJob = (id: number) => db().prepare(`${BASE} WHERE j.id=?`).get(id) as JobRow | undefined;
export const companyJobs = (cid: number) => db().prepare(`${BASE} WHERE j.company_id=? AND j.status='active' ORDER BY j.created_at DESC`).all(cid) as JobRow[];

export function salaryText(j: Pick<JobRow, "salary_min" | "salary_max" | "salary_period" | "negotiable">, negotiable = "Negotiable") {
  if (j.negotiable || (!j.salary_min && !j.salary_max)) return negotiable;
  const per = j.salary_period === "day" ? "day" : j.salary_period === "year" ? "year" : "month";
  const r = j.salary_min && j.salary_max && j.salary_min !== j.salary_max ? `${j.salary_min.toLocaleString("en")}–${j.salary_max.toLocaleString("en")}` : (j.salary_max || j.salary_min)!.toLocaleString("en");
  return `AED ${r} / ${per}`;
}
export const ago = (d: string) => { const m = Math.floor((Date.now() - new Date(d.replace(" ", "T") + "Z").getTime()) / 60000); return m < 60 ? `${Math.max(m, 1)}m` : m < 1440 ? `${Math.floor(m / 60)}h` : `${Math.floor(m / 1440)}d`; };
