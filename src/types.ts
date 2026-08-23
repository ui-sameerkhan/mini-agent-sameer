export interface Station {
  code: string
  name: string
  /** Distance from the section's reference origin, in km, used to interpolate crossing ETAs. */
  km: number
}

export type TrainKind = 'Express' | 'Superfast' | 'Passenger' | 'Freight'

/** A scheduled stop (or through-pass point) for a train along a section. */
export interface ScheduleStop {
  stationCode: string
  /** Scheduled departure time from this stop, "HH:MM", 24h clock. */
  departs: string
  /** Day offset from the train's start day (0 = same day, 1 = next day, ...). */
  dayOffset?: number
}

export interface Train {
  number: string
  name: string
  kind: TrainKind
  /** Average running speed for this section, km/h. Used to estimate how long it occupies a crossing. */
  avgSpeedKmh: number
  /** Approximate rake length in metres. Used to estimate crossing occupation time. */
  lengthM: number
  /** Ordered stops through the section this app models, earliest first. */
  schedule: ScheduleStop[]
}

/**
 * A level crossing (LC) gate located between two adjacent modelled stations.
 * ETA at the gate is interpolated from the schedule of the two bounding stations.
 */
export interface LevelCrossing {
  id: string
  gateNumber: string
  name: string
  road: string
  /** Station codes bounding the block section this crossing sits in. */
  fromStation: string
  toStation: string
  /** Distance from `fromStation`, in km, along the section. */
  km: number
  /** Minutes the gate is lowered before a train is due, per Indian Railways general practice. */
  closeBeforeMin: number
  /** Extra clearance margin (seconds) added after a train's estimated rear-clears-the-road time. */
  clearanceBufferSec: number
  lat: number
  lng: number
}

export type GateState = 'OPEN' | 'CLOSING_SOON' | 'CLOSED'

export interface TrainPass {
  train: Train
  /** Absolute simulated minutes (from app epoch) the train is estimated to reach the crossing. */
  etaMinutes: number
  /** Current live delay applied to this estimate, minutes. */
  delayMinutes: number
  /** Minutes the gate must stay down for this single passage. */
  occupancyMin: number
}

export interface CrossingStatus {
  crossing: LevelCrossing
  state: GateState
  /** Upcoming passes within the lookahead window, soonest first. */
  upcoming: TrainPass[]
  /** If CLOSED: absolute sim-minute the gate is expected to reopen. */
  closedUntil?: number
  /** If CLOSED: the pass(es) currently holding the gate down. */
  activePasses?: TrainPass[]
  /** If OPEN/CLOSING_SOON: minutes until the gate next comes down. */
  minutesToClose?: number
}
