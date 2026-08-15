package dev.garado.transit.interfaces.plan

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.data.api.transitous.TransitousClient
import dev.garado.transit.data.api.transitous.TransitousEndpoint
import dev.garado.transit.data.api.transitous.models.PlanApiResponse
import dev.garado.transit.data.api.transitous.toTripPlans
import dev.garado.transit.models.TripPlan
import java.time.Instant

class TransitousPlanProvider(
    lightContext: SealedLightContext,
    private val client: TransitousClient = TransitousClient(lightContext),
) : PlanInterface {

    override suspend fun plan(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        leaveTime: Long?,
        arrivalTime: Long?,
    ): List<TripPlan> {
        val (time, arriveBy) = when {
            arrivalTime != null -> arrivalTime to true
            leaveTime != null -> leaveTime to false
            else -> null to false
        }
        val response: PlanApiResponse? = client.request(
            endpoint = TransitousEndpoint.PLAN,
            params = buildMap {
                put("fromPlace", "$fromLat,$fromLon")
                put("toPlace", "$toLat,$toLon")
                put("arriveBy", arriveBy)
                if (time != null) put("time", Instant.ofEpochSecond(time).toString())
            },
        )
        return response?.toTripPlans() ?: emptyList()
    }
}
