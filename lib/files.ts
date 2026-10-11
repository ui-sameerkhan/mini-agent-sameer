import fs from "fs";
import path from "path";
import { randomBytes } from "crypto";
import { DATA_DIR } from "./db";

const ALLOWED: Record<string, string[]> = {
  image: ["image/jpeg", "image/png", "image/webp"],
  doc: ["application/pdf", "image/jpeg", "image/png", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"],
  audio: ["audio/webm", "audio/mpeg", "audio/mp4", "audio/ogg", "audio/wav", "audio/x-m4a"],
};
const EXT: Record<string, string> = { "image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp", "application/pdf": ".pdf",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document": ".docx", "audio/webm": ".webm", "audio/mpeg": ".mp3", "audio/mp4": ".m4a", "audio/ogg": ".ogg", "audio/wav": ".wav", "audio/x-m4a": ".m4a" };

/** Saves an uploaded File into data/uploads with a random name. Returns the stored name, or null if empty/invalid. */
export async function saveUpload(file: FormDataEntryValue | null, kind: keyof typeof ALLOWED, maxMb = 5): Promise<string | null> {
  if (!(file instanceof File) || file.size === 0) return null;
  if (file.size > maxMb * 1024 * 1024 || !ALLOWED[kind].includes(file.type)) return null;
  const name = randomBytes(16).toString("hex") + EXT[file.type];
  fs.writeFileSync(path.join(DATA_DIR, "uploads", name), Buffer.from(await file.arrayBuffer()));
  return name;
}
export const uploadPath = (name: string) => path.join(DATA_DIR, "uploads", path.basename(name));
