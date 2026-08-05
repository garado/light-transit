package dev.garado.transit.route

import dev.garado.transit.map.LatLon

/**
 * Decodes polyline into lat/lon points
 * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
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
