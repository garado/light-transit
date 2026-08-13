package dev.garado.transit.gtfs.local

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.api.NearbyStopsProvider
import dev.garado.transit.api.models.TripStop
import kotlin.math.cos

private const val SEARCH_RADIUS_METERS = 800.0
private const val MAX_RESULTS = 30
private const val METERS_PER_DEGREE_LAT = 111_320.0

/** Finds nearby stops from downloaded GTFS stops.txt data */
class GtfsLocalNearbyStopsProvider(lightContext: SealedLightContext) : NearbyStopsProvider {
    private val dao = GtfsStopsDatabaseHolder.get(lightContext).gtfsStopsDao()

    override suspend fun nearbyStops(lat: Double, lon: Double): List<TripStop> {
        val latDelta = SEARCH_RADIUS_METERS / METERS_PER_DEGREE_LAT
        val metersPerDegreeLon = METERS_PER_DEGREE_LAT * cos(Math.toRadians(lat)).coerceAtLeast(0.01)
        val lonDelta = SEARCH_RADIUS_METERS / metersPerDegreeLon

        return dao.nearby(
            minLat = lat - latDelta,
            maxLat = lat + latDelta,
            minLon = lon - lonDelta,
            maxLon = lon + lonDelta,
        )
            // collapse stops with shared parent_station into a single marker
            .groupBy { entity -> entity.sourceId to (entity.parentStation ?: "stop:${entity.stopId}") }
            .map { (_, entities) -> entities.toGroupedTripStop() }
            .sortedBy { squaredDistance(it.lat, it.lon, lat, lon) }
            .take(MAX_RESULTS)
    }
}

private fun squaredDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = lat1 - lat2
    val dLon = lon1 - lon2
    return dLat * dLat + dLon * dLon
}
