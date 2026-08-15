package dev.garado.transit.route

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.garado.transit.api.models.TripLeg
import dev.garado.transit.api.models.TripPlan
import dev.garado.transit.map.LatLon
import dev.garado.transit.map.LatLonBounds
import dev.garado.transit.map.MapOverlay
import dev.garado.transit.parseHexColor

/** Maps a trip's legs to drawable map overlays: walk legs in [walkLegColor], transit legs in their route color. */
fun TripPlan.toOverlays(walkLegColor: Color): List<MapOverlay.Polyline> = legs.mapNotNull { leg ->
    when (leg) {
        is TripLeg.Walk -> MapOverlay.Polyline(points = decodePolyline(leg.polyline), color = walkLegColor)
        is TripLeg.Transit -> leg.shape?.let { shape ->
            MapOverlay.Polyline(
                points = decodePolyline(shape),
                color = parseHexColor(leg.routeColor, fallback = walkLegColor),
            )
        }
    }
}

/** Draw one marker per stop for the whole trip */
fun TripPlan.stopMarkers(
    color: Color,
    highlightedStopId: String? = null,
    highlightColor: Color = color,
    radiusDp: Dp = 3.dp,
    highlightRadiusDp: Dp = 6.dp,
): List<MapOverlay.Marker> = legs
    .filterIsInstance<TripLeg.Transit>()
    .flatMap { it.stops }
    .distinctBy { it.globalStopId }
    .map { stop ->
        val isHighlighted = stop.globalStopId == highlightedStopId
        MapOverlay.Marker(
            point = LatLon(lat = stop.lat, lon = stop.lon),
            color = if (isHighlighted) highlightColor else color,
            radiusDp = if (isHighlighted) highlightRadiusDp else radiusDp,
        )
    }
    // draw the highlighted marker last so it isn't covered by stops clustered near it
    .sortedBy { it.radiusDp }

/** Simple average of every point across all overlays (for centering a map on a route) */
fun List<MapOverlay.Polyline>.centroid(): LatLon {
    val allPoints = flatMap { it.points }
    if (allPoints.isEmpty()) return LatLon(lat = 0.0, lon = 0.0)
    return LatLon(
        lat = allPoints.sumOf { it.lat } / allPoints.size,
        lon = allPoints.sumOf { it.lon } / allPoints.size,
    )
}

/** Bounding box of every point across all overlays (used to zoom a map to fit the whole route) */
fun List<MapOverlay.Polyline>.boundingBox(): LatLonBounds? {
    val allPoints = flatMap { it.points }
    if (allPoints.isEmpty()) return null
    return LatLonBounds(
        minLat = allPoints.minOf { it.lat },
        maxLat = allPoints.maxOf { it.lat },
        minLon = allPoints.minOf { it.lon },
        maxLon = allPoints.maxOf { it.lon },
    )
}
