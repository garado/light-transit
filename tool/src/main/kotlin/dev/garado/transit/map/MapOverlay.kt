package dev.garado.transit.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Something drawn on top of the map's tiles (independent of tile format) */
sealed interface MapOverlay {
    data class Polyline(val points: List<LatLon>, val color: Color, val widthDp: Dp = 3.dp) : MapOverlay
    data class Marker(val point: LatLon, val color: Color, val radiusDp: Dp = 3.dp, val id: String? = null) : MapOverlay
}
