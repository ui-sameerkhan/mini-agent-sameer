# Shughl — UAE job portal (Phase 1 MVP)

Mobile-first job portal for **blue-collar workers** (phone + OTP, icon categories, one-tap apply) and **white-collar professionals** (email, full profile, CV builder), plus employers, recruitment agencies and an admin panel.

**Stack:** Next.js 15 (App Router, server actions) · SQLite (`better-sqlite3`) · plain CSS · no third-party services. Runs on a ~$5 VPS; the schema in `lib/db.ts` is plain SQL and ports to Supabase/Postgres when you need to scale.

## Run it
```bash
npm install
npm run dev            # http://localhost:3000
```
In dev the DB (`data/shughl.db`) is created and seeded with demo jobs; **OTP codes appear on screen** (no SMS provider yet).

| Demo login | Email | Password |
|---|---|---|
| Admin | admin@shughl.local | admin12345 |
| Employer | employer@shughl.local | employer12345 |
| Worker | any phone number → code shown on screen | |

Production: `npm run build && npm start` with env vars:

| Var | Purpose |
|---|---|
| `AUTH_SECRET` | cookie signing secret (random 32+ chars). Auto-generated into `data/.secret` if unset |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | first admin account (seeded on empty DB) |
| `SITE_URL` | public URL for sitemap / structured data |
| `SMS_PROVIDER` + keys | enables real OTP (implement `sendSms` in `lib/notify.ts`). Without it, production login by phone is disabled |
| `DATA_DIR` | where DB and private uploads live (back this up) |
| `SHUGHL_DEMO=1` | demo seed data + on-screen OTP in a production build. **Never set on a real site** |

## What's built (Phase 1)
- **Auth:** phone + OTP (hashed codes, 5 min expiry, 5 attempts, 60 s resend), email + password (scrypt), signed httpOnly session cookie.
- **Jobseeker:** blue-collar profile (all requested fields, photo, voice note, private documents), white-collar profile (summary, experience, education, skills, salary, notice, portfolio, CV upload), CV builder → PDF via print, one-tap apply, cover letter (white-collar), save jobs, application tracking, messaging, contact-request approval.
- **Jobs:** every requested field; badges (urgent / visa / accommodation / transport / food / verified); search by keyword, city, category, collar, type, salary, benefits, date; pagination; share to WhatsApp/Facebook/LinkedIn; `/jobs/driver-dubai-1` style URLs (old slugs 308-redirect); Google for Jobs `JobPosting` JSON-LD; `sitemap.xml`; `robots.txt`.
- **Employer / agency:** trade-licence upload and verification, company page, post/pause/resume/close jobs, bulk vacancies, applicant dashboard (shortlist, interview, hire, reject, private notes, messages, call / WhatsApp), candidate database with masked profiles and contact requests (CV Database plan), agency licence number shown publicly, agency CSV candidate upload.
- **Admin:** approve/reject employers and jobs, view licence files, reports queue (remove job / dismiss), suspend users, set plans, feature jobs, stats.
- **Trust & safety:** "never pay" notices, job posts that ask for money are blocked (`lib/safety.ts`), report button, verified badge, phone/documents served only to the owner, admin, or an employer the candidate applied to / approved.
- **i18n:** English, Arabic, Hindi, Urdu, Bengali, Tagalog, Nepali for the core UI strings, with RTL for Arabic and Urdu.

## Not built yet (be aware)
- **Phase 2:** online payments (Stripe/Telr/PayTabs) + invoices — plans are currently activated by an admin; WhatsApp/SMS/email alerts (in-app only; `lib/notify.ts` is the hook); CV parsing; Google/LinkedIn sign-in; full translation of every page (core UI strings only).
- **Phase 3:** mobile app, AI matching, analytics.
- Admin editing of categories/cities/languages is in `lib/data.ts` (needs a deploy).
- Translations were written without a native review — have speakers check them. Privacy/Terms are drafts; get a UAE lawyer to review for PDPL compliance. Add rate limiting (e.g. at the reverse proxy) and virus-scanning of uploads before real launch.

## Deploy from a phone (no computer)
1. Merge this branch (or keep it) on GitHub.
2. On render.com choose **New → Blueprint**, connect the GitHub repo and pick this branch.
3. Type an admin password when asked, then deploy. You get a public https link.
4. `SHUGHL_DEMO=1` is for trying it out only. Remove it before real users (and connect an SMS provider).
