import Link from "next/link";
import { requireUser } from "@/lib/auth";
import { db, jobSlug } from "@/lib/db";
import { adminCompany, adminFeature, adminJob, adminPlan, adminReport, adminUser } from "@/app/actions";
import { CATEGORIES, CITIES, LANGUAGES, PLANS } from "@/lib/data";

export const metadata = { title: "Admin" };
const one = (sql: string) => (db().prepare(sql).get() as { n: number }).n;
export default async function Admin() {
  await requireUser(["admin"]);
  const d = db();
  const stats = [["Jobseekers", one("SELECT COUNT(*) n FROM users WHERE role='jobseeker'")], ["Employers", one("SELECT COUNT(*) n FROM companies")], ["Live jobs", one("SELECT COUNT(*) n FROM jobs WHERE status='active'")],
    ["Applications", one("SELECT COUNT(*) n FROM applications")], ["Open reports", one("SELECT COUNT(*) n FROM reports WHERE status='open'")], ["Revenue (AED)", one("SELECT COALESCE(SUM(amount),0) n FROM payments WHERE status='paid'")]];
  const companies = d.prepare("SELECT c.*, u.email FROM companies c JOIN users u ON u.id=c.owner_id ORDER BY (c.status='pending') DESC, c.id DESC LIMIT 50").all() as any[];
  const jobs = d.prepare("SELECT j.*, c.name company, c.status cstatus FROM jobs j JOIN companies c ON c.id=j.company_id ORDER BY (j.status='pending') DESC, j.id DESC LIMIT 50").all() as any[];
  const reports = d.prepare("SELECT r.*, j.title FROM reports r JOIN jobs j ON j.id=r.job_id WHERE r.status='open' ORDER BY r.id DESC").all() as any[];
  const users = d.prepare("SELECT id,role,name,phone,email,status FROM users ORDER BY id DESC LIMIT 30").all() as any[];
  const F = ({ a, label, cls = "" }: { a: () => Promise<void>; label: string; cls?: string }) => <form action={a} style={{ display: "inline" }}><button className={`btn sm ${cls}`}>{label}</button></form>;
  return (
    <div className="stack"><h1>Admin</h1>
      <div className="grid g3">{stats.map(([k, v]) => <div key={k} className="card"><div className="meta">{k}</div><div style={{ fontSize: "1.8rem", fontWeight: 800 }}>{v}</div></div>)}</div>
      <section className="card scroll"><h2>🚩 Reports ({reports.length})</h2><table className="tbl"><tbody>
        {reports.map((r) => <tr key={r.id}><td><Link href={`/jobs/${jobSlug({ id: r.job_id, title: r.title, city: "x" })}`}>{r.title}</Link></td><td>{r.reason}</td>
          <td><F a={adminReport.bind(null, r.id, "remove")} label="Remove job" cls="danger" /> <F a={adminReport.bind(null, r.id, "dismiss")} label="Dismiss" cls="alt" /></td></tr>)}
        {!reports.length && <tr><td>None 🎉</td></tr>}</tbody></table></section>
      <section className="card scroll"><h2>🏢 Employers &amp; trade licences</h2><table className="tbl"><thead><tr><th>Company</th><th>Licence</th><th>Status</th><th>Plan</th><th></th></tr></thead><tbody>
        {companies.map((c) => <tr key={c.id}><td>{c.name}<div className="meta">{c.email} · {c.kind}{c.agency_license_no ? ` · agency ${c.agency_license_no}` : ""}</div></td>
          <td>{c.trade_license_no}{c.license_file && <> · <a href={`/api/files/${c.license_file}`} target="_blank">view file</a></>}</td><td><span className="pill">{c.status}{c.verified ? " ✔" : ""}</span></td>
          <td>{PLANS.map((p) => <F key={p.id} a={adminPlan.bind(null, c.id, p.id)} label={p.id} cls={c.plan === p.id ? "" : "alt"} />)}</td>
          <td><F a={adminCompany.bind(null, c.id, "approve")} label="Approve" /> <F a={adminCompany.bind(null, c.id, "reject")} label="Reject" cls="danger" /></td></tr>)}</tbody></table></section>
      <section className="card scroll"><h2>💼 Jobs</h2><table className="tbl"><tbody>
        {jobs.map((j) => <tr key={j.id}><td><Link href={`/jobs/${jobSlug(j)}`}>{j.title}</Link><div className="meta">{j.company} · {j.city}</div></td><td><span className="pill">{j.status}</span>{j.featured ? " ⭐" : ""}</td>
          <td>{j.status !== "active" && <F a={adminJob.bind(null, j.id, "approve")} label="Approve" />} {j.status !== "rejected" && <F a={adminJob.bind(null, j.id, "reject")} label="Reject" cls="danger" />} <F a={adminFeature.bind(null, j.id)} label="★" cls="alt" /></td></tr>)}</tbody></table></section>
      <section className="card scroll"><h2>👥 Users</h2><table className="tbl"><tbody>
        {users.map((u) => <tr key={u.id}><td>{u.name || "—"}<div className="meta">{u.role} · {u.email || u.phone}</div></td><td>{u.status}</td>
          <td>{u.role !== "admin" && (u.status === "active" ? <F a={adminUser.bind(null, u.id, "suspended")} label="Suspend" cls="danger" /> : <F a={adminUser.bind(null, u.id, "active")} label="Restore" />)}</td></tr>)}</tbody></table></section>
      <section className="card"><h2>⚙️ Reference data</h2><p className="meta">Categories, cities and languages are defined in <code>lib/data.ts</code> for the MVP (editing them needs a deploy). Admin-editable tables are a Phase 2 item.</p>
        <p>{CATEGORIES.length} categories · {CITIES.length} cities · {LANGUAGES.length} languages</p></section>
    </div>
  );
}
