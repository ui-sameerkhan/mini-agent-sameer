import { ControlBar } from './components/ControlBar'
import { CrossingCard } from './components/CrossingCard'
import { CrossingMap } from './components/CrossingMap'
import { SummaryStrip } from './components/SummaryStrip'
import { useLiveRailData } from './hooks/useLiveRailData'

function App() {
  const { simMinutes, statuses, running, setRunning, speedIndex, setSpeedIndex, reset } = useLiveRailData()

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto max-w-5xl px-4 py-8">
        <header className="mb-6">
          <h1 className="text-2xl font-semibold tracking-tight">Barabanki Rail Gate Tracker</h1>
          <p className="mt-1 text-sm text-slate-400">
            Live open/closed status for level crossing gates in Barabanki city, predicted from train schedules and
            running delays.
          </p>
        </header>

        <div className="mb-4 rounded-lg border border-amber-500/20 bg-amber-500/5 px-4 py-2.5 text-xs text-amber-200/80">
          <strong className="font-semibold text-amber-200">Sample data, not live IR data.</strong> Indian Railways
          doesn't publish a free public feed for exact train position or LC gate sensors, so the stations, trains,
          schedules and gate numbers here are illustrative, and delays are simulated. The clock below runs on a
          demo timeline, not the real time of day — see the README for how to wire in a real data source.
        </div>

        <div className="space-y-4">
          <ControlBar
            simMinutes={simMinutes}
            running={running}
            setRunning={setRunning}
            speedIndex={speedIndex}
            setSpeedIndex={setSpeedIndex}
            reset={reset}
          />

          <SummaryStrip statuses={statuses} />

          <CrossingMap statuses={statuses} nowMinutes={simMinutes} />

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {statuses.map((status) => (
              <CrossingCard key={status.crossing.id} status={status} nowMinutes={simMinutes} />
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

export default App
