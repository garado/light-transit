/** GTFS parsers for getting schedule information */

package dev.garado.transit.gtfs.local

import android.util.Log
import java.io.BufferedReader

private const val TAG = "GtfsScheduleTxtParser"

internal object GtfsRoutesTxtParser {
    fun parse(sourceId: Long, reader: BufferedReader): List<GtfsRouteEntity> {
        val lines = reader.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return emptyList()

        val header = splitGtfsCsvLine(lines.next())
        val idIndex = header.indexOf("route_id")
        val shortNameIndex = header.indexOf("route_short_name")
        val longNameIndex = header.indexOf("route_long_name")
        val colorIndex = header.indexOf("route_color")
        val textColorIndex = header.indexOf("route_text_color")
        if (idIndex < 0) {
            Log.w(TAG, "routes.txt missing route_id")
            return emptyList()
        }

        val entities = mutableListOf<GtfsRouteEntity>()
        while (lines.hasNext()) {
            val fields = splitGtfsCsvLine(lines.next())
            val routeId = fields.getOrNull(idIndex)?.takeIf { it.isNotBlank() } ?: continue
            val shortName = fields.getOrNull(shortNameIndex)?.takeIf { it.isNotBlank() }
            val longName = fields.getOrNull(longNameIndex)?.takeIf { it.isNotBlank() }
            val name = shortName ?: longName ?: routeId
            val color = fields.getOrNull(colorIndex)?.takeIf { it.isNotBlank() }
            val textColor = fields.getOrNull(textColorIndex)?.takeIf { it.isNotBlank() }
            entities.add(GtfsRouteEntity(sourceId = sourceId, routeId = routeId, name = name, color = color, textColor = textColor))
        }
        return entities
    }
}

internal object GtfsTripsTxtParser {
    fun parse(sourceId: Long, reader: BufferedReader): List<GtfsTripEntity> {
        val lines = reader.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return emptyList()

        val header = splitGtfsCsvLine(lines.next())
        val tripIdIndex = header.indexOf("trip_id")
        val routeIdIndex = header.indexOf("route_id")
        val serviceIdIndex = header.indexOf("service_id")
        val headsignIndex = header.indexOf("trip_headsign")
        if (tripIdIndex < 0 || routeIdIndex < 0 || serviceIdIndex < 0) {
            Log.w(TAG, "trips.txt missing a required column (trip_id/route_id/service_id)")
            return emptyList()
        }

        val entities = mutableListOf<GtfsTripEntity>()
        while (lines.hasNext()) {
            val fields = splitGtfsCsvLine(lines.next())
            val tripId = fields.getOrNull(tripIdIndex)?.takeIf { it.isNotBlank() } ?: continue
            val routeId = fields.getOrNull(routeIdIndex)?.takeIf { it.isNotBlank() } ?: continue
            val serviceId = fields.getOrNull(serviceIdIndex)?.takeIf { it.isNotBlank() } ?: continue
            val headsign = fields.getOrNull(headsignIndex)?.takeIf { it.isNotBlank() }
            entities.add(
                GtfsTripEntity(sourceId = sourceId, tripId = tripId, routeId = routeId, serviceId = serviceId, headsign = headsign)
            )
        }
        return entities
    }
}

/**
 * stop_times.txt gets real big real fast. Process rows in batch to keep memory usage bounded.
 */
internal object GtfsStopTimesTxtParser {
    private const val BATCH_SIZE = 2_000

    /** Returns the total row count parsed */
    suspend fun parse(sourceId: Long, reader: BufferedReader, onBatch: suspend (List<GtfsStopTimeEntity>) -> Unit): Int {
        val lines = reader.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return 0

        val header = splitGtfsCsvLine(lines.next())
        val tripIdIndex = header.indexOf("trip_id")
        val stopIdIndex = header.indexOf("stop_id")
        val stopSequenceIndex = header.indexOf("stop_sequence")
        val departureTimeIndex = header.indexOf("departure_time")
        val arrivalTimeIndex = header.indexOf("arrival_time")
        if (tripIdIndex < 0 || stopIdIndex < 0 || stopSequenceIndex < 0 || (departureTimeIndex < 0 && arrivalTimeIndex < 0)) {
            Log.w(TAG, "stop_times.txt missing a required column (trip_id/stop_id/stop_sequence/departure_time)")
            return 0
        }

        var count = 0
        val batch = mutableListOf<GtfsStopTimeEntity>()
        while (lines.hasNext()) {
            val fields = splitGtfsCsvLine(lines.next())
            val tripId = fields.getOrNull(tripIdIndex)?.takeIf { it.isNotBlank() } ?: continue
            val stopId = fields.getOrNull(stopIdIndex)?.takeIf { it.isNotBlank() } ?: continue
            val stopSequence = fields.getOrNull(stopSequenceIndex)?.toIntOrNull() ?: continue
            val rawTime = fields.getOrNull(departureTimeIndex)?.takeIf { it.isNotBlank() }
                ?: fields.getOrNull(arrivalTimeIndex)?.takeIf { it.isNotBlank() }
                ?: continue
            val departureSeconds = parseGtfsTimeToSeconds(rawTime) ?: continue
            batch.add(
                GtfsStopTimeEntity(
                    sourceId = sourceId,
                    tripId = tripId,
                    stopSequence = stopSequence,
                    stopId = stopId,
                    departureSeconds = departureSeconds,
                )
            )
            count++
            if (batch.size >= BATCH_SIZE) {
                onBatch(batch.toList())
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) onBatch(batch.toList())
        return count
    }

    /** "HH:MM:SS" -> since local midnight. GTFS allows HH >= 24 for after-midnight trips */
    private fun parseGtfsTimeToSeconds(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 3) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        val seconds = parts[2].toIntOrNull() ?: return null
        return hours * 3600 + minutes * 60 + seconds
    }
}

internal object GtfsCalendarTxtParser {
    fun parse(sourceId: Long, reader: BufferedReader): List<GtfsCalendarEntity> {
        val lines = reader.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return emptyList()

        val header = splitGtfsCsvLine(lines.next())
        val serviceIdIndex = header.indexOf("service_id")
        val dayIndices = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
            .map { header.indexOf(it) }
        val startDateIndex = header.indexOf("start_date")
        val endDateIndex = header.indexOf("end_date")
        if (serviceIdIndex < 0 || dayIndices.any { it < 0 } || startDateIndex < 0 || endDateIndex < 0) {
            Log.w(TAG, "calendar.txt missing a required column")
            return emptyList()
        }

        val entities = mutableListOf<GtfsCalendarEntity>()
        while (lines.hasNext()) {
            val fields = splitGtfsCsvLine(lines.next())
            val serviceId = fields.getOrNull(serviceIdIndex)?.takeIf { it.isNotBlank() } ?: continue
            val days = dayIndices.map { fields.getOrNull(it) == "1" }
            val startDate = fields.getOrNull(startDateIndex)?.toIntOrNull() ?: continue
            val endDate = fields.getOrNull(endDateIndex)?.toIntOrNull() ?: continue
            entities.add(
                GtfsCalendarEntity(
                    sourceId = sourceId,
                    serviceId = serviceId,
                    monday = days[0],
                    tuesday = days[1],
                    wednesday = days[2],
                    thursday = days[3],
                    friday = days[4],
                    saturday = days[5],
                    sunday = days[6],
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        return entities
    }
}
