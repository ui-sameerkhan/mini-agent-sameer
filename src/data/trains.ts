import type { Train } from '../types'

/**
 * SAMPLE DATA. Six illustrative services through Barabanki Junction,
 * spread through the morning across its three lines (towards Lucknow,
 * towards Rudauli/Ayodhya, towards Safdarganj/Burhwal). A couple of pairs
 * are timed close together on purpose, to demonstrate the gate-merging
 * logic when two trains cross close together. Numbers/names are
 * fictional; do not treat as a real timetable.
 */
export const trains: Train[] = [
  {
    number: '15001',
    name: 'Saryu Express',
    kind: 'Superfast',
    avgSpeedKmh: 90,
    lengthM: 450,
    schedule: [
      { stationCode: 'LKO', departs: '09:05' },
      { stationCode: 'BBK', departs: '09:35' },
      { stationCode: 'RDL', departs: '10:20' },
    ],
  },
  {
    number: '15002',
    name: 'Awadh Passenger',
    kind: 'Passenger',
    avgSpeedKmh: 45,
    lengthM: 400,
    schedule: [
      { stationCode: 'LKO', departs: '09:15' },
      { stationCode: 'BBK', departs: '10:00' },
      { stationCode: 'RDL', departs: '11:10' },
    ],
  },
  {
    number: '15003',
    name: 'Bahraich Intercity',
    kind: 'Express',
    avgSpeedKmh: 75,
    lengthM: 460,
    schedule: [
      { stationCode: 'LKO', departs: '09:40' },
      { stationCode: 'BBK', departs: '10:10' },
      { stationCode: 'SFG', departs: '10:35' },
    ],
  },
  {
    number: '25004',
    name: 'Barabanki Freight',
    kind: 'Freight',
    avgSpeedKmh: 40,
    lengthM: 650,
    schedule: [
      { stationCode: 'LKO', departs: '10:00' },
      { stationCode: 'BBK', departs: '10:45' },
      { stationCode: 'SFG', departs: '11:20' },
    ],
  },
  {
    number: '15005',
    name: 'Ghaghra Express',
    kind: 'Superfast',
    avgSpeedKmh: 95,
    lengthM: 470,
    schedule: [
      { stationCode: 'LKO', departs: '09:50' },
      { stationCode: 'BBK', departs: '10:03' },
      { stationCode: 'RDL', departs: '10:45' },
    ],
  },
  {
    number: '15006',
    name: 'Barabanki–Burhwal Shuttle',
    kind: 'Passenger',
    avgSpeedKmh: 50,
    lengthM: 350,
    schedule: [
      { stationCode: 'BBK', departs: '10:50' },
      { stationCode: 'SFG', departs: '11:15' },
    ],
  },
]
