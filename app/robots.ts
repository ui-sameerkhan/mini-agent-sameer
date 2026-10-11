import type { MetadataRoute } from "next";
import { SITE } from "@/lib/data";
export default function robots(): MetadataRoute.Robots {
  return { rules: { userAgent: "*", allow: "/", disallow: ["/admin", "/employer", "/profile", "/applications", "/api/", "/cv"] }, sitemap: `${SITE.url}/sitemap.xml` };
}
