import type { Train } from '../types'

/**
 * SAMPLE DATA. Six illustrative services spread through the morning on the
 * NDLS → AGC stretch, chosen so a couple of them overlap at a shared
 * crossing (a good test of the gate-merging logic). Numbers/names are
 * fictional; do not treat as a real timetable.
 */
export const trains: Train[] = [
  {
    number: '10001',
    name: 'Yamuna Superfast',
    kind: 'Superfast',
    avgSpeedKmh: 110,
    lengthM: 450,
    schedule: [
      { stationCode: 'NDLS', departs: '09:05' },
      { stationCode: 'FDB', departs: '09:18' },
      { stationCode: 'PWL', departs: '09:32' },
      { stationCode: 'MTJ', departs: '10:17' },
      { stationCode: 'AGC', departs: '10:28' },
    ],
  },
  {
    number: '10002',
    name: 'Braj Passenger',
    kind: 'Passenger',
    avgSpeedKmh: 55,
    lengthM: 400,
    schedule: [
      { stationCode: 'NDLS', departs: '09:20' },
      { stationCode: 'FDB', departs: '09:46' },
      { stationCode: 'PWL', departs: '10:15' },
      { stationCode: 'MTJ', departs: '11:44' },
      { stationCode: 'AGC', departs: '12:45' },
    ],
  },
  {
    number: '10003',
    name: 'Taj Superfast Express',
    kind: 'Superfast',
    avgSpeedKmh: 95,
    lengthM: 480,
    schedule: [
      { stationCode: 'NDLS', departs: '09:50' },
      { stationCode: 'FDB', departs: '10:05' },
      { stationCode: 'PWL', departs: '10:22' },
      { stationCode: 'MTJ', departs: '11:13' },
      { stationCode: 'AGC', departs: '11:49' },
    ],
  },
  {
    number: '20004',
    name: 'Mathura Freight',
    kind: 'Freight',
    avgSpeedKmh: 45,
    lengthM: 650,
    schedule: [
      { stationCode: 'NDLS', departs: '10:10' },
      { stationCode: 'FDB', departs: '10:42' },
      { stationCode: 'PWL', departs: '11:17' },
      { stationCode: 'MTJ', departs: '13:06' },
      { stationCode: 'AGC', departs: '14:21' },
    ],
  },
  {
    number: '10005',
    name: 'Agra Express',
    kind: 'Express',
    avgSpeedKmh: 80,
    lengthM: 460,
    schedule: [
      { stationCode: 'NDLS', departs: '10:40' },
      { stationCode: 'FDB', departs: '10:58' },
      { stationCode: 'PWL', departs: '11:18' },
      { stationCode: 'MTJ', departs: '12:19' },
      { stationCode: 'AGC', departs: '13:01' },
    ],
  },
  {
    number: '10006',
    name: 'Shauryapath Superfast',
    kind: 'Superfast',
    avgSpeedKmh: 100,
    lengthM: 420,
    schedule: [
      { stationCode: 'NDLS', departs: '11:15' },
      { stationCode: 'FDB', departs: '11:29' },
      { stationCode: 'PWL', departs: '11:45' },
      { stationCode: 'MTJ', departs: '12:34' },
      { stationCode: 'AGC', departs: '13:08' },
    ],
  },
]
