package dev.garado.transit.interfaces.nearbyroutes

import dev.garado.transit.api.models.TripRoute

/** Backend for fetching transit routes near a location */
interface NearbyRoutesInterface {
    suspend fun nearbyRoutes(lat: Double, lon: Double): List<TripRoute>
}
