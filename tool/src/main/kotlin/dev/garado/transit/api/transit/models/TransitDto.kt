/** DTOs for Transit API based on transit-api.json */

package dev.garado.transit.api.transit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlanApiResponse(
    /** A list of results. It can be empty if no results are found. */
    val results: List<PlanResultDto> = emptyList(),
)

@Serializable
data class PlanResultDto(
    /** The expected start time in unix time */
    @SerialName("start_time") val startTime: Long,
    /** The expected end time in unix time */
    @SerialName("end_time") val endTime: Long,
    /** Travel duration in seconds */
    val duration: Long,
    val legs: List<LegDto> = emptyList(),
)

/** One leg of a trip. Walk legs carry [distance]/[polyline]; transit legs carry [departures]/[routes] */
@Serializable
data class LegDto(
    /** The mode of the current leg (e.g. "walk", "transit") */
    @SerialName("leg_mode") val legMode: String,
    /** The expected start time in unix time */
    @SerialName("start_time") val startTime: Long,
    /** The expected end time in unix time */
    @SerialName("end_time") val endTime: Long,
    /** Travel duration in seconds */
    val duration: Long,
    /** Total distance traveled of the leg. Walk legs only. */
    val distance: Double? = null,
    /** Shape of the traveled path provided in the encoded polyline format. Walk legs only. */
    val polyline: String? = null,
    /**
     * A list of departure options for the transit leg. Each departure contains a `plan_details`
     * object for more context and a reference to the `routes` array. Transit legs only.
     */
    val departures: List<DepartureDto>? = null,
    /**
     * Array of Route that can be used to travel that leg. The `plan_detail` object is provided
     * inside the itineraries for details specific to the plan. Transit legs only.
     */
    val routes: List<RouteDto>? = null,
)

@Serializable
data class DepartureDto(
    /** Departure time of the schedule item in UNIX time. If `is_real_time` is false, it will be equal to `scheduled_departure_time`. */
    @SerialName("departure_time") val departureTime: Long,
    /** Arrival time of the schedule item in UNIX time. If `is_real_time` is false, it will be equal to `scheduled_arrival_time`. */
    @SerialName("arrival_time") val arrivalTime: Long? = null,
    /** Departure time based on schedule information in UNIX time. */
    @SerialName("scheduled_departure_time") val scheduledDepartureTime: Long? = null,
    /** Arrival time based on schedule information in UNIX time. */
    @SerialName("scheduled_arrival_time") val scheduledArrivalTime: Long? = null,
    /** If this departure has been cancelled. Cancelled departures should either be crossed off or not shown at all in a UI. */
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    /** If the departure_time is based on real time data. */
    @SerialName("is_real_time") val isRealTime: Boolean = false,
    /** `wheelchair_accessible` of the corresponding trip, from the GTFS */
    @SerialName("wheelchair_accessible") val wheelchairAccessible: Int? = null,
)

@Serializable
data class RouteDto(
    /** A global route id: a string identifier that identifies a route across all of Transit's data. On a best-effort basis, the id will not change even across GTFS updates. */
    @SerialName("global_route_id") val globalRouteId: String,
    /** `route_short_name` from the GTFS */
    @SerialName("route_short_name") val routeShortName: String? = null,
    /** `route_long_name` from the GTFS */
    @SerialName("route_long_name") val routeLongName: String? = null,
    /** Route color as defined in GTFS */
    @SerialName("route_color") val routeColor: String? = null,
    /** Route text color as defined in GTFS, meant to be legible against [routeColor] */
    @SerialName("route_text_color") val routeTextColor: String? = null,
    /** Mode name for the given route. A human-readable string that more accurately represents what locals call the mode, e.g. "Métro" for Montreal's subway. */
    @SerialName("mode_name") val modeName: String? = null,
    val itineraries: List<ItineraryDto> = emptyList(),
)

@Serializable
data class ItineraryDto(
    /** The headsign from the GTFS. If a stop_headsign is provided in the GTFS, it will be provided here. */
    val headsign: String? = null,
    /** Direction id of the itinerary. In the majority of cases, the direction id will be the original one from the GTFS. */
    @SerialName("direction_id") val directionId: Int? = null,
    /** Encoded polyline for this itinerary's *entire* geometry, not just the segment ridden in this leg. Prefer [PlanDetailsDto.planShape] when present. */
    val shape: String? = null,
    /** List of stops of this itinerary. */
    val stops: List<StopDto> = emptyList(),
    /** Only present inside the `plan` call's response. */
    @SerialName("plan_details") val planDetails: PlanDetailsDto? = null,
)

@Serializable
data class PlanDetailsDto(
    /** Encoded polyline of just the segment of the itinerary between `start_stop_offset` and `end_stop_offset`, i.e. what was actually ridden in this leg. */
    @SerialName("plan_shape") val planShape: String? = null,
    /** Index into the itinerary's `stops` list where the leg starts (boarding stop). */
    @SerialName("start_stop_offset") val startStopOffset: Int? = null,
    /** Index into the itinerary's `stops` list where the leg ends (alighting stop). */
    @SerialName("end_stop_offset") val endStopOffset: Int? = null,
)

@Serializable
data class StopDto(
    /** A global stop id: a string identifier that identifies a stop across all of Transit's data. On a best-effort basis, the id will not change even across GTFS updates. */
    @SerialName("global_stop_id") val globalStopId: String,
    /** Stop name from the GTFS */
    @SerialName("stop_name") val stopName: String,
    @SerialName("stop_lat") val stopLat: Double,
    @SerialName("stop_lon") val stopLon: Double,
)
