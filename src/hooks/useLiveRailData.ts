import { useEffect, useMemo, useRef, useState } from 'react'
import { crossings } from '../data/crossings'
import { trains } from '../data/trains'
import { computeCrossingStatus } from '../lib/gateLogic'
import { MockTrainStatusProvider, type LiveDelay } from '../lib/liveTrainService'
import type { CrossingStatus } from '../types'

/** Demo day starts at 09:00 so there's always a spread of trains ahead, regardless of real wall-clock time. */
export const START_MINUTES = 9 * 60

const TICK_MS = 500

export const SPEED_OPTIONS = [
  { label: 'Real-time', minutesPerSecond: 1 / 60 },
  { label: '60×', minutesPerSecond: 1 },
  { label: '300×', minutesPerSecond: 5 },
  { label: '900×', minutesPerSecond: 15 },
] as const

export function useLiveRailData() {
  const providerRef = useRef(new MockTrainStatusProvider())
  const [simMinutes, setSimMinutes] = useState(START_MINUTES)
  const [delays, setDelays] = useState<Record<string, LiveDelay>>({})
  const [speedIndex, setSpeedIndex] = useState(2) // default to 300×
  const [running, setRunning] = useState(true)

  useEffect(() => {
    providerRef.current.init(trains.map((t) => t.number))
    setDelays(providerRef.current.snapshot())
  }, [])

  useEffect(() => {
    if (!running) return
    const minutesPerSecond = SPEED_OPTIONS[speedIndex].minutesPerSecond
    const id = setInterval(() => {
      const delta = minutesPerSecond * (TICK_MS / 1000)
      setSimMinutes((prev) => {
        const next = prev + delta
        providerRef.current.tick(delta, next)
        setDelays(providerRef.current.snapshot())
        return next
      })
    }, TICK_MS)
    return () => clearInterval(id)
  }, [running, speedIndex])

  const statuses: CrossingStatus[] = useMemo(
    () => crossings.map((c) => computeCrossingStatus(c, trains, delays, simMinutes)),
    [delays, simMinutes],
  )

  function reset() {
    providerRef.current = new MockTrainStatusProvider()
    providerRef.current.init(trains.map((t) => t.number))
    setDelays(providerRef.current.snapshot())
    setSimMinutes(START_MINUTES)
  }

  return {
    simMinutes,
    statuses,
    running,
    setRunning,
    speedIndex,
    setSpeedIndex,
    reset,
  }
}
