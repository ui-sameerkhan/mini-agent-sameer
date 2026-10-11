import Link from "next/link";
import { requireUser } from "@/lib/auth";
import { db, jobSlug } from "@/lib/db";
import { getT } from "@/lib/i18n";
import { respondContact, sendMessage } from "@/app/actions";
import { Notifications } from "@/components/Chrome";

export const metadata = { title: "My applications" };
export default async function Apps({ searchParams }: { searchParams: Promise<{ applied?: string }> }) {
  const u = await requireUser(["jobseeker"]);
  const { applied } = await searchParams;
  const { t } = await getT();
  const d = db();
  const apps = d.prepare(`SELECT a.*, j.title, j.city, j.id jid, c.name company FROM applications a JOIN jobs j ON j.id=a.job_id JOIN companies c ON c.id=j.company_id WHERE a.user_id=? ORDER BY a.id DESC`).all(u.id) as any[];
  const reqs = d.prepare(`SELECT r.id, c.name FROM contact_requests r JOIN companies c ON c.id=r.company_id WHERE r.user_id=? AND r.status='pending'`).all(u.id) as any[];
  const prof = d.prepare("SELECT trade,location,visa_status FROM profiles WHERE user_id=?").get(u.id) as any;
  return (
    <div className="stack">
      {applied && <div className="okmsg">✔ {t("applied")}! The employer will contact you.</div>}
      {!prof?.trade && <div className="safe">Complete your <Link href="/profile">profile</Link> so employers can find you.</div>}
      <Notifications userId={u.id} />
      {reqs.map((r) => <div key={r.id} className="card"><b>{r.name}</b> wants to contact you. Approving shares your phone number with them.
        <div style={{ display: "flex", gap: 8, marginTop: 8 }}><form action={respondContact.bind(null, r.id, true)}><button className="btn sm">Approve</button></form><form action={respondContact.bind(null, r.id, false)}><button className="btn sm danger">Decline</button></form></div></div>)}
      <h1>{t("my_apps")}</h1>
      {apps.length === 0 && <p>No applications yet. <Link href="/jobs">{t("find_jobs")}</Link></p>}
      {apps.map((a) => {
        const msgs = d.prepare("SELECT * FROM messages WHERE application_id=? ORDER BY id").all(a.id) as any[];
        return (
          <section key={a.id} className="card">
            <h3><Link href={`/jobs/${jobSlug({ id: a.jid, title: a.title, city: a.city })}`}>{a.title}</Link> <span className="pill">{a.status}</span></h3>
            <div className="meta">{a.company} · {a.city}{a.interview_at ? ` · Interview: ${a.interview_at.replace("T", " ")}` : ""}</div>
            {msgs.map((m) => <div key={m.id} className={`msg ${m.sender_id === u.id ? "me" : ""}`}>{m.body}</div>)}
            <form action={sendMessage.bind(null, a.id)} style={{ display: "flex", gap: 8, marginTop: 8 }}><input name="body" placeholder="Message employer" required /><button className="btn sm">Send</button></form>
          </section>
        );
      })}
    </div>
  );
}
