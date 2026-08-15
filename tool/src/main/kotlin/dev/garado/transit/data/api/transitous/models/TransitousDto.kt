/** 
 * DTOs for Transitous (MOTIS) routing API
 * https://redocly.github.io/redoc/?url=https://raw.githubusercontent.com/motis-project/motis/refs/tags/v2.10.2/openapi.yaml
 */

package dev.garado.transit.data.api.transitous.models

import kotlinx.serialization.Serializable

@Serializable
data class PlanApiResponse(
    val itineraries: List<ItineraryDto> = emptyList(),
)

@Serializable
data class ItineraryDto(
    /** ISO 8601 date-time */
    val startTime: String,
    /** ISO 8601 date-time */
    val endTime: String,
    /** journey duration in seconds */
    val duration: Long,
    val legs: List<LegDto> = emptyList(),
)

@Serializable
data class LegDto(
    /** e.g. "WALK", "BUS", "RAIL", "TRAM", "SUBWAY", ... */
    val mode: String,
    val from: PlaceDto,
    val to: PlaceDto,
    /** ISO 8601 date-time; leg departure time */
    val startTime: String,
    /** ISO 8601 date-time; leg arrival time */
    val endTime: String,
    /** leg duration in seconds */
    val duration: Long,
    /** non-transit legs only; distance traveled in meters */
    val distance: Double? = null,
    /** transit legs only */
    val headsign: String? = null,
    val routeShortName: String? = null,
    val routeLongName: String? = null,
    val routeColor: String? = null,
    val routeTextColor: String? = null,
    /** transit legs only; stops between [from] and [to] */
    val intermediateStops: List<PlaceDto> = emptyList(),
    val legGeometry: EncodedPolylineDto? = null,
)

@Serializable
data class PlaceDto(
    val name: String,
    val lat: Double,
    val lon: Double,
    val stopId: String? = null,
)

@Serializable
data class EncodedPolylineDto(
    /** Google-encoded polyline */
    val points: String,
)
