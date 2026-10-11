import { notFound, permanentRedirect } from "next/navigation";
import Link from "next/link";
import type { Metadata } from "next";
import { catName, SITE } from "@/lib/data";
import { currentUser } from "@/lib/auth";
import { db, jobIdFromSlug, jobSlug } from "@/lib/db";
import { getT } from "@/lib/i18n";
import { getJob, salaryText } from "@/lib/jobs";
import { applyJob, reportJob, toggleSave } from "@/app/actions";
import { Badges } from "@/components/ui";

type P = { params: Promise<{ slug: string }> };
const visible = (j: ReturnType<typeof getJob>, ownerOrAdmin: boolean) => j && (ownerOrAdmin || j.status === "active");

export async function generateMetadata({ params }: P): Promise<Metadata> {
  const j = getJob(jobIdFromSlug((await params).slug));
  if (!j || j.status !== "active") return {};
  return { title: `${j.title} in ${j.city}`, description: `${j.title} at ${j.company_name}, ${j.city}. ${salaryText(j)}. ${j.description.slice(0, 120)}`, alternates: { canonical: `/jobs/${jobSlug(j)}` } };
}

export default async function JobPage({ params }: P) {
  const { slug } = await params;
  const id = jobIdFromSlug(slug);
  const j = getJob(id);
  const u = await currentUser();
  const priv = !!u && (u.role === "admin" || (j && (db().prepare("SELECT 1 FROM companies WHERE id=? AND owner_id=?").get(j.company_id, u.id) as unknown)) != null);
  if (!visible(j, priv) || !j) notFound();
  if (slug !== jobSlug(j)) permanentRedirect(`/jobs/${jobSlug(j)}`);
  const { t, lang } = await getT();
  const applied = u?.role === "jobseeker" ? db().prepare("SELECT status FROM applications WHERE job_id=? AND user_id=?").get(j.id, u.id) as { status: string } | undefined : undefined;
  const saved = u ? !!db().prepare("SELECT 1 FROM saved_jobs WHERE user_id=? AND job_id=?").get(u.id, j.id) : false;
  const url = `${SITE.url}/jobs/${jobSlug(j)}`;
  const share = encodeURIComponent(`${j.title} – ${j.company_name} (${j.city}) ${url}`);
  const ld = {
    "@context": "https://schema.org", "@type": "JobPosting", title: j.title, description: j.description, datePosted: j.created_at.slice(0, 10),
    ...(j.deadline ? { validThrough: j.deadline } : {}), employmentType: { "full-time": "FULL_TIME", "part-time": "PART_TIME", contract: "CONTRACTOR", "daily wage": "OTHER", temporary: "TEMPORARY" }[j.job_type] || "OTHER",
    hiringOrganization: { "@type": "Organization", name: j.client_name || j.company_name, sameAs: `${SITE.url}/companies/${j.company_slug}` },
    jobLocation: { "@type": "Place", address: { "@type": "PostalAddress", addressLocality: j.city, addressRegion: j.area || j.city, addressCountry: SITE.country } },
    ...(j.salary_min || j.salary_max ? { baseSalary: { "@type": "MonetaryAmount", currency: "AED", value: { "@type": "QuantitativeValue", ...(j.salary_min ? { minValue: j.salary_min } : {}), ...(j.salary_max ? { maxValue: j.salary_max } : {}), unitText: { day: "DAY", month: "MONTH", year: "YEAR" }[j.salary_period] } } } : {}),
    totalJobOpenings: j.vacancies, identifier: { "@type": "PropertyValue", name: "Shughl", value: String(j.id) },
  };
  return (
    <div className="stack">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(ld).replace(/</g, "\\u003c") }} />
      {j.status !== "active" && <div className="err">Preview — this job is {j.status} and not public.</div>}
      <section className="card stack">
        <h1>{j.title}</h1>
        <div className="meta">{j.client_name ? <>{j.client_name} · recruited by </> : null}<Link href={`/companies/${j.company_slug}`}>{j.company_name}</Link> · {j.city}{j.area ? `, ${j.area}` : ""}</div>
        <Badges j={j} t={t} />
        <div className="sal" style={{ fontSize: "1.3rem" }}>{t("salary")}: {salaryText(j, t("negotiable"))}</div>
        <div className="safe">🛡️ {t("never_pay")}</div>
        {applied ? <div className="okmsg">✔ {t("applied")} — {applied.status}. <Link href="/applications">{t("my_apps")}</Link></div>
          : u?.role === "jobseeker" ? (
            <form action={applyJob.bind(null, j.id)} className="stack">
              {j.collar === "white" && <><label htmlFor="cl">Cover letter</label><textarea id="cl" name="cover_letter" /></>}
              <button className="btn big amber">✅ {t("apply")}</button>
            </form>
          ) : !u ? <Link className="btn big amber" href="/login">✅ {t("apply")}</Link> : null}
        {u?.role === "jobseeker" && <form action={toggleSave.bind(null, j.id)}><button className="btn alt sm">{saved ? "★ Saved" : "☆ Save"}</button></form>}
        <div className="share">{t("share")}:
          <a href={`https://wa.me/?text=${share}`} target="_blank" rel="noopener noreferrer">WhatsApp</a>
          <a href={`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`} target="_blank" rel="noopener noreferrer">Facebook</a>
          <a href={`https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(url)}`} target="_blank" rel="noopener noreferrer">LinkedIn</a></div>
      </section>
      <section className="card">
        <h2>Details</h2>
        <table className="tbl"><tbody>
          {[["Category", catName(j.category, lang)], ["Job type", j.job_type], ["Experience", j.experience ? `${j.experience}+ years` : "No experience needed"], ["Languages", j.languages || "Any"],
            ["Working hours", j.hours || "—"], [t("vacancies"), j.vacancies], [t("deadline"), j.deadline || "Open"]].map(([k, v]) => <tr key={String(k)}><th>{k}</th><td>{v}</td></tr>)}
        </tbody></table>
        <h2 style={{ marginTop: 16 }}>Description</h2>
        <p style={{ whiteSpace: "pre-wrap" }}>{j.description}</p>
      </section>
      <details className="card"><summary>⚠️ {t("report_job")}</summary>
        <form action={reportJob.bind(null, j.id)} className="stack"><label htmlFor="rs">Why? (asks for money, fake company, wrong details…)</label><textarea id="rs" name="reason" required /><button className="btn danger sm">{t("report_job")}</button></form>
      </details>
    </div>
  );
}
