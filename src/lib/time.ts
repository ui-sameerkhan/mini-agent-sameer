/** Label for the demo's simulated "day 1" — this app has no real calendar date, only a rolling clock. */
export const START_DAY_LABEL = 'Demo day'

/** Parse "HH:MM" into minutes-of-day. */
export function parseHM(hm: string): number {
  const [h, m] = hm.split(':').map(Number)
  return h * 60 + m
}

/** Absolute simulated minutes for a schedule stop, accounting for day rollover. */
export function absoluteMinutes(hm: string, dayOffset = 0): number {
  return dayOffset * 1440 + parseHM(hm)
}

export function formatClock(absMinutes: number): string {
  const m = ((Math.round(absMinutes) % 1440) + 1440) % 1440
  const h = Math.floor(m / 60)
  const mm = m % 60
  return `${String(h).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
}

/** Format a minute delta as "3m 20s" / "45s" / "1h 05m". */
export function formatDuration(minutes: number): string {
  const totalSeconds = Math.max(0, Math.round(minutes * 60))
  const h = Math.floor(totalSeconds / 3600)
  const m = Math.floor((totalSeconds % 3600) / 60)
  const s = totalSeconds % 60
  if (h > 0) return `${h}h ${String(m).padStart(2, '0')}m`
  if (m > 0) return `${m}m ${String(s).padStart(2, '0')}s`
  return `${s}s`
}
