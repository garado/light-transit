package dev.garado.transit.api.transitapi

import dev.garado.transit.models.StopDeparture
import dev.garado.transit.models.TripLeg
import dev.garado.transit.models.TripPlan
import dev.garado.transit.models.TripStop
import dev.garado.transit.api.transitapi.models.DepartureDto
import dev.garado.transit.api.transitapi.models.ItineraryDto
import dev.garado.transit.api.transitapi.models.LegDto
import dev.garado.transit.api.transitapi.models.PlanApiResponse
import dev.garado.transit.api.transitapi.models.PlanResultDto
import dev.garado.transit.api.transitapi.models.RouteDepartureDto
import dev.garado.transit.api.transitapi.models.RouteDto
import dev.garado.transit.api.transitapi.models.StopDeparturesApiResponse
import dev.garado.transit.api.transitapi.models.StopDto

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
        modeName = route.modeName,
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

internal fun StopDto.toTripStop() = TripStop(
    globalStopId = globalStopId,
    name = stopName,
    lat = stopLat,
    lon = stopLon,
)

fun StopDeparturesApiResponse.toStopDepartures(): Map<String, List<StopDeparture>> = routeDepartures
    .flatMap { it.toStopDepartures() }
    .groupBy { it.globalStopId }
    .mapValues { (_, departures) -> departures.sortedBy { it.departureTime } }

private fun RouteDepartureDto.toStopDepartures(): List<StopDeparture> = mergedItineraries.flatMap { itinerary ->
    val headsign = itinerary.itineraries.firstOrNull()?.let { it.mergedHeadsign ?: it.headsign }
    itinerary.scheduleItems
        .filterNot { it.isCancelled }
        .map { item ->
            StopDeparture(
                globalStopId = globalStopId,
                globalRouteId = globalRouteId,
                routeName = routeShortName ?: routeLongName ?: globalRouteId,
                routeColor = routeColor,
                routeTextColor = routeTextColor,
                headsign = headsign,
                departureTime = item.departureTime,
                isRealTime = item.isRealTime,
            )
        }
}
