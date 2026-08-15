package dev.garado.transit.models

data class LocationResult(
    val title: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val displayName: String? = null,
)
