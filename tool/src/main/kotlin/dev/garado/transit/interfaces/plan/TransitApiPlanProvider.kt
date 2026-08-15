package dev.garado.transit.interfaces.plan

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.api.models.TripPlan
import dev.garado.transit.api.transitapi.TransitApiClient
import dev.garado.transit.api.transitapi.TransitEndpoint
import dev.garado.transit.api.transitapi.models.PlanApiResponse
import dev.garado.transit.api.transitapi.toTripPlans

class TransitApiPlanProvider(
    lightContext: SealedLightContext,
    private val client: TransitApiClient = TransitApiClient(lightContext),
) : PlanProvider {

    /**
     * https://api-doc.transitapp.com/v4.html#GET/v4/public/plan
     *
     * The API only honors [leaveTime] if both [leaveTime] and [arrivalTime] are provided.
     */
    override suspend fun plan(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        leaveTime: Long?,
        arrivalTime: Long?,
    ): List<TripPlan> {
        val timeParams = when {
            leaveTime != null -> mapOf("leave_time" to leaveTime)
            arrivalTime != null -> mapOf("arrival_time" to arrivalTime)
            else -> mapOf("leave_time" to System.currentTimeMillis() / 1000)
        }
        val response: PlanApiResponse? = client.request(
            endpoint = TransitEndpoint.PLAN,
            params = mapOf(
                "from_lat" to fromLat,
                "from_lon" to fromLon,
                "to_lat" to toLat,
                "to_lon" to toLon,
                "mode" to "transit",
            ) + timeParams,
        )
        return response?.toTripPlans() ?: emptyList()
    }
}
