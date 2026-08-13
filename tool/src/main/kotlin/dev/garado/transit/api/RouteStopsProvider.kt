package dev.garado.transit.api

import dev.garado.transit.api.models.TripStop

/** Fetch the stops served by a route */
interface RouteStopsProvider {
    suspend fun stopsForRoute(globalRouteId: String): List<TripStop>
}
