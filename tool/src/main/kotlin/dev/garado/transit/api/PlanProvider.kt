package dev.garado.transit.api

import dev.garado.transit.api.models.TripPlan

/** Backend for fetching transit trip plans between two points. */
interface PlanProvider {
    /**
     * At most one of [leaveTime]/[arrivalTime] should be set. If neither is set, defaults to
     * leaving now.
     */
    suspend fun plan(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        leaveTime: Long? = null,
        arrivalTime: Long? = null,
    ): List<TripPlan>
}
