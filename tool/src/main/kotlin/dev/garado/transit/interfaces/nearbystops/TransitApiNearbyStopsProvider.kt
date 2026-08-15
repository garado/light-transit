package dev.garado.transit.interfaces.nearbystops

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.api.models.TripStop
import dev.garado.transit.api.transitapi.TransitApiClient
import dev.garado.transit.api.transitapi.TransitEndpoint
import dev.garado.transit.api.transitapi.models.NearbyStopsApiResponse
import dev.garado.transit.api.transitapi.toTripStop

class TransitApiNearbyStopsProvider(
    lightContext: SealedLightContext,
    private val client: TransitApiClient = TransitApiClient(lightContext),
) : NearbyStopsInterface {

    /**
     * https://api-doc.transitapp.com/v4.html#GET/v4/public/nearby_stops
     * Stations with multiple platforms are grouped under the parent station
     */
    override suspend fun nearbyStops(lat: Double, lon: Double): List<TripStop> {
        val response: NearbyStopsApiResponse? = client.request(
            endpoint = TransitEndpoint.NEARBY_STOPS,
            params = mapOf("lat" to lat, "lon" to lon),
        )
        return response?.stops
            ?.distinctBy { it.parentStation?.globalStopId ?: it.globalStopId }
            ?.map { it.toTripStop() }
            ?: emptyList()
    }
}
