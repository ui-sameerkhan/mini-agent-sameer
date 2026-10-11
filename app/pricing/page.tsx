import Link from "next/link";
import { PLANS } from "@/lib/data";
export const metadata = { title: "Pricing for employers", description: "Free basic job posts. Featured posts and CV database access for employers." };
export default function Pricing() {
  return (
    <div className="stack"><h1>Pricing for employers</h1><div className="safe">Jobseekers always use Shughl for free.</div>
      <div className="grid g3">{PLANS.map((p) => (
        <section key={p.id} className="card stack"><h2>{p.name}</h2><div style={{ fontSize: "2rem", fontWeight: 800 }}>{p.price ? `${p.price} ` : "Free"}<small className="meta">{p.price ? p.unit : ""}</small></div>
          <p className="meta">{p.blurb}</p><ul>{p.features.map((f) => <li key={f}>{f}</li>)}</ul>
          {p.price ? <Link className="btn alt" href="/contact">Contact us to activate</Link> : <Link className="btn" href="/signup/employer">Start free</Link>}</section>))}
      </div>
      <p className="meta">Online card payment (Telr / PayTabs / Stripe) and invoices arrive in Phase 2. Until then an admin activates paid plans after a bank transfer.</p></div>
  );
}
