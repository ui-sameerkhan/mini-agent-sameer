import type { Station } from '../types'

/**
 * SAMPLE DATA. A short stretch of the New Delhi – Agra corridor (via Mathura),
 * used to demonstrate the gate-status logic. Distances are approximate and
 * for illustration only — see the in-app disclaimer.
 */
export const stations: Station[] = [
  { code: 'NDLS', name: 'New Delhi', km: 0 },
  { code: 'FDB', name: 'Faridabad', km: 24 },
  { code: 'PWL', name: 'Palwal', km: 50 },
  { code: 'MTJ', name: 'Mathura Junction', km: 132 },
  { code: 'AGC', name: 'Agra Cantt', km: 188 },
]

export const stationByCode = Object.fromEntries(stations.map((s) => [s.code, s]))
