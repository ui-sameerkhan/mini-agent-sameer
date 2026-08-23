import type { GateState } from '../types'

const ROTATION: Record<GateState, number> = {
  OPEN: -75,
  CLOSING_SOON: -35,
  CLOSED: 0,
}

const STROKE: Record<GateState, string> = {
  OPEN: '#34d399',
  CLOSING_SOON: '#fbbf24',
  CLOSED: '#f87171',
}

export function GateIcon({ state, className }: { state: GateState; className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} aria-hidden="true">
      <rect x="6" y="46" width="52" height="4" rx="2" fill="#475569" />
      <circle cx="14" cy="48" r="4" fill="#334155" />
      <circle cx="50" cy="48" r="4" fill="#334155" />
      <g style={{ transition: 'transform 700ms cubic-bezier(0.4, 0, 0.2, 1)' }} transform={`rotate(${ROTATION[state]} 14 48)`}>
        <rect x="14" y="45.5" width="40" height="5" rx="2.5" fill={STROKE[state]} />
        <rect x="14" y="45.5" width="8" height="5" fill="#f8fafc" opacity="0.85" />
        <rect x="30" y="45.5" width="8" height="5" fill="#f8fafc" opacity="0.85" />
        <rect x="46" y="45.5" width="8" height="5" fill="#f8fafc" opacity="0.85" />
      </g>
      <rect x="10" y="10" width="8" height="38" rx="2" fill="#1e293b" />
      <circle cx="14" cy="16" r="4" fill={STROKE[state]} />
    </svg>
  )
}
