package dev.garado.transit.interfaces.nearbystops

import dev.garado.transit.models.TripStop

/** Backend for fetching transit stops near a location */
interface NearbyStopsInterface {
    suspend fun nearbyStops(lat: Double, lon: Double): List<TripStop>
}
