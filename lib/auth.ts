import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { createHmac, randomBytes, scryptSync, timingSafeEqual } from "crypto";
import fs from "fs";
import path from "path";
import { db, DATA_DIR } from "./db";

function secret() {
  if (process.env.AUTH_SECRET) return process.env.AUTH_SECRET;
  const f = path.join(DATA_DIR, ".secret");
  if (!fs.existsSync(f)) fs.writeFileSync(f, randomBytes(32).toString("hex"), { mode: 0o600 });
  return fs.readFileSync(f, "utf8");
}
const sign = (v: string) => createHmac("sha256", secret()).update(v).digest("hex");
const eq = (a: string, b: string) => a.length === b.length && timingSafeEqual(Buffer.from(a), Buffer.from(b));

export type User = { id: number; role: "jobseeker" | "employer" | "agency" | "admin"; name: string | null; phone: string | null; email: string | null; status: string };

export async function startSession(userId: number) {
  const exp = Date.now() + 30 * 864e5;
  const v = `${userId}.${exp}`;
  (await cookies()).set("sid", `${v}.${sign(v)}`, { httpOnly: true, sameSite: "lax", secure: process.env.NODE_ENV === "production", path: "/", maxAge: 30 * 86400 });
}
export async function endSession() { (await cookies()).delete("sid"); }

export async function currentUser(): Promise<User | null> {
  const raw = (await cookies()).get("sid")?.value;
  if (!raw) return null;
  const [id, exp, sig] = raw.split(".");
  if (!sig || !eq(sig, sign(`${id}.${exp}`)) || Number(exp) < Date.now()) return null;
  const u = db().prepare("SELECT id,role,name,phone,email,status FROM users WHERE id=?").get(Number(id)) as User | undefined;
  return u && u.status === "active" ? u : null;
}
export async function requireUser(roles?: User["role"][]): Promise<User> {
  const u = await currentUser();
  if (!u) redirect("/login");
  if (roles && !roles.includes(u.role)) redirect("/");
  return u;
}

export function verifyPassword(pw: string, stored: string | null) {
  if (!stored) return false;
  const [salt, hash] = stored.split(":");
  return eq(scryptSync(pw, salt, 32).toString("hex"), hash);
}
export const hashCode = (phone: string, code: string) => sign(`otp:${phone}:${code}`);
export const sameHash = eq;

/** Company owned by the signed-in employer/agency (null if none yet). */
export function myCompany(userId: number) {
  return db().prepare("SELECT * FROM companies WHERE owner_id=?").get(userId) as any;
}
