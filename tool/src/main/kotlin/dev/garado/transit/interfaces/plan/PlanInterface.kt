package dev.garado.transit.interfaces.plan

import dev.garado.transit.models.TripPlan

/** Backend for fetching transit trip plans between two points. */
interface PlanInterface {
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
