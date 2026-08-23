import { SPEED_OPTIONS } from '../hooks/useLiveRailData'
import { formatClock, START_DAY_LABEL } from '../lib/time'

interface Props {
  simMinutes: number
  running: boolean
  setRunning: (v: boolean) => void
  speedIndex: number
  setSpeedIndex: (i: number) => void
  reset: () => void
}

export function ControlBar({ simMinutes, running, setRunning, speedIndex, setSpeedIndex, reset }: Props) {
  const day = Math.floor(simMinutes / 1440) + 1
  return (
    <div className="flex flex-wrap items-center gap-3 rounded-xl bg-slate-900/60 p-3 ring-1 ring-white/10">
      <div className="font-mono text-lg tabular-nums text-slate-100">
        {day > 1 ? `${START_DAY_LABEL} +${day - 1} · ` : ''}
        {formatClock(simMinutes)}
      </div>
      <span className="text-xs text-slate-500">simulated clock</span>

      <div className="ml-auto flex items-center gap-2">
        <button
          onClick={() => setRunning(!running)}
          className="rounded-lg bg-slate-800 px-3 py-1.5 text-sm font-medium text-slate-200 ring-1 ring-white/10 hover:bg-slate-700"
        >
          {running ? 'Pause' : 'Resume'}
        </button>

        <div className="flex rounded-lg bg-slate-800 p-0.5 ring-1 ring-white/10">
          {SPEED_OPTIONS.map((opt, i) => (
            <button
              key={opt.label}
              onClick={() => setSpeedIndex(i)}
              className={`rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
                i === speedIndex ? 'bg-sky-500/20 text-sky-300' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>

        <button
          onClick={reset}
          className="rounded-lg bg-slate-800 px-3 py-1.5 text-sm font-medium text-slate-200 ring-1 ring-white/10 hover:bg-slate-700"
        >
          Reset
        </button>
      </div>
    </div>
  )
}
