import type { Station } from '../types'

/**
 * SAMPLE DATA. Stations around Barabanki Junction (Northern Railway,
 * Lucknow division, Uttar Pradesh) and the three lines radiating from it —
 * used to demonstrate the gate-status logic for crossings inside Barabanki
 * city. Distances are approximate and for illustration only — see the
 * in-app disclaimer.
 */
export const stations: Station[] = [
  { code: 'LKO', name: 'Lucknow NER', km: 0 },
  { code: 'BBK', name: 'Barabanki Junction', km: 28 },
  { code: 'SFG', name: 'Safdarganj', km: 42 }, // BBK -> Burhwal/Bahraich line
  { code: 'RDL', name: 'Rudauli', km: 58 }, // BBK -> Ayodhya line
]

export const stationByCode = Object.fromEntries(stations.map((s) => [s.code, s]))
