import Link from "next/link";
import ActionForm from "@/components/ActionForm";
import { saveProfile } from "@/app/actions";
import { requireUser } from "@/lib/auth";
import { db } from "@/lib/db";
import { getT } from "@/lib/i18n";
import { CITIES, DOC_KINDS, LANGUAGES, LICENSE_TYPES, NATIONALITIES, VISA_STATUSES } from "@/lib/data";

export const metadata = { title: "My profile" };
export default async function Profile() {
  const u = await requireUser(["jobseeker"]);
  const { t } = await getT();
  const p = db().prepare("SELECT * FROM profiles WHERE user_id=?").get(u.id) as any;
  const docs = db().prepare("SELECT kind,created_at FROM documents WHERE user_id=? ORDER BY id DESC").all(u.id) as { kind: string; created_at: string }[];
  const white = p.collar === "white";
  const mine = (p.languages || "").split(",");
  const Sel = ({ name, opts, v }: { name: string; opts: string[]; v?: string }) => <select name={name} defaultValue={v || ""}><option value="">—</option>{opts.map((o) => <option key={o}>{o}</option>)}</select>;
  return (
    <div className="stack">
      <h1>{white ? "💼" : "👷"} {t("profile")}</h1>
      <p className="meta">Your phone number and documents are hidden. Employers only see them after you apply or approve contact.</p>
      {!white && <p><Link href="/cv" className="btn alt sm">📄 CV builder</Link></p>}
      {white && <p><Link href="/cv" className="btn alt sm">📄 CV builder / download PDF</Link></p>}
      <ActionForm action={saveProfile} encType="multipart/form-data" className="card">
        <div className="row2">
          <div><label htmlFor="name">Full name</label><input id="name" name="name" defaultValue={u.name || ""} required /></div>
          <div><label htmlFor="photo">Photo</label><input id="photo" type="file" name="photo" accept="image/*" capture="user" />{p.photo && <small>✔ uploaded</small>}</div>
          <div><label>Nationality</label><Sel name="nationality" opts={NATIONALITIES} v={p.nationality} /></div>
          <div><label htmlFor="age">Age</label><input id="age" name="age" type="number" min="16" max="70" defaultValue={p.age ?? ""} /></div>
          <div><label>{white ? "Job title / specialty" : "Trade / skill (e.g. Driver, Mason)"}</label><input name="trade" defaultValue={p.trade || ""} /></div>
          <div><label>Years of experience</label><input name="years_exp" type="number" min="0" max="50" defaultValue={p.years_exp ?? ""} /></div>
          <div><label>Current location</label><Sel name="location" opts={CITIES} v={p.location} /></div>
          <div><label>Visa status</label><Sel name="visa_status" opts={VISA_STATUSES} v={p.visa_status} /></div>
          <div><label>Driving licence</label><Sel name="license_type" opts={LICENSE_TYPES} v={p.license_type} /></div>
          <div><label>Available from</label><input name="available_from" type="date" defaultValue={p.available_from || ""} /></div>
        </div>
        <label>Languages you speak</label>
        <div style={{ display: "flex", flexWrap: "wrap", gap: "4px 18px" }}>{LANGUAGES.map((l) => <label key={l.code} className="chk"><input type="checkbox" name="languages" value={l.name} defaultChecked={mine.includes(l.name)} />{l.name}</label>)}</div>
        {!white && <><label htmlFor="voice">🎤 Voice introduction (optional, max 5 MB)</label><input id="voice" type="file" name="voice" accept="audio/*" capture />{p.voice_file && <audio controls src={`/api/files/${p.voice_file}`} />}</>}
        {white && <>
          <label>Professional summary</label><textarea name="summary" defaultValue={p.summary || ""} />
          <label>Experience (one role per line: Title, Company, Years, what you did)</label><textarea name="experience" defaultValue={p.experience || ""} />
          <label>Education</label><textarea name="education" defaultValue={p.education || ""} />
          <div className="row2">
            <div><label>Skills (comma separated)</label><input name="skills" defaultValue={p.skills || ""} /></div>
            <div><label>Certifications</label><input name="certifications" defaultValue={p.certifications || ""} /></div>
            <div><label>Expected salary (AED / month)</label><input name="expected_salary" type="number" min="0" defaultValue={p.expected_salary ?? ""} /></div>
            <div><label>Notice period</label><input name="notice_period" defaultValue={p.notice_period || ""} /></div>
          </div>
          <label>Portfolio / LinkedIn links</label><input name="portfolio" defaultValue={p.portfolio || ""} />
          <label htmlFor="cv">CV upload (PDF/DOCX)</label><input id="cv" type="file" name="cv" accept=".pdf,.docx" />{p.cv_file && <small>✔ uploaded. Automatic CV parsing arrives in Phase 2 — fill the fields above for now.</small>}
        </>}
        <h2 style={{ marginTop: 20 }}>📎 Documents (private)</h2>
        <div className="row2">{DOC_KINDS.map((k) => <div key={k}><label>{k[0].toUpperCase() + k.slice(1)}{docs.some((d) => d.kind === k) ? " ✔" : ""}</label><input type="file" name={`doc_${k}`} accept=".pdf,image/*" /></div>)}</div>
        <label className="chk"><input type="checkbox" name="searchable" defaultChecked={!!p.searchable} />Let verified employers find my profile (phone stays hidden until I approve)</label>
        <p><button className="btn big">{t("save")}</button></p>
      </ActionForm>
    </div>
  );
}
