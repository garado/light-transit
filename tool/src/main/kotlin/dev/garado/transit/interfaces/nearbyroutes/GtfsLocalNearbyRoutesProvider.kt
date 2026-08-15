package dev.garado.transit.interfaces.nearbyroutes

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.models.TripRoute
import dev.garado.transit.data.gtfs.local.GtfsDatabaseHolder
import dev.garado.transit.data.gtfs.local.routeShape
import dev.garado.transit.data.gtfs.local.toTripRoute
import kotlin.math.cos

private const val SEARCH_RADIUS_METERS = 800.0
private const val METERS_PER_DEGREE_LAT = 111_320.0

/** Finds routes serving stops near a location from downloaded GTFS data */
class GtfsLocalNearbyRoutesProvider(lightContext: SealedLightContext) : NearbyRoutesInterface {
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
                    route.toTripRoute().copy(shape = scheduleDao.routeShape(sourceId, route.routeId))
                }
            }
    }
}
