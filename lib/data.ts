export type Collar = "blue" | "white";
export const SITE = { name: "Shughl", tagline: "Jobs for everyone in the UAE", url: process.env.SITE_URL || "http://localhost:3000", country: "AE" };

export const CATEGORIES: { id: string; collar: Collar; icon: string; en: string; ar: string }[] = [
  { id: "driver", collar: "blue", icon: "🚗", en: "Drivers", ar: "سائقون" },
  { id: "construction", collar: "blue", icon: "🏗️", en: "Construction", ar: "البناء" },
  { id: "cleaning", collar: "blue", icon: "🧹", en: "Cleaners", ar: "عمال نظافة" },
  { id: "security", collar: "blue", icon: "💂", en: "Security", ar: "الأمن" },
  { id: "hospitality", collar: "blue", icon: "🍽️", en: "Hospitality", ar: "الضيافة" },
  { id: "factory", collar: "blue", icon: "🏭", en: "Factory", ar: "المصانع" },
  { id: "delivery", collar: "blue", icon: "🛵", en: "Delivery", ar: "التوصيل" },
  { id: "technician", collar: "blue", icon: "🔧", en: "Technicians", ar: "فنيون" },
  { id: "office", collar: "white", icon: "🗂️", en: "Office & Admin", ar: "إدارة ومكتب" },
  { id: "it", collar: "white", icon: "💻", en: "IT & Software", ar: "تقنية المعلومات" },
  { id: "finance", collar: "white", icon: "📊", en: "Finance & Accounting", ar: "المالية والمحاسبة" },
  { id: "sales", collar: "white", icon: "🤝", en: "Sales & Marketing", ar: "المبيعات والتسويق" },
  { id: "management", collar: "white", icon: "🧭", en: "Management", ar: "الإدارة" },
  { id: "healthcare", collar: "white", icon: "🩺", en: "Healthcare", ar: "الرعاية الصحية" },
];
export const catName = (id: string, lang = "en") => { const c = CATEGORIES.find((x) => x.id === id); return c ? (lang === "ar" && c.ar) || c.en : id; };
export const catIcon = (id: string) => CATEGORIES.find((x) => x.id === id)?.icon || "💼";

export const CITIES = ["Dubai", "Abu Dhabi", "Sharjah", "Ajman", "Ras Al Khaimah", "Fujairah", "Umm Al Quwain", "Al Ain"];
export const JOB_TYPES = ["full-time", "part-time", "contract", "daily wage", "temporary"];
export const SALARY_PERIODS = ["day", "month", "year"];
export const VISA_STATUSES = ["own visa", "employment visa", "visit visa", "need visa"];
export const LICENSE_TYPES = ["none", "UAE light vehicle", "UAE heavy vehicle", "UAE motorcycle", "Home-country license"];
export const LANGUAGES = [
  { code: "en", name: "English", native: "English", rtl: false },
  { code: "ar", name: "Arabic", native: "العربية", rtl: true },
  { code: "hi", name: "Hindi", native: "हिन्दी", rtl: false },
  { code: "ur", name: "Urdu", native: "اردو", rtl: true },
  { code: "bn", name: "Bengali", native: "বাংলা", rtl: false },
  { code: "tl", name: "Tagalog", native: "Tagalog", rtl: false },
  { code: "ne", name: "Nepali", native: "नेपाली", rtl: false },
];
export const NATIONALITIES = ["India", "Pakistan", "Bangladesh", "Philippines", "Nepal", "Sri Lanka", "Egypt", "Kenya", "Uganda", "Ethiopia", "Jordan", "Syria", "Lebanon", "UAE", "Other"];
export const DOC_KINDS = ["passport", "id", "trade certificate", "license"];
export const APP_STATUSES = ["applied", "shortlisted", "interview", "hired", "rejected"];

export const PLANS = [
  { id: "free", name: "Basic", price: 0, unit: "AED", blurb: "Post jobs for free", features: ["1 active job post", "Applicant dashboard", "In-app messaging"] },
  { id: "featured", name: "Featured Post", price: 199, unit: "AED / job / 30 days", blurb: "Get seen first", features: ["Top of search results", "Urgent badge", "WhatsApp alert to matching workers (Phase 2)"] },
  { id: "cvdb", name: "CV Database", price: 499, unit: "AED / month", blurb: "Find candidates yourself", features: ["Search candidate database", "Contact requests", "Unlimited job posts"] },
];

export const slugify = (s: string) => s.toLowerCase().normalize("NFKD").replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "").slice(0, 60);

export const POSTS = [
  { slug: "how-to-spot-a-fake-job-offer", title: "How to spot a fake job offer in the UAE", date: "2026-09-01", body: "Real employers never ask you to pay to apply, for a visa, or for a 'processing fee'. If someone asks for money, do not pay — report the job. Check the company has a verified badge, look for a trade license number, and never send your passport copy to an unknown WhatsApp number." },
  { slug: "build-a-cv-that-gets-calls", title: "Build a CV that gets calls", date: "2026-09-12", body: "Keep it to one or two pages. Lead with a three-line summary, list your last three roles with measurable results, and put certifications near the top. Use our CV builder to download a clean PDF in minutes." },
  { slug: "driving-licence-conversion-uae", title: "Converting your driving licence in the UAE", date: "2026-09-20", body: "Many employers want a UAE licence. Check whether your country's licence can be converted without a new test, book your eye test, and keep your Emirates ID handy. Adding your licence type to your Shughl profile makes you visible to driver jobs." },
];
