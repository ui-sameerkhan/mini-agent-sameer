// Blocks job posts that ask jobseekers for money. Keep conservative: reject and ask the employer to edit.
const PATTERNS: RegExp[] = [
  /\b(registration|processing|application|visa|medical|training|security|booking|joining|agent|recruitment)\s+(fee|fees|charge|charges|deposit|payment)\b/i,
  /\b(pay|send|transfer|deposit)\b[^.\n]{0,40}\b(aed|dirhams?|usd|\$|money|fee|fees|cash)\b/i,
  /\b(aed|dirhams?)\s?\d+[^.\n]{0,30}\b(to apply|to register|to join|advance|refundable)\b/i,
  /\b(refundable|advance)\s+(deposit|payment|fee)\b/i,
  /\b(western union|money ?gram|bitcoin|crypto|gift ?card)\b/i,
  /\bpay (first|before)\b/i,
];
export function looksLikeScam(...texts: (string | null | undefined)[]): boolean {
  const t = texts.filter(Boolean).join("\n");
  return PATTERNS.some((p) => p.test(t));
}
export const SCAM_MESSAGE = "This post looks like it asks jobseekers for money (fees, deposits, payments). Shughl does not allow that. Please remove it and try again.";
