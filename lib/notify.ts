import { db } from "./db";
import { DEMO } from "./env";
/** In-app notification always; SMS/WhatsApp/email are provider stubs (Phase 2). */
export function notify(userId: number, body: string, link = "") {
  db().prepare("INSERT INTO notifications(user_id,body,link) VALUES(?,?,?)").run(userId, body, link);
}
export async function sendSms(phone: string, text: string) {
  // Phase 2: plug in Twilio / Unifonic / WhatsApp Cloud API here using env credentials.
  if (DEMO) console.log(`[sms -> ${phone}] ${text}`);
}
export const smsConfigured = () => !!process.env.SMS_PROVIDER;
