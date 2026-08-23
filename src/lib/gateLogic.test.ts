import { describe, expect, it } from 'vitest'
import { computeCrossingEvents, computeTrainPass, deriveStatus } from './gateLogic'
import type { LevelCrossing, Train } from '../types'

// Fixture section: station A (km 0) -- 20km -- station B (km 20).
// Both fixture stations exist in ../data/stations as codes reused for
// convenience isn't required by gateLogic, but computeTrainPass looks
// stations up by code via ../data/stations, so we reuse real codes here.
const crossing: LevelCrossing = {
  id: 'test-lc',
  gateNumber: 'LC-TEST',
  name: 'Test Crossing',
  road: 'Test Road',
  fromStation: 'NDLS',
  toStation: 'FDB', // NDLS km 0, FDB km 24 in the real fixture data
  km: 12, // midpoint
  closeBeforeMin: 5,
  clearanceBufferSec: 30,
  lat: 0,
  lng: 0,
}

function makeTrain(number: string, depNDLS: string, depFDB: string): Train {
  return {
    number,
    name: `Train ${number}`,
    kind: 'Express',
    avgSpeedKmh: 60,
    lengthM: 500,
    schedule: [
      { stationCode: 'NDLS', departs: depNDLS },
      { stationCode: 'FDB', departs: depFDB },
    ],
  }
}

describe('computeTrainPass', () => {
  it('interpolates ETA proportionally to distance along the section', () => {
    const train = makeTrain('T1', '09:00', '09:20')
    const pass = computeTrainPass(train, crossing, 0)
    expect(pass).not.toBeNull()
    // Midpoint of a 20-minute section departing 09:00 -> 09:10 (550 min).
    expect(pass!.etaMinutes).toBeCloseTo(550, 5)
  })

  it('shifts ETA by the live delay', () => {
    const train = makeTrain('T1', '09:00', '09:20')
    const pass = computeTrainPass(train, crossing, 15)
    expect(pass!.etaMinutes).toBeCloseTo(565, 5)
  })

  it('returns null when the train does not run through this section', () => {
    const train: Train = {
      number: 'T9',
      name: 'Elsewhere',
      kind: 'Express',
      avgSpeedKmh: 60,
      lengthM: 500,
      schedule: [{ stationCode: 'PWL', departs: '09:00' }],
    }
    expect(computeTrainPass(train, crossing, 0)).toBeNull()
  })
})

describe('deriveStatus', () => {
  const train = makeTrain('T1', '09:00', '09:20')
  const pass = computeTrainPass(train, crossing, 0)!
  // eta 550, occupancy = (500/1000/60)*60 + 30/60 = 1.0 -> interval [545, 551]

  it('is CLOSED while now falls inside the pre-close/clearance window', () => {
    const status = deriveStatus(crossing, [pass], 550)
    expect(status.state).toBe('CLOSED')
    expect(status.closedUntil).toBeCloseTo(551, 5)
    expect(status.activePasses).toHaveLength(1)
  })

  it('is CLOSING_SOON shortly before the close window starts', () => {
    const status = deriveStatus(crossing, [pass], 538) // 7 min before 545
    expect(status.state).toBe('CLOSING_SOON')
    expect(status.minutesToClose).toBeCloseTo(7, 5)
  })

  it('is OPEN well before or after the closure', () => {
    expect(deriveStatus(crossing, [pass], 500).state).toBe('OPEN')
    expect(deriveStatus(crossing, [pass], 600).state).toBe('OPEN')
  })

  it('merges overlapping passes into a single closed interval with both trains listed', () => {
    const train2 = makeTrain('T2', '09:03', '09:23') // eta 553, interval [548, 554]
    const pass2 = computeTrainPass(train2, crossing, 0)!
    // 552 is past train1's own interval end (551) but inside the merged span.
    const status = deriveStatus(crossing, [pass, pass2], 552)
    expect(status.state).toBe('CLOSED')
    expect(status.activePasses).toHaveLength(2)
    expect(status.closedUntil).toBeCloseTo(554, 5)
  })
})

describe('computeCrossingEvents', () => {
  it('drops trains far outside the lookahead/lookback window', () => {
    const nearTrain = makeTrain('NEAR', '09:00', '09:20')
    const farFutureTrain = makeTrain('FAR', '23:00', '23:20')
    const events = computeCrossingEvents(crossing, [nearTrain, farFutureTrain], {}, 550)
    expect(events.map((e) => e.train.number)).toEqual(['NEAR'])
  })

  it('applies live delays from the provider snapshot', () => {
    const train = makeTrain('T1', '09:00', '09:20')
    const events = computeCrossingEvents(
      crossing,
      [train],
      { T1: { delayMinutes: 20, asOfMinutes: 500, source: 'simulated' } },
      550,
    )
    expect(events[0].etaMinutes).toBeCloseTo(570, 5)
  })
})
