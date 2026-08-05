package dev.garado.transit.route

import androidx.compose.ui.graphics.Color
import dev.garado.transit.api.transit.models.TripLeg
import dev.garado.transit.api.transit.models.TripPlan
import dev.garado.transit.map.LatLon
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

/** Simple average of every point across all overlays; good enough to center a map on a route. */
fun List<MapOverlay.Polyline>.centroid(): LatLon {
    val allPoints = flatMap { it.points }
    if (allPoints.isEmpty()) return LatLon(lat = 0.0, lon = 0.0)
    return LatLon(
        lat = allPoints.sumOf { it.lat } / allPoints.size,
        lon = allPoints.sumOf { it.lon } / allPoints.size,
    )
}
