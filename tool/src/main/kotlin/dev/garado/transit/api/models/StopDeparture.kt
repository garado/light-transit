package dev.garado.transit.api.models

data class StopDeparture(
    val globalStopId: String,
    val routeName: String,
    val routeColor: String?,
    val routeTextColor: String?,
    val headsign: String?,
    val departureTime: Long,
    val isRealTime: Boolean,
)
