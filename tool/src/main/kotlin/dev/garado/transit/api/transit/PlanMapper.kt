package dev.garado.transit.api.transit

import dev.garado.transit.api.transit.models.DepartureDto
import dev.garado.transit.api.transit.models.ItineraryDto
import dev.garado.transit.api.transit.models.LegDto
import dev.garado.transit.api.transit.models.PlanApiResponse
import dev.garado.transit.api.transit.models.PlanResultDto
import dev.garado.transit.api.transit.models.RouteDto
import dev.garado.transit.api.transit.models.StopDto
import dev.garado.transit.api.transit.models.TripLeg
import dev.garado.transit.api.transit.models.TripPlan
import dev.garado.transit.api.transit.models.TripStop

fun PlanApiResponse.toTripPlans(): List<TripPlan> = results.map(PlanResultDto::toTripPlan)

private fun PlanResultDto.toTripPlan(): TripPlan = TripPlan(
    startTime = startTime,
    endTime = endTime,
    duration = duration,
    legs = legs.mapNotNull(LegDto::toTripLeg),
)

private fun LegDto.toTripLeg(): TripLeg? = when (legMode) {
    "walk" -> toWalkLeg()
    "transit" -> toTransitLeg()
    else -> null
}

private fun LegDto.toWalkLeg(): TripLeg.Walk? {
    val legDistance = distance ?: return null
    val legPolyline = polyline ?: return null
    return TripLeg.Walk(startTime, endTime, duration, legDistance, legPolyline)
}

private fun LegDto.toTransitLeg(): TripLeg.Transit? {
    val route = routes?.firstOrNull() ?: return null
    val departure = departures?.firstOrNull() ?: return null
    val itinerary = route.itineraries.firstOrNull()
    return TripLeg.Transit(
        startTime = startTime,
        endTime = endTime,
        duration = duration,
        routeName = route.routeShortName ?: route.routeLongName ?: route.globalRouteId,
        routeColor = route.routeColor,
        routeTextColor = route.routeTextColor,
        headsign = itinerary?.headsign,
        nextDepartureTime = departure.departureTime,
        stops = itinerary?.riddenStops() ?: emptyList(),
        shape = itinerary?.planDetails?.planShape ?: itinerary?.shape,
    )
}

/** [ItineraryDto.stops] covers every single stop; slice only the ones we need */
private fun ItineraryDto.riddenStops(): List<TripStop> {
    val start = planDetails?.startStopOffset
    val end = planDetails?.endStopOffset
    val slice = if (start != null && end != null && start in stops.indices && end in stops.indices) {
        stops.subList(start, end + 1)
    } else {
        stops
    }
    return slice.map(StopDto::toTripStop)
}

private fun StopDto.toTripStop() = TripStop(
    globalStopId = globalStopId,
    name = stopName,
    lat = stopLat,
    lon = stopLon,
)
