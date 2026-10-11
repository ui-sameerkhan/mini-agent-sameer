import Link from "next/link";
import { getT } from "@/lib/i18n";
export const metadata = { title: "Sign up" };
export default async function Signup() {
  const { t } = await getT();
  return (
    <div className="grid g3">
      <Link href="/login" className="cat"><span className="ic">👷</span>{t("worker_signup")}</Link>
      <Link href="/signup/pro" className="cat white"><span className="ic">💼</span>{t("pro_signup")}</Link>
      <Link href="/signup/employer" className="cat white"><span className="ic">🏢</span>{t("employer_signup")}</Link>
    </div>
  );
}
