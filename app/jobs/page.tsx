import Link from "next/link";
import type { Metadata } from "next";
import { CATEGORIES, CITIES, JOB_TYPES, catName } from "@/lib/data";
import { getT } from "@/lib/i18n";
import { searchJobs } from "@/lib/jobs";
import { JobCard } from "@/components/ui";

export const metadata: Metadata = { title: "Jobs in the UAE", description: "Search verified jobs across the UAE: drivers, construction, cleaners, security, office, IT, finance and more." };
type SP = Record<string, string | string[] | undefined>;
const one = (v: string | string[] | undefined) => (Array.isArray(v) ? v[0] : v) || "";

export default async function Jobs({ searchParams }: { searchParams: Promise<SP> }) {
  const sp = await searchParams;
  const { t, lang } = await getT();
  const benefits = ([] as string[]).concat(sp.benefits || []);
  const f = { q: one(sp.q), city: one(sp.city), category: one(sp.category), collar: one(sp.collar), type: one(sp.type), min: one(sp.min), period: one(sp.period), posted: one(sp.posted), benefits, page: Number(one(sp.page)) || 1 };
  const { rows, total, page, pages } = searchJobs(f);
  const qs = (p: number) => { const u = new URLSearchParams(); Object.entries(f).forEach(([k, v]) => { if (k !== "page" && k !== "benefits" && v) u.set(k, String(v)); }); benefits.forEach((b) => u.append("benefits", b)); u.set("page", String(p)); return `/jobs?${u}`; };
  return (
    <div className="layout">
      <aside className="card">
        <form className="filters" method="get">
          <h3>{t("filters")}</h3>
          <input name="q" defaultValue={f.q} placeholder={t("keyword")} aria-label={t("keyword")} />
          <select name="city" defaultValue={f.city}><option value="">{t("all_cities")}</option>{CITIES.map((c) => <option key={c}>{c}</option>)}</select>
          <select name="category" defaultValue={f.category}><option value="">{t("all_categories")}</option>{CATEGORIES.map((c) => <option key={c.id} value={c.id}>{c.icon} {catName(c.id, lang)}</option>)}</select>
          <select name="collar" defaultValue={f.collar}><option value="">Worker + Professional</option><option value="blue">👷 Workers / trades</option><option value="white">💼 Professionals</option></select>
          <select name="type" defaultValue={f.type}><option value="">Any job type</option>{JOB_TYPES.map((x) => <option key={x}>{x}</option>)}</select>
          <div style={{ display: "flex", gap: 6 }}>
            <input name="min" type="number" min="0" defaultValue={f.min} placeholder="Min AED" aria-label="Minimum salary" />
            <select name="period" defaultValue={f.period || "month"} aria-label="Salary period"><option value="day">/day</option><option value="month">/month</option><option value="year">/year</option></select>
          </div>
          <select name="posted" defaultValue={f.posted}><option value="">{t("date_posted")}</option><option value="1">24h</option><option value="7">7d</option><option value="30">30d</option></select>
          {[["visa_provided", t("visa_provided")], ["accommodation", t("accommodation")], ["transport", t("transport")], ["food", t("food")], ["urgent", t("urgent")]].map(([k, l]) =>
            <label key={k} className="chk"><input type="checkbox" name="benefits" value={k} defaultChecked={benefits.includes(k)} />{l}</label>)}
          <button className="btn">{t("search")}</button>
        </form>
      </aside>
      <section className="stack">
        <h1>{total} {t("jobs")}</h1>
        <div className="safe">🛡️ {t("never_pay")}</div>
        {rows.length === 0 && <p>{t("no_jobs")}</p>}
        <div className="stack">{rows.map((j) => <JobCard key={j.id} j={j} t={t} lang={lang} />)}</div>
        {pages > 1 && <nav style={{ display: "flex", gap: 8, justifyContent: "center" }}>
          {page > 1 && <Link className="btn alt sm" href={qs(page - 1)}>←</Link>}<span className="pill">{page} / {pages}</span>{page < pages && <Link className="btn alt sm" href={qs(page + 1)}>→</Link>}
        </nav>}
      </section>
    </div>
  );
}
