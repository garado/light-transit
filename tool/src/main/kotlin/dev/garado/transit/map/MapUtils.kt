package dev.garado.transit.map

import kotlin.math.cos
import kotlin.math.tan
import kotlin.math.ln
import kotlin.math.PI

/** Standard Web Mercator "slippy map" tile size (pixels per tile at any zoom). */
const val TILE_SIZE = 256.0

/** Fractional (x, y) tile coordinates for (lat, lon) at the given integer zoom. */
fun lonLatToTileFraction(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
    val n = Math.pow(2.0, zoom.toDouble())
    val x = (lon + 180.0) / 360.0 * n
    val latRad = Math.toRadians(lat)
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
    return x to y
}

/**
 * Ground resolution (meters/pixel) at [lat] and [zoom] -- [zoom] may be fractional (e.g. mid-pinch,
 * between two integer tile zooms); the formula is continuous, so there's no need to round it first.
 */
fun metersPerPixel(lat: Double, zoom: Double): Double {
    val earthCircumferenceMeters = 40_075_016.686
    val latRad = Math.toRadians(lat)
    return (earthCircumferenceMeters * cos(latRad)) / (TILE_SIZE * Math.pow(2.0, zoom))
}

/** Integer-zoom convenience overload -- see the [Double] version for the actual formula. */
fun metersPerPixel(lat: Double, zoom: Int): Double = metersPerPixel(lat, zoom.toDouble())
