import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { db } from "@/lib/db";
import { getT } from "@/lib/i18n";
import { companyJobs } from "@/lib/jobs";
import { JobCard } from "@/components/ui";

const get = (slug: string) => db().prepare("SELECT * FROM companies WHERE slug=? AND status='approved'").get(slug) as any;
export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const c = get((await params).slug); return c ? { title: `${c.name} jobs`, description: c.description?.slice(0, 150) } : {};
}
export default async function Company({ params }: { params: Promise<{ slug: string }> }) {
  const c = get((await params).slug);
  if (!c) notFound();
  const { t, lang } = await getT();
  const jobs = companyJobs(c.id);
  return (
    <div className="stack">
      <section className="card" style={{ display: "flex", gap: 16, alignItems: "center" }}>
        {c.logo ? <img src={`/api/files/${c.logo}`} alt="" width={80} height={80} style={{ objectFit: "contain" }} /> : <div style={{ fontSize: "3rem" }} aria-hidden>🏢</div>}
        <div><h1 style={{ margin: 0 }}>{c.name}</h1>
          <div className="badges">{c.verified ? <span className="badge ver">✔ {t("verified")}</span> : null}{c.kind === "agency" && <span className="badge warn">Recruitment agency · Licence {c.agency_license_no}</span>}</div>
          <div className="meta">{c.city}{c.website ? <> · <a href={c.website} rel="nofollow noopener noreferrer">{c.website}</a></> : null}</div></div>
      </section>
      {c.description && <section className="card"><p style={{ whiteSpace: "pre-wrap", margin: 0 }}>{c.description}</p></section>}
      <h2>{jobs.length} open jobs</h2>
      <div className="stack">{jobs.map((j) => <JobCard key={j.id} j={j} t={t} lang={lang} />)}</div>
    </div>
  );
}
