package dev.garado.transit.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Something drawn on top of the map's tiles (independent of tile format) */
sealed interface MapOverlay {
    data class Polyline(val points: List<LatLon>, val color: Color, val widthDp: Dp = 3.dp) : MapOverlay
}
