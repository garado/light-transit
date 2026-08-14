package dev.garado.transit.gtfs.local

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.api.RouteStopsProvider
import dev.garado.transit.api.models.TripStop

/** Finds the stops served by a route from downloaded GTFS data */
class GtfsLocalRouteStopsProvider(lightContext: SealedLightContext) : RouteStopsProvider {
    private val scheduleDao = GtfsDatabaseHolder.get(lightContext).gtfsScheduleDao()

    override suspend fun stopsForRoute(globalRouteId: String): List<TripStop> {
        val (sourceId, routeId) = parseGtfsGlobalRouteId(globalRouteId) ?: return emptyList()
        val tripId = scheduleDao.representativeTripId(sourceId, routeId) ?: return emptyList()
        return scheduleDao.stopsForTrip(sourceId, tripId)
            // collapse stops with shared parent_station into a single marker
            .groupBy { entity -> entity.parentStation ?: "stop:${entity.stopId}" }
            .map { (_, entities) -> entities.toGroupedTripStop() }
    }

    suspend fun shapeForRoute(globalRouteId: String): String? {
        val (sourceId, routeId) = parseGtfsGlobalRouteId(globalRouteId) ?: return null
        return scheduleDao.routeShape(sourceId, routeId)
    }
}
