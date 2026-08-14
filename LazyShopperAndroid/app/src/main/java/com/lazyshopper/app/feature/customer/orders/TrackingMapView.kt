package com.lazyshopper.app.feature.customer.orders

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
 * Customer-facing order tracking map: a pin for the customer's own drop location (📍) and one
 * for the delivery partner's live position (🛵). Mirrors `feature.delivery.map.RiderMapView`'s
 * setup but from the customer's point of view, so the marker titles read correctly here.
 */
@Composable
fun TrackingMapView(
    customerLat: Double?,
    customerLng: Double?,
    partnerLat: Double?,
    partnerLng: Double?,
    partnerName: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val mapView = remember {
        configureOsmdroidTracking(context)
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

            if (customerLat != null && customerLng != null) {
                val point = GeoPoint(customerLat, customerLng)
                points += point
                view.overlays.add(
                    Marker(view).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "📍 Your delivery location"
                    },
                )
            }
            if (partnerLat != null && partnerLng != null) {
                val point = GeoPoint(partnerLat, partnerLng)
                points += point
                view.overlays.add(
                    Marker(view).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "🛵 ${partnerName ?: "Delivery partner"}"
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

private var osmdroidTrackingConfigured = false

private fun configureOsmdroidTracking(context: Context) {
    if (osmdroidTrackingConfigured) return
    Configuration.getInstance().load(
        context.applicationContext,
        context.applicationContext.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE),
    )
    Configuration.getInstance().userAgentValue = context.applicationContext.packageName
    osmdroidTrackingConfigured = true
}
