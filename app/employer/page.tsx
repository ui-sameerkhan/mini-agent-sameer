import Link from "next/link";
import ActionForm from "@/components/ActionForm";
import { Notifications } from "@/components/Chrome";
import { bulkUploadCandidates, saveCompany, setMyJobStatus } from "@/app/actions";
import { myCompany, requireUser } from "@/lib/auth";
import { db, jobSlug } from "@/lib/db";
import { CITIES } from "@/lib/data";

export const metadata = { title: "Employer dashboard" };
export default async function Employer({ searchParams }: { searchParams: Promise<{ posted?: string }> }) {
  const u = await requireUser(["employer", "agency"]);
  const c = myCompany(u.id);
  const { posted } = await searchParams;
  const jobs = db().prepare(`SELECT j.*, (SELECT COUNT(*) FROM applications WHERE job_id=j.id) apps FROM jobs j WHERE company_id=? ORDER BY id DESC`).all(c.id) as any[];
  return (
    <div className="stack">
      <h1>🏢 {c.name} {c.verified ? <span className="badge ver">✔ Verified</span> : null}</h1>
      {c.status === "pending" && <div className="safe">⏳ Your company is awaiting verification (usually within 1 business day). You can post jobs now; they go live once we approve your trade licence.</div>}
      {c.status === "rejected" && <div className="err">Your company was rejected. Contact support.</div>}
      {posted && <div className="okmsg">Job submitted. It goes live after admin approval.</div>}
      <Notifications userId={u.id} />
      <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
        <Link className="btn" href="/employer/jobs/new">+ Post a job</Link>
        <Link className="btn alt" href="/employer/candidates">🔎 Candidate database</Link>
        <Link className="btn alt" href={`/companies/${c.slug}`}>View public page</Link>
        <span className="pill">Plan: {c.plan}</span>
      </div>
      <section className="card scroll"><h2>Your jobs</h2>
        <table className="tbl"><thead><tr><th>Job</th><th>Status</th><th>Applicants</th><th></th></tr></thead><tbody>
          {jobs.map((j) => <tr key={j.id}>
            <td><Link href={`/employer/jobs/${j.id}`}>{j.title}</Link> <span className="meta">· {j.city} · {j.vacancies} vacancies</span></td>
            <td><span className="pill">{j.status}</span></td><td>{j.apps}</td>
            <td style={{ whiteSpace: "nowrap" }}>
              {j.status === "active" && <form action={setMyJobStatus.bind(null, j.id, "paused")} style={{ display: "inline" }}><button className="btn sm alt">Pause</button></form>}
              {j.status === "paused" && <form action={setMyJobStatus.bind(null, j.id, "active")} style={{ display: "inline" }}><button className="btn sm alt">Resume</button></form>}
              {["active", "paused", "pending"].includes(j.status) && <form action={setMyJobStatus.bind(null, j.id, "closed")} style={{ display: "inline" }}><button className="btn sm danger">Close</button></form>}
              {j.status === "active" && <Link className="btn sm alt" href={`/jobs/${jobSlug(j)}`}>View</Link>}
            </td></tr>)}
          {jobs.length === 0 && <tr><td colSpan={4}>No jobs yet.</td></tr>}
        </tbody></table></section>
      <section className="card"><h2>Company profile</h2>
        <ActionForm action={saveCompany} encType="multipart/form-data">
          <label>Logo</label><input type="file" name="logo" accept="image/*" />
          <label>Description</label><textarea name="description" defaultValue={c.description || ""} />
          <div className="row2"><div><label>City</label><select name="city" defaultValue={c.city}>{CITIES.map((x) => <option key={x}>{x}</option>)}</select></div>
            <div><label>Website</label><input name="website" type="url" defaultValue={c.website || ""} /></div></div>
          <p><button className="btn">Save</button></p>
        </ActionForm></section>
      {u.role === "agency" && <section className="card"><h2>Bulk upload candidates (CSV)</h2>
        <p className="meta">Columns: name, phone, nationality, trade, years_exp, location, visa_status. Only upload people who agreed to be listed.</p>
        <ActionForm action={bulkUploadCandidates} encType="multipart/form-data"><input type="file" name="csv" accept=".csv" required /><p><button className="btn alt">Upload</button></p></ActionForm></section>}
    </div>
  );
}
