# Rail Gate Tracker

A live dashboard that predicts the open/closed status of Indian Railway
level crossing (LC) gates, computed from train timetables and running
delays — React + TypeScript + Tailwind, built with Vite.

## Why "predicted", not "live sensor data"

Indian Railways doesn't publish a free, public real-time feed for exact
train GPS position or LC gate sensor state — running status is only
available through NTES/CRIS or licensed commercial aggregators. So instead
of pretending to show sensor data this app doesn't have, it does what a
gatekeeper does: works out when a train is due, and derives gate state from
that.

**Everything you see here — stations, trains, schedules, gate numbers,
coordinates, and live delays — is illustrative sample data for one short,
fictionalised stretch of the New Delhi–Agra corridor.** It is not sourced
from an official IR timetable or feed, and must not be used for real
travel, safety, or operational decisions. See `src/data/*.ts` for the
disclaimers next to each dataset.

## How gate status is computed

1. **ETA at the gate** (`src/lib/gateLogic.ts::computeTrainPass`) — each
   level crossing sits at a known distance along the block section between
   two stations. A train's ETA at the crossing is the scheduled departure
   time from the section's start station, linearly interpolated by
   distance, then shifted by that train's current live delay.
2. **Occupancy window** — a train occupies the crossing from
   `ETA - closeBeforeMin` (Indian Railways lowers gates some minutes ahead
   of an approaching train) through `ETA + (train length / speed) + clearance
   buffer` (time for the full rake to clear the road, plus a safety margin).
3. **Merging** (`mergeClosedIntervals`) — overlapping or back-to-back
   occupancy windows (e.g. two trains crossing close together) are merged
   into one continuous closure, and the UI lists every train responsible.
4. **State** (`deriveStatus`) — `CLOSED` if now falls inside a window,
   `CLOSING_SOON` if a window starts within the next 10 minutes, else
   `OPEN`.

This is all pure, unit-tested logic (`src/lib/gateLogic.test.ts`) decoupled
from where the delay numbers come from.

## Live data: pluggable by design

`src/lib/liveTrainService.ts` defines a small `TrainStatusProvider`
interface — `init`, `tick`, `snapshot` — that the gate-status engine and UI
depend on. `MockTrainStatusProvider` simulates delays with a bounded random
walk so gates actually cycle open/closed while you watch. To connect a real
data source (a licensed NTES/CRIS feed, a commercial rail-data API, or your
own trackside reporting), implement the same interface against it and swap
it in inside `src/hooks/useLiveRailData.ts` — nothing else in the app needs
to change.

The on-screen clock is a simulated timeline (starting at a fixed 09:00, not
wall-clock time), with playback controls (pause, and 1×/60×/300×/900×
speed) so you can watch a full day's worth of gate cycles in seconds. A
real deployment would replace this with the actual current time.

## Project structure

```
src/
  types.ts                  Domain types (Station, Train, LevelCrossing, CrossingStatus, ...)
  data/                     Sample stations/trains/crossings (clearly marked illustrative)
  lib/
    time.ts                 HH:MM <-> minute helpers, duration formatting
    liveTrainService.ts     TrainStatusProvider interface + mock implementation
    gateLogic.ts            ETA, occupancy, interval-merging, OPEN/CLOSING_SOON/CLOSED derivation
    gateLogic.test.ts       Unit tests for the logic above
  hooks/useLiveRailData.ts  Owns the simulated clock + ticks the provider + recomputes statuses
  components/               ControlBar, SummaryStrip, CrossingCard, StatusBadge, GateIcon
  App.tsx
```

## Development

```bash
npm install
npm run dev      # start the dev server
npm run test     # run the gate-logic unit tests
npm run build    # typecheck + production build
npm run lint      # oxlint
```
