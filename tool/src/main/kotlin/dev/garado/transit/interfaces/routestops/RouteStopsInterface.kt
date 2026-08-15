package dev.garado.transit.interfaces.routestops

import dev.garado.transit.api.models.TripStop

/** Fetch the stops served by a route */
interface RouteStopsInterface {
    suspend fun stopsForRoute(globalRouteId: String): List<TripStop>
}
