import Link from "next/link";
import { CATEGORIES, CITIES, catName } from "@/lib/data";
import { getT } from "@/lib/i18n";
import { searchJobs } from "@/lib/jobs";
import { JobCard } from "@/components/ui";

export default async function Home() {
  const { t, lang } = await getT();
  const latest = searchJobs({}).rows.slice(0, 6);
  return (
    <div className="stack">
      <section className="hero">
        <h1>{t("hero_title")}</h1>
        <p>{t("hero_sub")}</p>
        <form action="/jobs">
          <input name="q" placeholder={t("keyword")} aria-label={t("keyword")} />
          <select name="city" aria-label="City"><option value="">{t("all_cities")}</option>{CITIES.map((c) => <option key={c}>{c}</option>)}</select>
          <button className="btn amber">{t("search")}</button>
        </form>
        <p style={{ marginTop: 14 }}><Link href="/login" className="btn alt">📱 {t("worker_signup")}</Link></p>
      </section>
      <div className="safe">🛡️ {t("never_pay")}</div>
      <section>
        <h2>{t("browse_categories")}</h2>
        <div className="cats">
          {CATEGORIES.map((c) => <Link key={c.id} href={`/jobs?category=${c.id}`} className={`cat ${c.collar}`}><span className="ic" aria-hidden>{c.icon}</span>{catName(c.id, lang)}</Link>)}
        </div>
      </section>
      <section>
        <h2>{t("latest_jobs")}</h2>
        <div className="grid g2">{latest.map((j) => <JobCard key={j.id} j={j} t={t} lang={lang} />)}</div>
        <p><Link href="/jobs" className="btn alt">{t("find_jobs")} →</Link></p>
      </section>
      <section className="grid g3">
        <div className="card"><h3>👷 {t("worker_signup")}</h3><Link className="btn" href="/login">{t("signup")}</Link></div>
        <div className="card"><h3>💼 {t("pro_signup")}</h3><Link className="btn alt" href="/signup/pro">{t("signup")}</Link></div>
        <div className="card"><h3>🏢 {t("employer_signup")}</h3><Link className="btn alt" href="/signup/employer">{t("post_job")}</Link></div>
      </section>
    </div>
  );
}
