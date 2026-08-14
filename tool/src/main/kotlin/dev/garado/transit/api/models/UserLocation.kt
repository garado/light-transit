package dev.garado.transit.api.models

data class UserLocation(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Double,
    val altitudeMeters: Double,
    val speedMetersPerSecond: Double,
    /** epoch milliseconds */
    val timestamp: Long,
    /** positioning technology (LightOS reports "gnss") (probably not needed, but whatever) */
    val technology: String,
)
