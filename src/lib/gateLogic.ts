import { stationByCode } from '../data/stations'
import type { CrossingStatus, LevelCrossing, Train, TrainPass } from '../types'
import { absoluteMinutes } from './time'
import type { LiveDelay } from './liveTrainService'

/** How far ahead we bother predicting closures, and how far back a just-cleared pass still counts. */
const LOOKAHEAD_MIN = 240
const LOOKBACK_MIN = 15
/** A gate this close to coming down is flagged CLOSING_SOON rather than plain OPEN. */
const WARN_WINDOW_MIN = 10

/**
 * Estimate when `train` reaches `crossing`, by linearly interpolating
 * between its scheduled departure from the two stations bounding the
 * crossing's block section, then shifting by the train's current live delay.
 */
export function computeTrainPass(
  train: Train,
  crossing: LevelCrossing,
  delayMinutes: number,
): TrainPass | null {
  const fromStop = train.schedule.find((s) => s.stationCode === crossing.fromStation)
  const toStop = train.schedule.find((s) => s.stationCode === crossing.toStation)
  if (!fromStop || !toStop) return null

  const fromStation = stationByCode[crossing.fromStation]
  const toStation = stationByCode[crossing.toStation]
  if (!fromStation || !toStation) return null

  const fromAbs = absoluteMinutes(fromStop.departs, fromStop.dayOffset ?? 0)
  let toAbs = absoluteMinutes(toStop.departs, toStop.dayOffset ?? 0)
  if (toAbs <= fromAbs) toAbs += 1440

  const sectionKm = toStation.km - fromStation.km
  const sectionMin = toAbs - fromAbs
  if (sectionKm <= 0 || sectionMin <= 0) return null

  const fraction = crossing.km / sectionKm
  const scheduledEta = fromAbs + fraction * sectionMin

  // Time for the train's full length to clear the road, plus the crossing's buffer.
  const occupancyMin = (train.lengthM / 1000 / train.avgSpeedKmh) * 60 + crossing.clearanceBufferSec / 60

  return {
    train,
    etaMinutes: scheduledEta + delayMinutes,
    delayMinutes,
    occupancyMin,
  }
}

/** All train passes through `crossing` worth showing, soonest first. */
export function computeCrossingEvents(
  crossing: LevelCrossing,
  trains: Train[],
  delays: Record<string, LiveDelay>,
  nowMinutes: number,
): TrainPass[] {
  const passes: TrainPass[] = []
  for (const train of trains) {
    const delayMinutes = delays[train.number]?.delayMinutes ?? 0
    const pass = computeTrainPass(train, crossing, delayMinutes)
    if (!pass) continue
    if (pass.etaMinutes + pass.occupancyMin < nowMinutes - LOOKBACK_MIN) continue
    if (pass.etaMinutes > nowMinutes + LOOKAHEAD_MIN) continue
    passes.push(pass)
  }
  return passes.sort((a, b) => a.etaMinutes - b.etaMinutes)
}

interface ClosedInterval {
  start: number
  end: number
  passes: TrainPass[]
}

function mergeClosedIntervals(crossing: LevelCrossing, passes: TrainPass[]): ClosedInterval[] {
  const raw = passes
    .map((p) => ({
      start: p.etaMinutes - crossing.closeBeforeMin,
      end: p.etaMinutes + p.occupancyMin,
      passes: [p],
    }))
    .sort((a, b) => a.start - b.start)

  const merged: ClosedInterval[] = []
  for (const interval of raw) {
    const last = merged[merged.length - 1]
    if (last && interval.start <= last.end) {
      last.end = Math.max(last.end, interval.end)
      last.passes.push(...interval.passes)
    } else {
      merged.push(interval)
    }
  }
  return merged
}

/** Derive the current OPEN / CLOSING_SOON / CLOSED status for a crossing from its upcoming passes. */
export function deriveStatus(
  crossing: LevelCrossing,
  passes: TrainPass[],
  nowMinutes: number,
): CrossingStatus {
  const intervals = mergeClosedIntervals(crossing, passes)
  const upcoming = passes.filter((p) => p.etaMinutes + p.occupancyMin >= nowMinutes)

  const active = intervals.find((i) => nowMinutes >= i.start && nowMinutes <= i.end)
  if (active) {
    return {
      crossing,
      state: 'CLOSED',
      upcoming,
      closedUntil: active.end,
      activePasses: active.passes,
    }
  }

  const next = intervals.find((i) => i.start > nowMinutes)
  if (next && next.start - nowMinutes <= WARN_WINDOW_MIN) {
    return {
      crossing,
      state: 'CLOSING_SOON',
      upcoming,
      minutesToClose: next.start - nowMinutes,
    }
  }

  return {
    crossing,
    state: 'OPEN',
    upcoming,
    minutesToClose: next ? next.start - nowMinutes : undefined,
  }
}

export function computeCrossingStatus(
  crossing: LevelCrossing,
  trains: Train[],
  delays: Record<string, LiveDelay>,
  nowMinutes: number,
): CrossingStatus {
  const passes = computeCrossingEvents(crossing, trains, delays, nowMinutes)
  return deriveStatus(crossing, passes, nowMinutes)
}
