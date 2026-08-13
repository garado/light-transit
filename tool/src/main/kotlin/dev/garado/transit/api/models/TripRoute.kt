package dev.garado.transit.api.models

data class TripRoute(
    val globalRouteId: String,
    val name: String,
    val color: String?,
    val textColor: String?,
    /** encoded polyline for the route's path, or null if unavailable */
    val shape: String? = null,
)
