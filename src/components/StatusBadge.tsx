import type { GateState } from '../types'

const STYLES: Record<GateState, string> = {
  OPEN: 'bg-emerald-500/15 text-emerald-400 ring-emerald-500/30',
  CLOSING_SOON: 'bg-amber-500/15 text-amber-400 ring-amber-500/30',
  CLOSED: 'bg-red-500/15 text-red-400 ring-red-500/30',
}

const LABELS: Record<GateState, string> = {
  OPEN: 'Open',
  CLOSING_SOON: 'Closing soon',
  CLOSED: 'Closed',
}

export function StatusBadge({ state }: { state: GateState }) {
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset ${STYLES[state]}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {LABELS[state]}
    </span>
  )
}
