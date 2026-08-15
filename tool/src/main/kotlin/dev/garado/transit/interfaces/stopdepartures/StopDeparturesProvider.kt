package dev.garado.transit.interfaces.stopdepartures

import dev.garado.transit.api.models.StopDeparture

/** Backend for fetching upcoming departures for a batch of stops at once. */
interface StopDeparturesProvider {
    suspend fun departures(globalStopIds: List<String>, maxDepartures: Int = 10): Map<String, List<StopDeparture>>
}
