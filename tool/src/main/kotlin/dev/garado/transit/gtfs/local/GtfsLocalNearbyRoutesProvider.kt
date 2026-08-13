package dev.garado.transit.gtfs.local

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.api.NearbyRoutesProvider
import dev.garado.transit.api.models.TripRoute
import dev.garado.transit.map.LatLon
import dev.garado.transit.route.encodePolyline
import kotlin.math.cos

private const val SEARCH_RADIUS_METERS = 800.0
private const val METERS_PER_DEGREE_LAT = 111_320.0

/** Finds routes serving stops near a location from downloaded GTFS data */
class GtfsLocalNearbyRoutesProvider(lightContext: SealedLightContext) : NearbyRoutesProvider {
    private val stopsDao = GtfsDatabaseHolder.get(lightContext).gtfsStopsDao()
    private val scheduleDao = GtfsDatabaseHolder.get(lightContext).gtfsScheduleDao()

    override suspend fun nearbyRoutes(lat: Double, lon: Double): List<TripRoute> {
        val latDelta = SEARCH_RADIUS_METERS / METERS_PER_DEGREE_LAT
        val metersPerDegreeLon = METERS_PER_DEGREE_LAT * cos(Math.toRadians(lat)).coerceAtLeast(0.01)
        val lonDelta = SEARCH_RADIUS_METERS / metersPerDegreeLon

        val nearbyStops = stopsDao.nearby(
            minLat = lat - latDelta,
            maxLat = lat + latDelta,
            minLon = lon - lonDelta,
            maxLon = lon + lonDelta,
        )

        return nearbyStops
            .groupBy { it.sourceId }
            .flatMap { (sourceId, entities) ->
                scheduleDao.routesServing(sourceId, entities.map { it.stopId }).map { route ->
                    route.toTripRoute().copy(shape = routeShape(sourceId, route.routeId))
                }
            }
    }

    private suspend fun routeShape(sourceId: Long, routeId: String): String? {
        val shapeId = scheduleDao.representativeShapeId(sourceId, routeId) ?: return null
        val points = scheduleDao.shapePoints(sourceId, shapeId)
        if (points.isEmpty()) return null
        return encodePolyline(points.map { LatLon(lat = it.lat, lon = it.lon) })
    }
}
