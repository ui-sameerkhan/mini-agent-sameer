import Link from "next/link";
import { myCompany, requireUser } from "@/lib/auth";
import { db } from "@/lib/db";
import { CITIES, VISA_STATUSES } from "@/lib/data";
import { requestContact } from "@/app/actions";

export const metadata = { title: "Candidate database" };
export default async function Candidates({ searchParams }: { searchParams: Promise<Record<string, string>> }) {
  const u = await requireUser(["employer", "agency"]);
  const c = myCompany(u.id);
  if (c.plan !== "cvdb" || c.status !== "approved") return (
    <div className="card stack"><h1>🔎 Candidate database</h1><p>{c.status !== "approved" ? "Available after your company is verified." : "Included in the CV Database plan."}</p><Link className="btn" href="/pricing">See plans</Link></div>);
  const sp = await searchParams;
  const w = ["us.status='active'", "p.searchable=1"]; const a: (string | number)[] = [];
  if (sp.q) { w.push("(p.trade LIKE ? OR p.skills LIKE ? OR p.summary LIKE ?)"); a.push(`%${sp.q}%`, `%${sp.q}%`, `%${sp.q}%`); }
  if (sp.location) { w.push("p.location=?"); a.push(sp.location); }
  if (sp.visa) { w.push("p.visa_status=?"); a.push(sp.visa); }
  if (sp.exp) { w.push("p.years_exp>=?"); a.push(Number(sp.exp) || 0); }
  if (sp.lang) { w.push("p.languages LIKE ?"); a.push(`%${sp.lang}%`); }
  const rows = db().prepare(`SELECT us.id uid, us.name, us.phone, p.*, (SELECT status FROM contact_requests WHERE company_id=? AND user_id=us.id) cr
    FROM profiles p JOIN users us ON us.id=p.user_id WHERE ${w.join(" AND ")} ORDER BY us.id DESC LIMIT 50`).all(c.id, ...a) as any[];
  return (
    <div className="stack"><h1>🔎 Candidate database</h1>
      <form className="card row2" method="get">
        <input name="q" placeholder="Trade or skill" defaultValue={sp.q} />
        <select name="location" defaultValue={sp.location}><option value="">Any location</option>{CITIES.map((x) => <option key={x}>{x}</option>)}</select>
        <select name="visa" defaultValue={sp.visa}><option value="">Any visa status</option>{VISA_STATUSES.map((x) => <option key={x}>{x}</option>)}</select>
        <input name="exp" type="number" min="0" placeholder="Min years experience" defaultValue={sp.exp} />
        <input name="lang" placeholder="Language" defaultValue={sp.lang} /><button className="btn">Search</button></form>
      {rows.map((r) => (
        <div key={r.uid} className="card">
          <b>{(r.name || "Candidate").split(" ")[0]}</b> <span className="meta">· {[r.trade, r.nationality, r.years_exp != null && `${r.years_exp} yrs`, r.location, r.visa_status].filter(Boolean).join(" · ")}</span>
          <div style={{ marginTop: 8 }}>{r.cr === "approved" ? <>📞 {r.phone} · <a href={`https://wa.me/${r.phone?.replace(/\D/g, "")}`} target="_blank" rel="noopener noreferrer">WhatsApp</a></>
            : r.cr === "pending" ? <span className="pill">Request sent</span> : r.cr === "declined" ? <span className="pill">Declined</span>
            : <form action={requestContact.bind(null, r.uid)}><button className="btn sm alt">Request contact</button></form>}</div>
        </div>))}
      {rows.length === 0 && <p>No candidates match.</p>}
    </div>
  );
}
