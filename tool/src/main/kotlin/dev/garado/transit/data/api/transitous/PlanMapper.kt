package dev.garado.transit.data.api.transitous

import dev.garado.transit.data.api.transitous.models.EncodedPolylineDto
import dev.garado.transit.data.api.transitous.models.LegDto
import dev.garado.transit.data.api.transitous.models.PlaceDto
import dev.garado.transit.data.api.transitous.models.PlanApiResponse
import dev.garado.transit.models.TripLeg
import dev.garado.transit.models.TripPlan
import dev.garado.transit.models.TripStop
import dev.garado.transit.util.decodePolyline
import dev.garado.transit.util.encodePolyline
import java.time.Instant

fun PlanApiResponse.toTripPlans(): List<TripPlan> = itineraries.map { itinerary ->
    TripPlan(
        startTime = itinerary.startTime.toEpochSeconds(),
        endTime = itinerary.endTime.toEpochSeconds(),
        duration = itinerary.duration,
        legs = itinerary.legs.map { it.toTripLeg() },
    )
}

private fun LegDto.toTripLeg(): TripLeg = if (mode == "WALK") toWalkLeg() else toTransitLeg()

private fun LegDto.toWalkLeg(): TripLeg.Walk = TripLeg.Walk(
    startTime = startTime.toEpochSeconds(),
    endTime = endTime.toEpochSeconds(),
    duration = duration,
    distance = distance ?: 0.0,
    polyline = legGeometry?.toNormalizedPolyline() ?: "",
)

private fun LegDto.toTransitLeg(): TripLeg.Transit = TripLeg.Transit(
    startTime = startTime.toEpochSeconds(),
    endTime = endTime.toEpochSeconds(),
    duration = duration,
    routeName = routeShortName ?: routeLongName ?: mode,
    routeColor = routeColor,
    routeTextColor = routeTextColor,
    modeName = mode,
    headsign = headsign,
    nextDepartureTime = startTime.toEpochSeconds(),
    stops = intermediateStops.map { it.toTripStop() },
    shape = legGeometry?.toNormalizedPolyline(),
)

private fun PlaceDto.toTripStop() = TripStop(
    globalStopId = stopId ?: name,
    name = name,
    lat = lat,
    lon = lon,
)

/** Transitous encodes polylines at its own reported precision (6, sometimes 7); re-encode at 5 to match the app's convention. */
private fun EncodedPolylineDto.toNormalizedPolyline(): String = encodePolyline(decodePolyline(points, precision))

private fun String.toEpochSeconds(): Long = Instant.parse(this).epochSecond
