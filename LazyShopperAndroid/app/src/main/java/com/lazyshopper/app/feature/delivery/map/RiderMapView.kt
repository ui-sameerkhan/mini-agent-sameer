package com.lazyshopper.app.feature.delivery.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Minimal, read-only rider-tracking map: a pin for the rider's own live position and one for
 * the customer's drop location (if known). No routing/road-line — that's the customer-side
 * tracking screen's job; the rider just needs a quick "am I close" glance.
 */
@Composable
fun RiderMapView(
    riderLat: Double?,
    riderLng: Double?,
    customerLat: Double?,
    customerLng: Double?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val mapView = remember {
        configureOsmdroid(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(20.5937, 78.9629)) // India centroid fallback until a fix arrives
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            view.overlays.clear()
            val points = mutableListOf<GeoPoint>()

            if (riderLat != null && riderLng != null) {
                val point = GeoPoint(riderLat, riderLng)
                points += point
                view.overlays.add(
                    Marker(view).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "You"
                    },
                )
            }
            if (customerLat != null && customerLng != null) {
                val point = GeoPoint(customerLat, customerLng)
                points += point
                view.overlays.add(
                    Marker(view).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Customer"
                    },
                )
            }

            when (points.size) {
                1 -> view.controller.setCenter(points[0])
                2 -> view.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 100)
            }
            view.invalidate()
        },
    )
}

private var osmdroidConfigured = false

private fun configureOsmdroid(context: Context) {
    if (osmdroidConfigured) return
    Configuration.getInstance().load(
        context.applicationContext,
        context.applicationContext.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE),
    )
    Configuration.getInstance().userAgentValue = context.applicationContext.packageName
    osmdroidConfigured = true
}
