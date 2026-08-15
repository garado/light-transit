package dev.garado.transit.data.api.transitous

import dev.garado.transit.data.api.transitous.models.LegDto
import dev.garado.transit.data.api.transitous.models.PlaceDto
import dev.garado.transit.data.api.transitous.models.PlanApiResponse
import dev.garado.transit.models.TripLeg
import dev.garado.transit.models.TripPlan
import dev.garado.transit.models.TripStop
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
    polyline = legGeometry?.points ?: "",
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
    shape = legGeometry?.points,
)

private fun PlaceDto.toTripStop() = TripStop(
    globalStopId = stopId ?: name,
    name = name,
    lat = lat,
    lon = lon,
)

private fun String.toEpochSeconds(): Long = Instant.parse(this).epochSecond
