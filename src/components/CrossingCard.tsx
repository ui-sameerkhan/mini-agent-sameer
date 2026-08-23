import { stationByCode } from '../data/stations'
import { formatClock, formatDuration } from '../lib/time'
import type { CrossingStatus } from '../types'
import { GateIcon } from './GateIcon'
import { StatusBadge } from './StatusBadge'

const CARD_RING: Record<CrossingStatus['state'], string> = {
  OPEN: 'ring-white/10',
  CLOSING_SOON: 'ring-amber-500/30',
  CLOSED: 'ring-red-500/30',
}

export function CrossingCard({ status, nowMinutes }: { status: CrossingStatus; nowMinutes: number }) {
  const { crossing, state, upcoming } = status
  const from = stationByCode[crossing.fromStation]
  const to = stationByCode[crossing.toStation]
  const activeNumbers = new Set(status.activePasses?.map((p) => p.train.number))

  return (
    <div className={`rounded-xl bg-slate-900/60 p-4 ring-1 ${CARD_RING[state]} transition-colors`}>
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <GateIcon state={state} className="h-10 w-10 shrink-0" />
          <div>
            <h3 className="font-medium text-slate-100">{crossing.name}</h3>
            <p className="text-xs text-slate-400">
              Gate {crossing.gateNumber} · {crossing.road}
            </p>
          </div>
        </div>
        <StatusBadge state={state} />
      </div>

      <p className="mt-3 text-xs text-slate-500">
        {from?.name} → {to?.name} · km {crossing.km} of section
      </p>

      {state === 'CLOSED' && status.closedUntil !== undefined && (
        <p className="mt-2 text-sm text-red-300">
          Reopens in {formatDuration(status.closedUntil - nowMinutes)} for{' '}
          {status.activePasses?.map((p) => p.train.name).join(' & ')}
        </p>
      )}
      {state === 'CLOSING_SOON' && status.minutesToClose !== undefined && (
        <p className="mt-2 text-sm text-amber-300">Gate comes down in {formatDuration(status.minutesToClose)}</p>
      )}
      {state === 'OPEN' && status.minutesToClose !== undefined && (
        <p className="mt-2 text-sm text-slate-400">Next closure in {formatDuration(status.minutesToClose)}</p>
      )}
      {state === 'OPEN' && status.minutesToClose === undefined && (
        <p className="mt-2 text-sm text-slate-500">No trains due through this section soon</p>
      )}

      {upcoming.length > 0 && (
        <ul className="mt-3 space-y-1.5 border-t border-white/5 pt-3">
          {upcoming.slice(0, 4).map((pass) => (
            <li
              key={pass.train.number}
              className={`flex items-center justify-between text-xs ${
                activeNumbers.has(pass.train.number) ? 'text-red-300' : 'text-slate-400'
              }`}
            >
              <span className="truncate">
                {pass.train.number} · {pass.train.name}{' '}
                <span className="text-slate-600">({pass.train.kind})</span>
              </span>
              <span className="ml-2 shrink-0 tabular-nums">
                {formatClock(pass.etaMinutes)}
                {pass.delayMinutes > 0 && <span className="text-amber-400"> +{Math.round(pass.delayMinutes)}m</span>}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
