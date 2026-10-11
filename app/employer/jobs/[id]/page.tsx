import Link from "next/link";
import { notFound } from "next/navigation";
import { requireUser } from "@/lib/auth";
import { db } from "@/lib/db";
import { APP_STATUSES } from "@/lib/data";
import { sendMessage, updateApplication } from "@/app/actions";

export default async function Applicants({ params }: { params: Promise<{ id: string }> }) {
  const u = await requireUser(["employer", "agency"]);
  const id = Number((await params).id);
  const d = db();
  const job = d.prepare("SELECT j.* FROM jobs j JOIN companies c ON c.id=j.company_id WHERE j.id=? AND c.owner_id=?").get(id, u.id) as any;
  if (!job) notFound();
  const apps = d.prepare(`SELECT a.*, u.name, u.phone, u.email, p.* FROM applications a JOIN users u ON u.id=a.user_id LEFT JOIN profiles p ON p.user_id=u.id WHERE a.job_id=? ORDER BY a.id DESC`).all(id) as any[];
  return (
    <div className="stack">
      <p><Link href="/employer">← Dashboard</Link></p>
      <h1>{job.title} — {apps.length} applicants</h1>
      {apps.map((a) => {
        const docs = d.prepare("SELECT kind,file FROM documents WHERE user_id=?").all(a.user_id) as any[];
        const msgs = d.prepare("SELECT * FROM messages WHERE application_id=?").all(a.id) as any[];
        const wa = a.phone ? `https://wa.me/${a.phone.replace(/\D/g, "")}` : null;
        return (
          <section key={a.id} className="card stack">
            <div style={{ display: "flex", gap: 12 }}>
              {a.photo ? <img src={`/api/files/${a.photo}`} alt="" width={64} height={64} style={{ borderRadius: 12, objectFit: "cover" }} /> : <div style={{ fontSize: "2.5rem" }}>👤</div>}
              <div><h3 style={{ margin: 0 }}>{a.name || "Unnamed"} <span className="pill">{a.status}</span></h3>
                <div className="meta">{[a.trade, a.nationality, a.age && `${a.age} yrs`, a.years_exp != null && `${a.years_exp} yrs exp`, a.location, a.visa_status].filter(Boolean).join(" · ")}</div>
                <div className="meta">Languages: {a.languages || "—"} · Available: {a.available_from || "—"} · Licence: {a.license_type || "—"}</div></div>
            </div>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              {a.phone && <a className="btn sm alt" href={`tel:${a.phone}`}>📞 {a.phone}</a>}{wa && <a className="btn sm alt" href={wa} target="_blank" rel="noopener noreferrer">WhatsApp</a>}
              {a.cv_file && <a className="btn sm alt" href={`/api/files/${a.cv_file}`}>CV</a>}
              {docs.map((x) => <a key={x.file} className="btn sm alt" href={`/api/files/${x.file}`}>{x.kind}</a>)}
            </div>
            {a.voice_file && <audio controls src={`/api/files/${a.voice_file}`} />}
            {a.cover_letter && <p style={{ whiteSpace: "pre-wrap" }}><b>Cover letter:</b> {a.cover_letter}</p>}
            <form action={updateApplication.bind(null, a.id)} className="row2">
              <div><label>Status</label><select name="status" defaultValue={a.status}>{APP_STATUSES.map((s) => <option key={s}>{s}</option>)}</select></div>
              <div><label>Interview</label><input type="datetime-local" name="interview_at" defaultValue={a.interview_at || ""} /></div>
              <div style={{ gridColumn: "1/-1" }}><label>Private notes</label><textarea name="notes" defaultValue={a.notes || ""} style={{ minHeight: 70 }} /></div>
              <div><button className="btn sm">Update</button></div>
            </form>
            {msgs.map((m) => <div key={m.id} className={`msg ${m.sender_id === u.id ? "me" : ""}`}>{m.body}</div>)}
            <form action={sendMessage.bind(null, a.id)} style={{ display: "flex", gap: 8 }}><input name="body" placeholder="Message candidate" required /><button className="btn sm alt">Send</button></form>
          </section>
        );
      })}
    </div>
  );
}
