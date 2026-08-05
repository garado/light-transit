package dev.garado.transit.api.transit.models

data class TripPlan(
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val legs: List<TripLeg>,
)

sealed interface TripLeg {
    val startTime: Long
    val endTime: Long
    val duration: Long

    data class Walk(
        override val startTime: Long,
        override val endTime: Long,
        override val duration: Long,
        val distance: Double,
        val polyline: String,
    ) : TripLeg

    data class Transit(
        override val startTime: Long,
        override val endTime: Long,
        override val duration: Long,
        val routeName: String,
        val routeColor: String?,
        val headsign: String?,
        val nextDepartureTime: Long,
        val stops: List<TripStop>,
        val shape: String?,
    ) : TripLeg
}

data class TripStop(
    val globalStopId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
)
