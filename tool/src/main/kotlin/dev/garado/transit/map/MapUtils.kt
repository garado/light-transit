/**
 * Map view utilities.
 */

package dev.garado.transit.map

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.tan
import kotlin.math.ln
import kotlin.math.PI

const val TILE_SIZE = 256.0

/** Fractional (x, y) tile coordinates for (lat, lon) at the given integer zoom */
fun lonLatToTileFraction(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
    val n = Math.pow(2.0, zoom.toDouble())
    val x = (lon + 180.0) / 360.0 * n
    val latRad = Math.toRadians(lat)
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
    return x to y
}

/** Ground resolution (meters/pixel) at [lat] and [zoom] */
fun metersPerPixel(lat: Double, zoom: Double): Double {
    val earthCircumferenceMeters = 40_075_016.686
    val latRad = Math.toRadians(lat)
    return (earthCircumferenceMeters * cos(latRad)) / (TILE_SIZE * Math.pow(2.0, zoom))
}

/** Integer-zoom convenience overload */
fun metersPerPixel(lat: Double, zoom: Int): Double = metersPerPixel(lat, zoom.toDouble())

/** Screen offset for a fractional tile-space point, relative to the projection origin */
fun tileFractionToOffset(fracX: Double, fracY: Double, liveFracX: Double, liveFracY: Double, scale: Float): Offset {
    val offsetX = ((fracX - liveFracX) * TILE_SIZE).toFloat() * scale
    val offsetY = ((fracY - liveFracY) * TILE_SIZE).toFloat() * scale
    return Offset(offsetX, offsetY)
}

/** Screen offset (relative to the view's projection origin) for a (lat, lon) point */
fun lonLatToOffset(lat: Double, lon: Double, zoom: Int, liveFracX: Double, liveFracY: Double, scale: Float): Offset {
    val (fracX, fracY) = lonLatToTileFraction(lat, lon, zoom)
    return tileFractionToOffset(fracX, fracY, liveFracX, liveFracY, scale)
}
