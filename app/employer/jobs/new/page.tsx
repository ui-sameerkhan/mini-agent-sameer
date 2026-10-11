import ActionForm from "@/components/ActionForm";
import { postJob } from "@/app/actions";
import { myCompany, requireUser } from "@/lib/auth";
import { CATEGORIES, CITIES, JOB_TYPES, LANGUAGES } from "@/lib/data";

export const metadata = { title: "Post a job" };
export default async function NewJob() {
  const u = await requireUser(["employer", "agency"]);
  const c = myCompany(u.id);
  return (
    <section className="card"><h1>Post a job</h1>
      <div className="safe">🛡️ Jobs that ask workers for any payment (fees, deposits, "refundable" money) are blocked and the company is banned.</div>
      <ActionForm action={postJob}>
        <label>Job title</label><input name="title" required placeholder="e.g. Light Vehicle Driver" />
        {c.kind === "agency" && <><label>Client company (required for agencies)</label><input name="client_name" required /></>}
        <div className="row2">
          <div><label>Category</label><select name="category" required>{CATEGORIES.map((x) => <option key={x.id} value={x.id}>{x.icon} {x.en} ({x.collar === "blue" ? "worker" : "professional"})</option>)}</select></div>
          <div><label>Job type</label><select name="job_type">{JOB_TYPES.map((x) => <option key={x}>{x}</option>)}</select></div>
          <div><label>City</label><select name="city">{CITIES.map((x) => <option key={x}>{x}</option>)}</select></div>
          <div><label>Area</label><input name="area" /></div>
          <div><label>Salary from (AED)</label><input name="salary_min" type="number" min="0" /></div>
          <div><label>Salary to (AED)</label><input name="salary_max" type="number" min="0" /></div>
          <div><label>Salary per</label><select name="salary_period" defaultValue="month"><option value="day">day</option><option value="month">month</option><option value="year">year</option></select></div>
          <div><label>Experience (years)</label><input name="experience" type="number" min="0" defaultValue={0} /></div>
          <div><label>Vacancies (bulk hiring: e.g. 50)</label><input name="vacancies" type="number" min="1" defaultValue={1} /></div>
          <div><label>Working hours</label><input name="hours" placeholder="8 hrs, 6 days" /></div>
          <div><label>Apply before</label><input name="deadline" type="date" /></div>
        </div>
        <label className="chk"><input type="checkbox" name="negotiable" />Salary negotiable (hide numbers)</label>
        <label>Languages required</label>
        <div style={{ display: "flex", flexWrap: "wrap", gap: "2px 16px" }}>{LANGUAGES.map((l) => <label key={l.code} className="chk"><input type="checkbox" name="languages" value={l.name} />{l.name}</label>)}</div>
        <label>Benefits</label>
        <div style={{ display: "flex", flexWrap: "wrap", gap: "2px 16px" }}>
          {[["visa_provided", "Visa provided"], ["accommodation", "Accommodation"], ["transport", "Transport"], ["food", "Food allowance"], ["urgent", "Urgent hiring"]].map(([k, l]) => <label key={k} className="chk"><input type="checkbox" name={k} />{l}</label>)}
        </div>
        <label>Description</label><textarea name="description" required style={{ minHeight: 180 }} />
        <p><button className="btn big">Submit for approval</button></p>
      </ActionForm>
    </section>
  );
}
