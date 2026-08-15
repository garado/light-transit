/**
 * Polyline utils
 * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */

package dev.garado.transit.util

import dev.garado.transit.models.LatLon

/** Decodes polyline into lat/lon points */
fun decodePolyline(encoded: String): List<LatLon> {
    val points = mutableListOf<LatLon>()
    var index = 0
    var lat = 0
    var lon = 0

    while (index < encoded.length) {
        val (deltaLat, indexAfterLat) = decodeSignedValue(encoded, index)
        lat += deltaLat
        index = indexAfterLat

        val (deltaLon, indexAfterLon) = decodeSignedValue(encoded, index)
        lon += deltaLon
        index = indexAfterLon

        points.add(LatLon(lat = lat / 1e5, lon = lon / 1e5))
    }
    return points
}

/** Decodes one zigzag-encoded varint starting at [startIndex]; returns (value, indexAfter) */
private fun decodeSignedValue(encoded: String, startIndex: Int): Pair<Int, Int> {
    var index = startIndex
    var shift = 0
    var result = 0
    var b: Int
    do {
        b = encoded[index++].code - 63
        result = result or ((b and 0x1f) shl shift)
        shift += 5
    } while (b >= 0x20)
    val value = if (result and 1 != 0) (result shr 1).inv() else (result shr 1)
    return value to index
}

/** Inverse of [decodePolyline] */
fun encodePolyline(points: List<LatLon>): String {
    val result = StringBuilder()
    var lastLat = 0
    var lastLon = 0
    for (point in points) {
        // each point is delta-encoded against the previous one, not absolute
        val lat = Math.round(point.lat * 1e5).toInt()
        val lon = Math.round(point.lon * 1e5).toInt()
        encodeSignedValue(lat - lastLat, result)
        encodeSignedValue(lon - lastLon, result)
        lastLat = lat
        lastLon = lon
    }
    return result.toString()
}

/** Zigzag-encodes [value] into 5-bit chunks, inverse of [decodeSignedValue] */
private fun encodeSignedValue(value: Int, result: StringBuilder) {
    // zigzag: map signed -> unsigned so small magnitudes (either sign) stay small
    var v = if (value < 0) (value shl 1).inv() else (value shl 1)
    while (v >= 0x20) {
        // continuation bit (0x20) set while more chunks remain, +63 shifts into printable ASCII
        result.append(((0x20 or (v and 0x1f)) + 63).toChar())
        v = v shr 5
    }
    result.append((v + 63).toChar())
}
