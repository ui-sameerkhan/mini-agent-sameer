import type { Metadata, Viewport } from "next";
import "./globals.css";
import { Header, Footer } from "@/components/Chrome";
import { getT } from "@/lib/i18n";
import { SITE } from "@/lib/data";

export const metadata: Metadata = {
  metadataBase: new URL(SITE.url),
  title: { default: "Shughl — Jobs in the UAE for every worker and professional", template: "%s | Shughl" },
  description: "Find verified blue-collar and white-collar jobs in Dubai, Abu Dhabi and across the UAE. Free for jobseekers — never pay to apply.",
};
export const viewport: Viewport = { width: "device-width", initialScale: 1, themeColor: "#0f766e" };

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const { lang, rtl } = await getT();
  return (
    <html lang={lang} dir={rtl ? "rtl" : "ltr"}>
      <body>
        <Header />
        <main><div className="wrap">{children}</div></main>
        <Footer />
      </body>
    </html>
  );
}
