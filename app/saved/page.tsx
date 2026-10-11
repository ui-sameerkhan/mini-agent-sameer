import { requireUser } from "@/lib/auth";
import { db } from "@/lib/db";
import { getT } from "@/lib/i18n";
import { getJob } from "@/lib/jobs";
import { JobCard } from "@/components/ui";
export const metadata = { title: "Saved jobs" };
export default async function Saved() {
  const u = await requireUser(["jobseeker"]);
  const { t, lang } = await getT();
  const ids = db().prepare("SELECT job_id FROM saved_jobs WHERE user_id=?").all(u.id) as { job_id: number }[];
  const jobs = ids.map((i) => getJob(i.job_id)).filter((j) => j && j.status === "active");
  return <div className="stack"><h1>★ Saved jobs</h1>{jobs.length === 0 && <p>{t("no_jobs")}</p>}{jobs.map((j) => <JobCard key={j!.id} j={j!} t={t} lang={lang} />)}</div>;
}
