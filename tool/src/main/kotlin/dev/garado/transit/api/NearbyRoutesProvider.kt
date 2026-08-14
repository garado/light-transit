package dev.garado.transit.api

import dev.garado.transit.api.models.TripRoute

/** Backend for fetching transit routes near a location */
interface NearbyRoutesProvider {
    suspend fun nearbyRoutes(lat: Double, lon: Double): List<TripRoute>
}
