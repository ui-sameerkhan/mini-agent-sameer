import ActionForm from "@/components/ActionForm";
import { registerPro } from "@/app/actions";
export const metadata = { title: "Professional sign up" };
export default function Pro() {
  return (
    <section className="card" style={{ maxWidth: 520, margin: "0 auto" }}>
      <h1>💼 Create your professional profile</h1>
      <ActionForm action={registerPro}>
        <label htmlFor="n">Full name</label><input id="n" name="name" autoComplete="name" required />
        <label htmlFor="e">Email</label><input id="e" name="email" type="email" autoComplete="email" required />
        <label htmlFor="p">Password (8+ characters)</label><input id="p" name="password" type="password" minLength={8} autoComplete="new-password" required />
        <p><button className="btn big">Create account</button></p>
        <p className="meta">Google and LinkedIn sign-in are planned for Phase 2 (needs OAuth keys).</p>
      </ActionForm>
    </section>
  );
}
