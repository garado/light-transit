package dev.garado.transit.interfaces.stopdepartures

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.models.StopDeparture
import dev.garado.transit.data.api.transitapi.TransitApiClient
import dev.garado.transit.data.api.transitapi.TransitEndpoint
import dev.garado.transit.data.api.transitapi.models.StopDeparturesApiResponse
import dev.garado.transit.data.api.transitapi.toStopDepartures

class TransitApiStopDeparturesProvider(
    lightContext: SealedLightContext,
    private val client: TransitApiClient = TransitApiClient(lightContext),
) : StopDeparturesInterface {

    /** https://api-doc.transitapp.com/v4.html#GET/v4/public/stop_departures */
    override suspend fun departures(globalStopIds: List<String>, maxDepartures: Int): Map<String, List<StopDeparture>> {
        if (globalStopIds.isEmpty()) return emptyMap()
        val response: StopDeparturesApiResponse? = client.request(
            endpoint = TransitEndpoint.STOP_DEPARTURES,
            params = mapOf(
                "global_stop_ids" to globalStopIds.joinToString(","),
                "max_num_departures" to maxDepartures.coerceIn(1, 10),
                "merge_platform_stops" to true,
                "exclude_terminal_arrivals" to true,
            ),
        )
        return response?.toStopDepartures() ?: emptyMap()
    }
}
