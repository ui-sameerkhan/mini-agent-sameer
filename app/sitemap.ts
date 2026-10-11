import type { MetadataRoute } from "next";
import { db, jobSlug } from "@/lib/db";
import { CATEGORIES, POSTS, SITE } from "@/lib/data";

export const dynamic = "force-dynamic";
export default function sitemap(): MetadataRoute.Sitemap {
  const jobs = db().prepare("SELECT j.id,j.title,j.city,j.created_at FROM jobs j JOIN companies c ON c.id=j.company_id WHERE j.status='active' AND c.status='approved'").all() as any[];
  const cos = db().prepare("SELECT slug FROM companies WHERE status='approved'").all() as { slug: string }[];
  const u = (p: string) => SITE.url + p;
  return [
    ...["", "/jobs", "/pricing", "/blog", "/about", "/contact", "/faq", "/privacy", "/terms"].map((p) => ({ url: u(p) })),
    ...CATEGORIES.map((c) => ({ url: u(`/jobs?category=${c.id}`) })), ...POSTS.map((p) => ({ url: u(`/blog/${p.slug}`) })),
    ...cos.map((c) => ({ url: u(`/companies/${c.slug}`) })), ...jobs.map((j) => ({ url: u(`/jobs/${jobSlug(j)}`), lastModified: new Date(j.created_at.replace(" ", "T") + "Z") })),
  ];
}
