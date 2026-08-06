package dev.garado.transit.map

data class LatLon(val lat: Double, val lon: Double)

data class LatLonBounds(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double) {
    val center: LatLon get() = LatLon(lat = (minLat + maxLat) / 2, lon = (minLon + maxLon) / 2)
}
