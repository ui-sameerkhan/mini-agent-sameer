/**
 * Live train delay data.
 *
 * Indian Railways does not publish a free, public real-time feed for exact
 * train position or level-crossing gate sensors — running status is only
 * available through NTES/CRIS and a handful of licensed commercial
 * aggregators. So this app is built around a `TrainStatusProvider`
 * interface: everything downstream (the gate-status engine, the UI) only
 * depends on "what's this train's current delay, and how fresh is that
 * reading", not on where the number came from.
 *
 * `MockTrainStatusProvider` below simulates that feed for the demo. To go
 * live, implement the same interface against a real data source (a
 * licensed NTES/CRIS feed, a commercial rail-data API, or your own
 * trackside sensors) and swap it in — nothing else in the app needs to
 * change.
 */

export interface LiveDelay {
  /** Minutes late (0 = running on time; negative values are not modelled). */
  delayMinutes: number
  /** Absolute simulated minute this reading was produced. */
  asOfMinutes: number
  source: 'simulated' | 'live'
}

export interface TrainStatusProvider {
  /** Prepare internal state for this set of train numbers. */
  init(trainNumbers: string[]): void
  /** Advance the provider's internal clock/state by this many simulated minutes. */
  tick(deltaSimMinutes: number, nowSimMinutes: number): void
  /** Latest known delay per train number. */
  snapshot(): Record<string, LiveDelay>
}

interface TrainWalkState {
  delay: number
  /** Slow-moving "typical delay today" the random walk reverts toward. */
  baseline: number
  volatility: number
}

function mulberry32(seed: number) {
  let a = seed
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

export class MockTrainStatusProvider implements TrainStatusProvider {
  private state = new Map<string, TrainWalkState>()
  private now = 0
  private rand: () => number

  constructor(seed = 42) {
    this.rand = mulberry32(seed)
  }

  init(trainNumbers: string[]) {
    for (const number of trainNumbers) {
      if (this.state.has(number)) continue
      const baseline = Math.max(0, Math.round((this.rand() - 0.35) * 12))
      this.state.set(number, {
        delay: baseline,
        baseline,
        volatility: 0.6 + this.rand() * 1.2,
      })
    }
  }

  tick(deltaSimMinutes: number, nowSimMinutes: number) {
    this.now = nowSimMinutes
    if (deltaSimMinutes <= 0) return
    for (const s of this.state.values()) {
      // Occasionally the "typical delay" shifts, simulating a signal hold or
      // a section running ahead of schedule.
      if (this.rand() < 0.02 * deltaSimMinutes) {
        s.baseline = Math.max(0, s.baseline + (this.rand() - 0.5) * 20)
      }
      const meanReversion = (s.baseline - s.delay) * 0.05 * deltaSimMinutes
      const noise = (this.rand() - 0.5) * s.volatility * Math.sqrt(deltaSimMinutes)
      s.delay = Math.min(90, Math.max(0, s.delay + meanReversion + noise))
    }
  }

  snapshot(): Record<string, LiveDelay> {
    const out: Record<string, LiveDelay> = {}
    for (const [number, s] of this.state.entries()) {
      out[number] = { delayMinutes: Math.round(s.delay), asOfMinutes: this.now, source: 'simulated' }
    }
    return out
  }
}
