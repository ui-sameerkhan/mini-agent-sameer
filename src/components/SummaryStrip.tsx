import type { CrossingStatus, GateState } from '../types'

const ORDER: GateState[] = ['CLOSED', 'CLOSING_SOON', 'OPEN']

const COLORS: Record<GateState, string> = {
  OPEN: 'text-emerald-400',
  CLOSING_SOON: 'text-amber-400',
  CLOSED: 'text-red-400',
}

const LABELS: Record<GateState, string> = {
  OPEN: 'Open',
  CLOSING_SOON: 'Closing soon',
  CLOSED: 'Closed',
}

export function SummaryStrip({ statuses }: { statuses: CrossingStatus[] }) {
  const counts = statuses.reduce(
    (acc, s) => {
      acc[s.state] += 1
      return acc
    },
    { OPEN: 0, CLOSING_SOON: 0, CLOSED: 0 } as Record<GateState, number>,
  )

  return (
    <div className="flex gap-4 text-sm">
      {ORDER.map((state) => (
        <div key={state} className="flex items-center gap-1.5">
          <span className={`text-base font-semibold tabular-nums ${COLORS[state]}`}>{counts[state]}</span>
          <span className="text-slate-500">{LABELS[state]}</span>
        </div>
      ))}
      <div className="ml-auto text-slate-500">{statuses.length} gates tracked</div>
    </div>
  )
}
