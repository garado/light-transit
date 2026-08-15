package dev.garado.transit.interfaces.routestops

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.models.TripStop
import dev.garado.transit.data.gtfs.local.GtfsDatabaseHolder
import dev.garado.transit.data.gtfs.local.parseGtfsGlobalRouteId
import dev.garado.transit.data.gtfs.local.routeShape
import dev.garado.transit.data.gtfs.local.toGroupedTripStop

/** Finds the stops served by a route from downloaded GTFS data */
class GtfsLocalRouteStopsProvider(lightContext: SealedLightContext) : RouteStopsInterface {
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
