package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.Site
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double)

data class NearestSite(val site: Site, val distanceM: Long) {
    val insideGeofence: Boolean get() = distanceM <= (if (site.radius > 0) site.radius else 500)
}

object Geo {
    private const val EARTH_RADIUS_M = 6371000.0

    /** Haversine distance in meters, rounded — identical formula to the web app's distM(). */
    fun distanceMeters(a: LatLng, b: LatLng): Long {
        fun toRad(x: Double) = x * PI / 180
        val dLat = toRad(b.lat - a.lat)
        val dLng = toRad(b.lng - a.lng)
        val sinLat = sin(dLat / 2)
        val sinLng = sin(dLng / 2)
        val h = sinLat * sinLat + cos(toRad(a.lat)) * cos(toRad(b.lat)) * sinLng * sinLng
        return (2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))).roundToLong()
    }

    fun nearestSite(point: LatLng, sites: List<Site>): NearestSite? {
        if (sites.isEmpty()) return null
        return sites
            .map { NearestSite(it, distanceMeters(point, LatLng(it.lat, it.lng))) }
            .minByOrNull { it.distanceM }
    }

    /** "480m" below 1000m, "1.2km" at/above — matches the web app's distance display rule. */
    fun formatDistance(meters: Long): String =
        if (meters >= 1000) "%.1fkm".format(meters / 1000.0) else "${meters}m"
}
