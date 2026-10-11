import Link from "next/link";
import { currentUser } from "@/lib/auth";
import { db } from "@/lib/db";
import { LANGUAGES, SITE } from "@/lib/data";
import { getT } from "@/lib/i18n";
import { logout, setLang, markNotificationsRead } from "@/app/actions";

export async function Header() {
  const { t, lang } = await getT();
  const u = await currentUser();
  const unread = u ? (db().prepare("SELECT COUNT(*) n FROM notifications WHERE user_id=? AND read=0").get(u.id) as { n: number }).n : 0;
  const home = !u ? "/login" : u.role === "admin" ? "/admin" : u.role === "jobseeker" ? "/applications" : "/employer";
  return (
    <header className="top">
      <div className="wrap row">
        <Link href="/" className="logo" style={{ textDecoration: "none" }}>Shughl<b>.</b></Link>
        <nav className="links" aria-label="Main">
          <Link href="/jobs">{t("jobs")}</Link>
          {(!u || u.role === "employer" || u.role === "agency") && <Link href={u ? "/employer/jobs/new" : "/signup/employer"}>{t("post_job")}</Link>}
          <Link href="/pricing">{t("pricing")}</Link>
          <details className="lang">
            <summary aria-label={t("language")}>🌐 {LANGUAGES.find((l) => l.code === lang)?.native}</summary>
            <form action={setLang} className="menu">{LANGUAGES.map((l) => <button key={l.code} name="lang" value={l.code}>{l.native}</button>)}</form>
          </details>
          {u ? (
            <>
              <Link href={home} className="bell">🔔{unread > 0 && <i>{unread}</i>}</Link>
              <Link href={home}>{u.role === "jobseeker" ? t("my_apps") : t("dashboard")}</Link>
              <form action={logout}><button>{t("logout")}</button></form>
            </>
          ) : (
            <><Link href="/login">{t("login")}</Link><Link href="/signup" className="btn sm">{t("signup")}</Link></>
          )}
        </nav>
      </div>
    </header>
  );
}

export async function Footer() {
  const { t } = await getT();
  return (
    <footer>
      <div className="wrap stack">
        <div><b>{SITE.name}</b> — {t("footer_safe")}</div>
        <div className="cols">
          {[["about", "About"], ["contact", "Contact"], ["faq", "FAQ"], ["blog", t("blog")], ["privacy", "Privacy"], ["terms", "Terms"]].map(([h, l]) => <Link key={h} href={`/${h}`}>{l}</Link>)}
        </div>
        <div>© {new Date().getFullYear()} {SITE.name}. Jobseekers never pay to apply.</div>
      </div>
    </footer>
  );
}

export async function Notifications({ userId }: { userId: number }) {
  const rows = db().prepare("SELECT * FROM notifications WHERE user_id=? ORDER BY id DESC LIMIT 8").all(userId) as { id: number; body: string; link: string; read: number; created_at: string }[];
  if (!rows.length) return null;
  return (
    <section className="card">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}><h3>🔔 Notifications</h3>
        <form action={markNotificationsRead}><button className="btn sm alt">Mark read</button></form></div>
      {rows.map((r) => <p key={r.id} style={{ margin: "6px 0", fontWeight: r.read ? 400 : 700 }}>{r.link ? <Link href={r.link}>{r.body}</Link> : r.body}</p>)}
    </section>
  );
}
