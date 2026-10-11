import { requireUser } from "@/lib/auth";
import { db } from "@/lib/db";
import PrintButton from "@/components/PrintButton";
export const metadata = { title: "CV builder" };
export default async function CV() {
  const u = await requireUser(["jobseeker"]);
  const p = db().prepare("SELECT * FROM profiles WHERE user_id=?").get(u.id) as any;
  const Sec = ({ h, v }: { h: string; v?: string | null }) => v ? <section><h2 style={{ borderBottom: "2px solid #0f766e", paddingBottom: 4 }}>{h}</h2><p style={{ whiteSpace: "pre-wrap" }}>{v}</p></section> : null;
  return (
    <div className="stack">
      <div className="noprint"><p>This CV is built from your profile. Click the button, then choose <b>Save as PDF</b>. Edit your <a href="/profile">profile</a> to change it.</p><PrintButton /></div>
      <article className="cv stack">
        <header><h1 style={{ marginBottom: 0 }}>{u.name}</h1><div>{p.trade}{p.location ? ` · ${p.location}` : ""}{u.email ? ` · ${u.email}` : ""}{u.phone ? ` · ${u.phone}` : ""}</div></header>
        <Sec h="Summary" v={p.summary} />
        <Sec h="Experience" v={p.experience || (p.years_exp ? `${p.years_exp} years as ${p.trade || "worker"}` : "")} />
        <Sec h="Education" v={p.education} /><Sec h="Skills" v={p.skills} /><Sec h="Certifications" v={p.certifications} />
        <Sec h="Languages" v={p.languages?.split(",").join(", ")} />
        <Sec h="Details" v={[p.nationality && `Nationality: ${p.nationality}`, p.visa_status && `Visa: ${p.visa_status}`, p.license_type && p.license_type !== "none" && `Licence: ${p.license_type}`, p.notice_period && `Notice: ${p.notice_period}`].filter(Boolean).join("\n")} />
        <Sec h="Links" v={p.portfolio} />
      </article>
    </div>
  );
}
