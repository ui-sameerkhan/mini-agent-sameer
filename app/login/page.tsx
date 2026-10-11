import Link from "next/link";
import ActionForm from "@/components/ActionForm";
import PhoneLogin from "@/components/PhoneLogin";
import { loginEmail } from "@/app/actions";
import { getT } from "@/lib/i18n";

export const metadata = { title: "Log in" };
export default async function Login() {
  const { t } = await getT();
  return (
    <div className="grid g2">
      <section className="card stack"><h1>📱 {t("worker_signup")}</h1><p className="meta">No email or CV needed. We send a 6-digit code to your phone.</p>
        <PhoneLogin labels={{ phone: t("phone"), send: t("send_otp"), enter: t("enter_otp"), verify: t("verify") }} /></section>
      <section className="card"><h2>✉️ Email {t("login")}</h2><p className="meta">Professionals, employers, agencies and admin.</p>
        <ActionForm action={loginEmail}>
          <label htmlFor="e">Email</label><input id="e" name="email" type="email" autoComplete="email" required />
          <label htmlFor="p">Password</label><input id="p" name="password" type="password" autoComplete="current-password" required />
          <p><button className="btn">{t("login")}</button></p>
        </ActionForm>
        <p><Link href="/signup/pro">{t("pro_signup")}</Link> · <Link href="/signup/employer">{t("employer_signup")}</Link></p>
      </section>
    </div>
  );
}
