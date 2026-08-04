package dev.garado.transit.search

data class LocationResult(val label: String, val lat: Double, val lon: Double)

fun fakeLocationSearch(query: String): List<LocationResult> {
    if (query.isBlank()) return emptyList()
    return listOf(
        LocationResult("$query Ave", lat = 0.0, lon = 0.0),
        LocationResult("$query St", lat = 0.0, lon = 0.0),
        LocationResult("$query Blvd", lat = 0.0, lon = 0.0),
    )
}
