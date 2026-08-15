package dev.garado.transit.api.transitapi

import dev.garado.transit.models.TripLeg
import dev.garado.transit.api.transitapi.models.DepartureDto
import dev.garado.transit.api.transitapi.models.ItineraryDto
import dev.garado.transit.api.transitapi.models.LegDto
import dev.garado.transit.api.transitapi.models.PlanApiResponse
import dev.garado.transit.api.transitapi.models.PlanDetailsDto
import dev.garado.transit.api.transitapi.models.PlanResultDto
import dev.garado.transit.api.transitapi.models.RouteDto
import dev.garado.transit.api.transitapi.models.StopDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanMapperTest {

    @Test
    fun `maps a walk leg with distance and polyline`() {
        val leg = LegDto(legMode = "walk", startTime = 100, endTime = 200, duration = 100, distance = 50.0, polyline = "abc123")

        val walkLeg = planResponseWithLegs(leg).toTripPlans().single().legs.single() as TripLeg.Walk

        assertEquals(50.0, walkLeg.distance)
        assertEquals("abc123", walkLeg.polyline)
    }

    @Test
    fun `drops a walk leg missing distance or polyline`() {
        val missingDistance = LegDto(legMode = "walk", startTime = 0, endTime = 0, duration = 0, polyline = "abc")
        val missingPolyline = LegDto(legMode = "walk", startTime = 0, endTime = 0, duration = 0, distance = 1.0)

        assertTrue(planResponseWithLegs(missingDistance).toTripPlans().single().legs.isEmpty())
        assertTrue(planResponseWithLegs(missingPolyline).toTripPlans().single().legs.isEmpty())
    }

    @Test
    fun `drops a transit leg missing routes or departures`() {
        val missingRoutes = LegDto(legMode = "transit", startTime = 0, endTime = 0, duration = 0, departures = listOf(departure()))
        val missingDepartures = LegDto(legMode = "transit", startTime = 0, endTime = 0, duration = 0, routes = listOf(route()))

        assertTrue(planResponseWithLegs(missingRoutes).toTripPlans().single().legs.isEmpty())
        assertTrue(planResponseWithLegs(missingDepartures).toTripPlans().single().legs.isEmpty())
    }

    @Test
    fun `drops legs with an unrecognized leg_mode`() {
        val leg = LegDto(legMode = "microtransit", startTime = 0, endTime = 0, duration = 0)

        assertTrue(planResponseWithLegs(leg).toTripPlans().single().legs.isEmpty())
    }

    @Test
    fun `maps a transit leg, preferring plan_shape over the full itinerary shape`() {
        val itinerary = ItineraryDto(
            headsign = "Downtown",
            shape = "full-route-shape",
            stops = listOf(StopDto("1:1", "Stop A", 1.0, 2.0)),
            planDetails = PlanDetailsDto(planShape = "ridden-segment-shape"),
        )
        val leg = LegDto(
            legMode = "transit",
            startTime = 0,
            endTime = 0,
            duration = 0,
            departures = listOf(departure(departureTime = 555L)),
            routes = listOf(route(routeShortName = "55", itineraries = listOf(itinerary))),
        )

        val transitLeg = planResponseWithLegs(leg).toTripPlans().single().legs.single() as TripLeg.Transit

        assertEquals("ridden-segment-shape", transitLeg.shape)
        assertEquals("Downtown", transitLeg.headsign)
        assertEquals(555L, transitLeg.nextDepartureTime)
        assertEquals("55", transitLeg.routeName)
        assertEquals(1, transitLeg.stops.size)
    }

    @Test
    fun `falls back to the full itinerary shape when plan_shape is absent`() {
        val itinerary = ItineraryDto(shape = "full-route-shape", planDetails = null)
        val leg = LegDto(
            legMode = "transit",
            startTime = 0,
            endTime = 0,
            duration = 0,
            departures = listOf(departure()),
            routes = listOf(route(itineraries = listOf(itinerary))),
        )

        val transitLeg = planResponseWithLegs(leg).toTripPlans().single().legs.single() as TripLeg.Transit

        assertEquals("full-route-shape", transitLeg.shape)
    }

    @Test
    fun `slices stops to the ridden segment using start_stop_offset and end_stop_offset`() {
        val allStops = listOf(
            StopDto("1", "Origin Terminus", 0.0, 0.0),
            StopDto("2", "Before Boarding", 0.0, 0.0),
            StopDto("3", "Boarding Stop", 0.0, 0.0),
            StopDto("4", "Middle Stop", 0.0, 0.0),
            StopDto("5", "Alighting Stop", 0.0, 0.0),
            StopDto("6", "After Alighting", 0.0, 0.0),
            StopDto("7", "Destination Terminus", 0.0, 0.0),
        )
        val itinerary = ItineraryDto(
            stops = allStops,
            planDetails = PlanDetailsDto(startStopOffset = 2, endStopOffset = 4),
        )
        val leg = LegDto(
            legMode = "transit",
            startTime = 0,
            endTime = 0,
            duration = 0,
            departures = listOf(departure()),
            routes = listOf(route(itineraries = listOf(itinerary))),
        )

        val transitLeg = planResponseWithLegs(leg).toTripPlans().single().legs.single() as TripLeg.Transit

        assertEquals(listOf("Boarding Stop", "Middle Stop", "Alighting Stop"), transitLeg.stops.map { it.name })
    }

    @Test
    fun `falls back to the full stop list when offsets are absent`() {
        val allStops = listOf(StopDto("1", "A", 0.0, 0.0), StopDto("2", "B", 0.0, 0.0))
        val itinerary = ItineraryDto(stops = allStops, planDetails = PlanDetailsDto())
        val leg = LegDto(
            legMode = "transit",
            startTime = 0,
            endTime = 0,
            duration = 0,
            departures = listOf(departure()),
            routes = listOf(route(itineraries = listOf(itinerary))),
        )

        val transitLeg = planResponseWithLegs(leg).toTripPlans().single().legs.single() as TripLeg.Transit

        assertEquals(listOf("A", "B"), transitLeg.stops.map { it.name })
    }

    @Test
    fun `falls back from route_short_name to route_long_name to global_route_id for routeName`() {
        val onlyLongName = route(routeShortName = null, routeLongName = "Long Name", globalRouteId = "id-1")
        val onlyId = route(routeShortName = null, routeLongName = null, globalRouteId = "id-2")

        assertEquals("Long Name", transitLegFor(onlyLongName).routeName)
        assertEquals("id-2", transitLegFor(onlyId).routeName)
    }

    private fun transitLegFor(route: RouteDto): TripLeg.Transit {
        val leg = LegDto(
            legMode = "transit",
            startTime = 0,
            endTime = 0,
            duration = 0,
            departures = listOf(departure()),
            routes = listOf(route),
        )
        return planResponseWithLegs(leg).toTripPlans().single().legs.single() as TripLeg.Transit
    }

    private fun planResponseWithLegs(vararg legs: LegDto) = PlanApiResponse(
        results = listOf(PlanResultDto(startTime = 1, endTime = 2, duration = 1, legs = legs.toList())),
    )

    private fun departure(departureTime: Long = 100L) = DepartureDto(departureTime = departureTime)

    private fun route(
        globalRouteId: String = "route-1",
        routeShortName: String? = "1",
        routeLongName: String? = null,
        itineraries: List<ItineraryDto> = emptyList(),
    ) = RouteDto(
        globalRouteId = globalRouteId,
        routeShortName = routeShortName,
        routeLongName = routeLongName,
        itineraries = itineraries,
    )
}
