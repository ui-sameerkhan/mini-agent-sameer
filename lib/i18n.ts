import { cookies } from "next/headers";
import { LANGUAGES } from "./data";

type Dict = Record<string, string>;
const en: Dict = {
  home: "Home", jobs: "Jobs", find_jobs: "Find jobs", post_job: "Post a job", login: "Log in", logout: "Log out", signup: "Sign up",
  dashboard: "Dashboard", profile: "My profile", my_apps: "My applications", pricing: "Pricing", blog: "Career tips",
  hero_title: "Find work you can trust", hero_sub: "Real jobs from verified UAE employers. You never pay to apply.",
  search: "Search", keyword: "Job title or keyword", all_cities: "All cities", all_categories: "All categories",
  browse_categories: "What work do you do?", latest_jobs: "Latest jobs", apply: "Apply now", applied: "Applied",
  never_pay: "Jobseekers never pay to apply. If anyone asks you for money, report the job.",
  report_job: "Report this job", urgent: "Urgent hiring", visa_provided: "Visa provided", accommodation: "Accommodation",
  transport: "Transport", food: "Food allowance", verified: "Verified employer", salary: "Salary", negotiable: "Negotiable",
  phone: "Phone number", send_otp: "Send code", enter_otp: "Enter the 6-digit code", verify: "Verify", save: "Save",
  worker_signup: "I am looking for a job (phone)", pro_signup: "I am a professional (email)", employer_signup: "I want to hire",
  share: "Share", no_jobs: "No jobs found. Try different filters.", filters: "Filters", date_posted: "Date posted",
  language: "Language", footer_safe: "Never pay for a job. Report scams to us.", vacancies: "Vacancies", deadline: "Apply before",
};
const tr: Record<string, Dict> = {
  en,
  ar: { home: "الرئيسية", jobs: "الوظائف", find_jobs: "ابحث عن وظيفة", post_job: "انشر وظيفة", login: "تسجيل الدخول", logout: "خروج", signup: "إنشاء حساب", dashboard: "لوحة التحكم", profile: "ملفي", my_apps: "طلباتي", pricing: "الأسعار", blog: "نصائح مهنية",
    hero_title: "ابحث عن عمل موثوق", hero_sub: "وظائف حقيقية من أصحاب عمل موثّقين في الإمارات. لا تدفع أبدًا للتقديم.", search: "بحث", keyword: "المسمى الوظيفي أو كلمة", all_cities: "كل المدن", all_categories: "كل الفئات",
    browse_categories: "ما هو عملك؟", latest_jobs: "أحدث الوظائف", apply: "قدّم الآن", applied: "تم التقديم", never_pay: "لا يدفع الباحث عن عمل أي مبلغ للتقديم. إذا طلب أحد منك مالًا فأبلغ عن الوظيفة.",
    report_job: "الإبلاغ عن الوظيفة", urgent: "توظيف عاجل", visa_provided: "التأشيرة متوفرة", accommodation: "سكن", transport: "مواصلات", food: "بدل طعام", verified: "صاحب عمل موثّق", salary: "الراتب", negotiable: "قابل للتفاوض",
    phone: "رقم الهاتف", send_otp: "أرسل الرمز", enter_otp: "أدخل الرمز المكوّن من 6 أرقام", verify: "تحقق", save: "حفظ", worker_signup: "أبحث عن عمل (هاتف)", pro_signup: "أنا محترف (بريد إلكتروني)", employer_signup: "أريد التوظيف",
    share: "مشاركة", no_jobs: "لا توجد وظائف. جرّب فلاتر أخرى.", filters: "تصفية", date_posted: "تاريخ النشر", language: "اللغة", footer_safe: "لا تدفع مقابل وظيفة. أبلغنا عن الاحتيال.", vacancies: "الشواغر", deadline: "قدّم قبل" },
  hi: { home: "होम", jobs: "नौकरियां", find_jobs: "नौकरी खोजें", post_job: "नौकरी पोस्ट करें", login: "लॉग इन", logout: "लॉग आउट", signup: "साइन अप", dashboard: "डैशबोर्ड", profile: "मेरी प्रोफाइल", my_apps: "मेरे आवेदन", pricing: "कीमतें", blog: "करियर टिप्स",
    hero_title: "भरोसेमंद काम पाएं", hero_sub: "UAE के सत्यापित नियोक्ताओं से असली नौकरियां। आवेदन के लिए कभी पैसे न दें।", search: "खोजें", keyword: "नौकरी का नाम", all_cities: "सभी शहर", all_categories: "सभी श्रेणियां",
    browse_categories: "आप कौन सा काम करते हैं?", latest_jobs: "नई नौकरियां", apply: "अभी आवेदन करें", applied: "आवेदन किया", never_pay: "आवेदन के लिए कभी पैसे न दें। कोई पैसे मांगे तो रिपोर्ट करें।",
    report_job: "इस नौकरी की रिपोर्ट करें", urgent: "जरूरी भर्ती", visa_provided: "वीज़ा मिलेगा", accommodation: "रहने की जगह", transport: "आने-जाने की सुविधा", food: "खाने का भत्ता", verified: "सत्यापित नियोक्ता", salary: "वेतन", negotiable: "बातचीत से",
    phone: "फोन नंबर", send_otp: "कोड भेजें", enter_otp: "6 अंकों का कोड डालें", verify: "सत्यापित करें", save: "सेव करें", worker_signup: "मुझे नौकरी चाहिए (फोन)", pro_signup: "मैं प्रोफेशनल हूं (ईमेल)", employer_signup: "मुझे लोग रखने हैं",
    share: "शेयर", no_jobs: "कोई नौकरी नहीं मिली।", filters: "फ़िल्टर", date_posted: "पोस्ट की तारीख", language: "भाषा", footer_safe: "नौकरी के लिए पैसे न दें। धोखाधड़ी की रिपोर्ट करें।", vacancies: "पद", deadline: "अंतिम तिथि" },
  ur: { home: "ہوم", jobs: "نوکریاں", find_jobs: "نوکری تلاش کریں", post_job: "نوکری پوسٹ کریں", login: "لاگ ان", logout: "لاگ آؤٹ", signup: "سائن اپ", dashboard: "ڈیش بورڈ", profile: "میری پروفائل", my_apps: "میری درخواستیں", pricing: "قیمتیں", blog: "کیریئر ٹپس",
    hero_title: "قابلِ اعتماد کام تلاش کریں", hero_sub: "UAE کے تصدیق شدہ آجروں سے اصلی نوکریاں۔ درخواست کے لیے کبھی پیسے نہ دیں۔", search: "تلاش", keyword: "نوکری کا نام", all_cities: "تمام شہر", all_categories: "تمام زمرے",
    browse_categories: "آپ کیا کام کرتے ہیں؟", latest_jobs: "تازہ نوکریاں", apply: "ابھی درخواست دیں", applied: "درخواست دی", never_pay: "درخواست کے لیے کبھی پیسے نہ دیں۔ کوئی پیسے مانگے تو رپورٹ کریں۔",
    report_job: "اس نوکری کی رپورٹ کریں", urgent: "فوری بھرتی", visa_provided: "ویزا دیا جائے گا", accommodation: "رہائش", transport: "ٹرانسپورٹ", food: "کھانے کا الاؤنس", verified: "تصدیق شدہ آجر", salary: "تنخواہ", negotiable: "قابلِ گفتگو",
    phone: "فون نمبر", send_otp: "کوڈ بھیجیں", enter_otp: "6 ہندسوں کا کوڈ درج کریں", verify: "تصدیق", save: "محفوظ کریں", worker_signup: "مجھے نوکری چاہیے (فون)", pro_signup: "میں پروفیشنل ہوں (ای میل)", employer_signup: "مجھے لوگ رکھنے ہیں",
    share: "شیئر", no_jobs: "کوئی نوکری نہیں ملی۔", filters: "فلٹر", date_posted: "پوسٹ کی تاریخ", language: "زبان", footer_safe: "نوکری کے لیے پیسے نہ دیں۔ فراڈ کی رپورٹ کریں۔", vacancies: "آسامیاں", deadline: "آخری تاریخ" },
  bn: { home: "হোম", jobs: "চাকরি", find_jobs: "চাকরি খুঁজুন", post_job: "চাকরি দিন", login: "লগ ইন", logout: "লগ আউট", signup: "সাইন আপ", dashboard: "ড্যাশবোর্ড", profile: "আমার প্রোফাইল", my_apps: "আমার আবেদন", pricing: "মূল্য", blog: "ক্যারিয়ার টিপস",
    hero_title: "বিশ্বস্ত কাজ খুঁজুন", hero_sub: "UAE-র যাচাইকৃত নিয়োগকর্তাদের আসল চাকরি। আবেদনের জন্য কখনো টাকা দেবেন না।", search: "খুঁজুন", keyword: "চাকরির নাম", all_cities: "সব শহর", all_categories: "সব বিভাগ",
    browse_categories: "আপনি কী কাজ করেন?", latest_jobs: "নতুন চাকরি", apply: "এখনই আবেদন করুন", applied: "আবেদন করা হয়েছে", never_pay: "আবেদনের জন্য কখনো টাকা দেবেন না। কেউ টাকা চাইলে রিপোর্ট করুন।",
    report_job: "এই চাকরি রিপোর্ট করুন", urgent: "জরুরি নিয়োগ", visa_provided: "ভিসা দেওয়া হবে", accommodation: "থাকার ব্যবস্থা", transport: "যাতায়াত", food: "খাবার ভাতা", verified: "যাচাইকৃত নিয়োগকর্তা", salary: "বেতন", negotiable: "আলোচনা সাপেক্ষ",
    phone: "ফোন নম্বর", send_otp: "কোড পাঠান", enter_otp: "৬ সংখ্যার কোড দিন", verify: "যাচাই করুন", save: "সেভ", worker_signup: "আমি চাকরি খুঁজছি (ফোন)", pro_signup: "আমি পেশাজীবী (ইমেইল)", employer_signup: "আমি লোক নিতে চাই",
    share: "শেয়ার", no_jobs: "কোনো চাকরি পাওয়া যায়নি।", filters: "ফিল্টার", date_posted: "প্রকাশের তারিখ", language: "ভাষা", footer_safe: "চাকরির জন্য টাকা দেবেন না। প্রতারণা রিপোর্ট করুন।", vacancies: "পদ", deadline: "শেষ তারিখ" },
  tl: { home: "Home", jobs: "Trabaho", find_jobs: "Maghanap ng trabaho", post_job: "Mag-post ng trabaho", login: "Mag-log in", logout: "Mag-log out", signup: "Mag-sign up", dashboard: "Dashboard", profile: "Aking profile", my_apps: "Aking mga aplikasyon", pricing: "Presyo", blog: "Career tips",
    hero_title: "Maghanap ng mapagkakatiwalaang trabaho", hero_sub: "Totoong trabaho mula sa mga verified na employer sa UAE. Hindi ka magbabayad para mag-apply.", search: "Hanapin", keyword: "Titulo ng trabaho", all_cities: "Lahat ng lungsod", all_categories: "Lahat ng kategorya",
    browse_categories: "Anong trabaho mo?", latest_jobs: "Pinakabagong trabaho", apply: "Mag-apply na", applied: "Nag-apply na", never_pay: "Hindi ka magbabayad para mag-apply. Kung may humingi ng pera, i-report ito.",
    report_job: "I-report ang trabahong ito", urgent: "Urgent hiring", visa_provided: "May visa", accommodation: "Tirahan", transport: "Transportasyon", food: "Allowance sa pagkain", verified: "Verified na employer", salary: "Sahod", negotiable: "Puwedeng pag-usapan",
    phone: "Numero ng telepono", send_otp: "Ipadala ang code", enter_otp: "Ilagay ang 6-digit na code", verify: "I-verify", save: "I-save", worker_signup: "Naghahanap ako ng trabaho (telepono)", pro_signup: "Propesyonal ako (email)", employer_signup: "Gusto kong mag-hire",
    share: "Ibahagi", no_jobs: "Walang nahanap na trabaho.", filters: "Mga filter", date_posted: "Petsa ng post", language: "Wika", footer_safe: "Huwag magbayad para sa trabaho. I-report ang scam.", vacancies: "Bakante", deadline: "Mag-apply bago" },
  ne: { home: "गृहपृष्ठ", jobs: "जागिर", find_jobs: "जागिर खोज्नुहोस्", post_job: "जागिर पोस्ट गर्नुहोस्", login: "लगइन", logout: "लगआउट", signup: "साइन अप", dashboard: "ड्यासबोर्ड", profile: "मेरो प्रोफाइल", my_apps: "मेरा आवेदन", pricing: "मूल्य", blog: "करियर सुझाव",
    hero_title: "भरपर्दो काम खोज्नुहोस्", hero_sub: "UAE का प्रमाणित रोजगारदाताबाट साँचो जागिर। आवेदनका लागि कहिल्यै पैसा नतिर्नुहोस्।", search: "खोज्नुहोस्", keyword: "जागिरको नाम", all_cities: "सबै सहर", all_categories: "सबै वर्ग",
    browse_categories: "तपाईं के काम गर्नुहुन्छ?", latest_jobs: "नयाँ जागिर", apply: "अहिले आवेदन दिनुहोस्", applied: "आवेदन दिइयो", never_pay: "आवेदनका लागि कहिल्यै पैसा नतिर्नुहोस्। कसैले पैसा मागे रिपोर्ट गर्नुहोस्।",
    report_job: "यो जागिर रिपोर्ट गर्नुहोस्", urgent: "तत्काल भर्ना", visa_provided: "भिसा उपलब्ध", accommodation: "बसोबास", transport: "यातायात", food: "खाना भत्ता", verified: "प्रमाणित रोजगारदाता", salary: "तलब", negotiable: "छलफल गर्न सकिने",
    phone: "फोन नम्बर", send_otp: "कोड पठाउनुहोस्", enter_otp: "६ अंकको कोड हाल्नुहोस्", verify: "प्रमाणित गर्नुहोस्", save: "सुरक्षित गर्नुहोस्", worker_signup: "म जागिर खोज्दैछु (फोन)", pro_signup: "म पेशाकर्मी हुँ (इमेल)", employer_signup: "म मान्छे भर्ना गर्न चाहन्छु",
    share: "सेयर", no_jobs: "कुनै जागिर भेटिएन।", filters: "फिल्टर", date_posted: "पोस्ट मिति", language: "भाषा", footer_safe: "जागिरका लागि पैसा नतिर्नुहोस्। ठगी रिपोर्ट गर्नुहोस्।", vacancies: "पद", deadline: "अन्तिम मिति" },
};

export async function getLang(): Promise<string> {
  const c = (await cookies()).get("lang")?.value;
  return c && LANGUAGES.some((l) => l.code === c) ? c : "en";
}
export const isRtl = (lang: string) => !!LANGUAGES.find((l) => l.code === lang)?.rtl;
export function makeT(lang: string) { return (k: string) => tr[lang]?.[k] ?? en[k] ?? k; }
export async function getT() { const lang = await getLang(); return { lang, t: makeT(lang), rtl: isRtl(lang) }; }
