// Mirrors app/src/main/java/com/ktc/sitepulse/domain/WorkerSearch.kt.
export function findExact(workers, id) {
  const trimmed = (id || "").trim();
  return workers.find((w) => w.id === trimmed) || null;
}

export function suggest(workers, query, limit = 8) {
  const q = (query || "").trim().toLowerCase();
  if (!q) return [];
  const raw = (query || "").trim();
  return workers
    .filter((w) => w.name.toLowerCase().includes(q) || w.id.includes(raw))
    .slice(0, limit);
}
