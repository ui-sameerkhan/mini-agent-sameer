import fs from "fs";
import { NextResponse } from "next/server";
import { currentUser } from "@/lib/auth";
import { db } from "@/lib/db";
import { uploadPath } from "@/lib/files";

const TYPES: Record<string, string> = { ".jpg": "image/jpeg", ".png": "image/png", ".webp": "image/webp", ".pdf": "application/pdf", ".webm": "audio/webm", ".mp3": "audio/mpeg", ".m4a": "audio/mp4", ".ogg": "audio/ogg", ".wav": "audio/wav", ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document" };

// Public: company logos and profile photos. Private (owner / admin / employer with an application or approved contact): everything else.
export async function GET(_: Request, { params }: { params: Promise<{ name: string }> }) {
  const { name } = await params;
  const file = name.replace(/[^a-f0-9.]/g, "");
  const p = uploadPath(file);
  if (!fs.existsSync(p)) return new NextResponse("Not found", { status: 404 });
  const d = db();
  const isPublic = d.prepare("SELECT 1 FROM companies WHERE logo=? AND status='approved'").get(file);
  if (!isPublic) {
    const u = await currentUser();
    if (!u) return new NextResponse("Forbidden", { status: 403 });
    let ok = u.role === "admin";
    if (!ok) {
      const owner = d.prepare(`SELECT user_id FROM documents WHERE file=? UNION SELECT user_id FROM profiles WHERE photo=? OR cv_file=? OR voice_file=?
        UNION SELECT owner_id FROM companies WHERE logo=? OR license_file=?`).all(file, file, file, file, file, file) as { user_id?: number; owner_id?: number }[];
      const ownerId = owner[0] ? (owner[0].user_id ?? owner[0].owner_id) : undefined;
      if (ownerId === u.id) ok = true;
      else if ((u.role === "employer" || u.role === "agency") && ownerId) {
        ok = !!d.prepare(`SELECT 1 FROM applications a JOIN jobs j ON j.id=a.job_id JOIN companies c ON c.id=j.company_id WHERE c.owner_id=? AND a.user_id=?
          UNION SELECT 1 FROM contact_requests r JOIN companies c ON c.id=r.company_id WHERE c.owner_id=? AND r.user_id=? AND r.status='approved'`).get(u.id, ownerId, u.id, ownerId);
      }
    }
    if (!ok) return new NextResponse("Forbidden", { status: 403 });
  }
  const ext = file.slice(file.lastIndexOf("."));
  return new NextResponse(fs.readFileSync(p), { headers: { "Content-Type": TYPES[ext] || "application/octet-stream", "Cache-Control": "private, max-age=3600", "X-Content-Type-Options": "nosniff" } });
}
