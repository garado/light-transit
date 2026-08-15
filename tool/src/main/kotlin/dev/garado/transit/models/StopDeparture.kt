package dev.garado.transit.models

data class StopDeparture(
    val globalStopId: String,
    val globalRouteId: String,
    val routeName: String,
    val routeColor: String?,
    val routeTextColor: String?,
    val headsign: String?,
    val departureTime: Long,
    val isRealTime: Boolean,
)
