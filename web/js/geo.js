// Haversine distance — identical formula to app/src/main/java/com/ktc/sitepulse/domain/Geo.kt,
// which itself was ported from this app's original distM() function. Keep in sync.
const EARTH_RADIUS_M = 6371000;

export function distanceMeters(a, b) {
  const toRad = (x) => (x * Math.PI) / 180;
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const sinLat = Math.sin(dLat / 2);
  const sinLng = Math.sin(dLng / 2);
  const h = sinLat * sinLat + Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * sinLng * sinLng;
  return Math.round(2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(h))));
}

export function nearestSite(point, sites) {
  if (!sites.length) return null;
  let best = null;
  for (const site of sites) {
    const d = distanceMeters(point, { lat: site.lat, lng: site.lng });
    if (!best || d < best.distanceM) best = { site, distanceM: d };
  }
  const radius = best.site.radius > 0 ? best.site.radius : 500;
  best.insideGeofence = best.distanceM <= radius;
  return best;
}

export function formatDistance(meters) {
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)}km` : `${meters}m`;
}
