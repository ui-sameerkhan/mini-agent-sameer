import Link from "next/link";
import { catIcon, catName } from "@/lib/data";
import { jobSlug } from "@/lib/db";
import { ago, salaryText, type JobRow } from "@/lib/jobs";

export function Badges({ j, t }: { j: JobRow; t: (k: string) => string }) {
  return (
    <div className="badges">
      {j.urgent ? <span className="badge urgent">🔥 {t("urgent")}</span> : null}
      {j.visa_provided ? <span className="badge">🛂 {t("visa_provided")}</span> : null}
      {j.accommodation ? <span className="badge">🏠 {t("accommodation")}</span> : null}
      {j.transport ? <span className="badge">🚌 {t("transport")}</span> : null}
      {j.food ? <span className="badge">🍛 {t("food")}</span> : null}
      {j.company_verified ? <span className="badge ver">✔ {t("verified")}</span> : null}
    </div>
  );
}

export function JobCard({ j, t, lang }: { j: JobRow; t: (k: string) => string; lang: string }) {
  return (
    <Link href={`/jobs/${jobSlug(j)}`} className="card job">
      <div style={{ display: "flex", gap: 12 }}>
        <div style={{ fontSize: "2.4rem" }} aria-hidden>{catIcon(j.category)}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <h3>{j.title}</h3>
          <div className="meta">{j.client_name ? `${j.client_name} · via ${j.company_name}` : j.company_name} · {j.city}{j.area ? `, ${j.area}` : ""}</div>
          <div className="sal">{salaryText(j, t("negotiable"))}</div>
          <Badges j={j} t={t} />
          <div className="meta">{catName(j.category, lang)} · {j.job_type} · {j.vacancies > 1 ? `${j.vacancies} ${t("vacancies")} · ` : ""}{ago(j.created_at)}</div>
        </div>
      </div>
    </Link>
  );
}
