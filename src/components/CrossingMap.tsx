import 'leaflet/dist/leaflet.css'
import { CircleMarker, MapContainer, Popup, TileLayer } from 'react-leaflet'
import { barabankiJunction, cityView } from '../data/crossings'
import { formatDuration } from '../lib/time'
import type { CrossingStatus, GateState } from '../types'

const MARKER_COLOR: Record<GateState, string> = {
  OPEN: '#34d399',
  CLOSING_SOON: '#fbbf24',
  CLOSED: '#f87171',
}

export function CrossingMap({ statuses, nowMinutes }: { statuses: CrossingStatus[]; nowMinutes: number }) {
  return (
    <div className="overflow-hidden rounded-xl ring-1 ring-white/10">
      <MapContainer
        center={cityView.center}
        zoom={cityView.zoom}
        scrollWheelZoom={false}
        style={{ height: '360px', width: '100%', background: '#0f172a' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <CircleMarker
          center={[barabankiJunction.lat, barabankiJunction.lng]}
          radius={7}
          pathOptions={{ color: '#38bdf8', fillColor: '#0ea5e9', fillOpacity: 0.9, weight: 2 }}
        >
          <Popup>{barabankiJunction.name}</Popup>
        </CircleMarker>

        {statuses.map((status) => {
          const { crossing } = status
          return (
            <CircleMarker
              key={crossing.id}
              center={[crossing.lat, crossing.lng]}
              radius={10}
              pathOptions={{
                color: MARKER_COLOR[status.state],
                fillColor: MARKER_COLOR[status.state],
                fillOpacity: 0.85,
                weight: 2,
              }}
            >
              <Popup>
                <div className="text-sm">
                  <p className="font-medium">{crossing.name}</p>
                  <p className="text-xs text-slate-500">
                    Gate {crossing.gateNumber} · {crossing.road}
                  </p>
                  <p className="mt-1">
                    {status.state === 'CLOSED' && status.closedUntil !== undefined
                      ? `Closed — reopens in ${formatDuration(status.closedUntil - nowMinutes)}`
                      : status.state === 'CLOSING_SOON' && status.minutesToClose !== undefined
                        ? `Open — closes in ${formatDuration(status.minutesToClose)}`
                        : status.minutesToClose !== undefined
                          ? `Open — next closure in ${formatDuration(status.minutesToClose)}`
                          : 'Open'}
                  </p>
                </div>
              </Popup>
            </CircleMarker>
          )
        })}
      </MapContainer>
    </div>
  )
}
