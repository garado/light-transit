package dev.garado.transit.api

import dev.garado.transit.api.models.TripStop

/** Backend for fetching transit stops near a location */
interface NearbyStopsProvider {
    suspend fun nearbyStops(lat: Double, lon: Double): List<TripStop>
}
