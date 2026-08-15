package dev.garado.transit.gtfs.local

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.api.models.StopDeparture
import dev.garado.transit.interfaces.stopdepartures.StopDeparturesInterface
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**
 * Computes upcoming departures from locally-stored GTFS schedule data
 * calendar.txt day-of-week/date-range only
 * excludes real-time data, exceptions (calendar_dates.txt), frequencies.txt
 */
class GtfsLocalStopDeparturesProvider(lightContext: SealedLightContext) : StopDeparturesInterface {
    private val dao = GtfsDatabaseHolder.get(lightContext).gtfsScheduleDao()

    override suspend fun departures(globalStopIds: List<String>, maxDepartures: Int): Map<String, List<StopDeparture>> {
        val stopsBySource = globalStopIds.mapNotNull { globalStopId ->
            parseGtfsGlobalStopId(globalStopId)?.let { (sourceId, rawStopId) -> Triple(globalStopId, sourceId, rawStopId) }
        }.groupBy { it.second }

        val now = ZonedDateTime.now()
        val todayInt = now.year * 10_000 + now.monthValue * 100 + now.dayOfMonth
        val nowSeconds = now.hour * 3600 + now.minute * 60 + now.second
        val midnightEpochSeconds = now.toLocalDate().atStartOfDay(now.zone).toEpochSecond()

        val result = mutableMapOf<String, MutableList<StopDeparture>>()
        for ((sourceId, entries) in stopsBySource) {
            val activeServiceIds = dao.calendarsActiveOn(sourceId, todayInt)
                .filter { it.runsOn(now.dayOfWeek) }
                .map { it.serviceId }
            if (activeServiceIds.isEmpty()) continue

            val globalIdByRawStopId = entries.associate { it.third to it.first }
            val rows = dao.departures(
                sourceId = sourceId,
                stopIds = entries.map { it.third },
                afterSeconds = nowSeconds,
                serviceIds = activeServiceIds,
            )

            for (row in rows) {
                val globalStopId = globalIdByRawStopId[row.stopId] ?: continue
                val stopDepartures = result.getOrPut(globalStopId) { mutableListOf() }
                if (stopDepartures.size >= maxDepartures) continue
                stopDepartures.add(
                    StopDeparture(
                        globalStopId = globalStopId,
                        globalRouteId = "gtfs:$sourceId:${row.routeId}",
                        routeName = row.routeName,
                        routeColor = row.routeColor,
                        routeTextColor = row.routeTextColor,
                        headsign = row.headsign,
                        departureTime = midnightEpochSeconds + row.departureSeconds,
                        isRealTime = false,
                    )
                )
            }
        }
        return result
    }

    private fun GtfsCalendarEntity.runsOn(day: DayOfWeek) = when (day) {
        DayOfWeek.MONDAY -> monday
        DayOfWeek.TUESDAY -> tuesday
        DayOfWeek.WEDNESDAY -> wednesday
        DayOfWeek.THURSDAY -> thursday
        DayOfWeek.FRIDAY -> friday
        DayOfWeek.SATURDAY -> saturday
        DayOfWeek.SUNDAY -> sunday
    }
}
