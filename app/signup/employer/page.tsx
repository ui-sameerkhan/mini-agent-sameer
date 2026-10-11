import ActionForm from "@/components/ActionForm";
import { registerEmployer } from "@/app/actions";
import { CITIES } from "@/lib/data";
export const metadata = { title: "Employer sign up" };
export default function Emp() {
  return (
    <section className="card" style={{ maxWidth: 640, margin: "0 auto" }}>
      <h1>🏢 Register your company</h1>
      <p className="meta">We verify every trade licence before jobs go live. This protects jobseekers from scams.</p>
      <ActionForm action={registerEmployer} encType="multipart/form-data">
        <label>Account type</label>
        <label className="chk"><input type="radio" name="kind" value="employer" defaultChecked />Company / individual employer</label>
        <label className="chk"><input type="radio" name="kind" value="agency" />Recruitment agency (posts for clients)</label>
        <div className="row2">
          <div><label htmlFor="c">Company name</label><input id="c" name="company" required /></div>
          <div><label htmlFor="ci">City</label><select id="ci" name="city">{CITIES.map((c) => <option key={c}>{c}</option>)}</select></div>
          <div><label htmlFor="tl">Trade licence number</label><input id="tl" name="trade_license_no" required /></div>
          <div><label htmlFor="al">Agency licence number (agencies only)</label><input id="al" name="agency_license_no" /></div>
        </div>
        <label htmlFor="lf">Trade licence copy (PDF/JPG/PNG)</label><input id="lf" type="file" name="license_file" accept=".pdf,image/*" required />
        <label htmlFor="d">About the company</label><textarea id="d" name="description" />
        <div className="row2">
          <div><label htmlFor="n">Your name</label><input id="n" name="name" required /></div>
          <div><label htmlFor="e">Work email</label><input id="e" name="email" type="email" required /></div>
        </div>
        <label htmlFor="p">Password (8+ characters)</label><input id="p" name="password" type="password" minLength={8} required />
        <p><button className="btn big">Submit for verification</button></p>
      </ActionForm>
    </section>
  );
}
