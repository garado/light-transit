/** GTFS parsers for getting schedule information */

package dev.garado.transit.gtfs.local

import android.util.Log
import java.io.BufferedReader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
            entities.add(
                GtfsRouteEntity(
                    sourceId = sourceId,
                    routeId = routeId,
                    name = name,
                    longName = longName?.takeIf { it != name },
                    color = color,
                    textColor = textColor,
                )
            )
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
        val shapeIdIndex = header.indexOf("shape_id")
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
            val shapeId = fields.getOrNull(shapeIdIndex)?.takeIf { it.isNotBlank() }
            entities.add(
                GtfsTripEntity(
                    sourceId = sourceId,
                    tripId = tripId,
                    routeId = routeId,
                    serviceId = serviceId,
                    headsign = headsign,
                    shapeId = shapeId,
                )
            )
        }
        return entities
    }
}

/** This is the most performance critical code in the entire app rn */
internal object GtfsStopTimesTxtParser {
    private const val BATCH_SIZE = 5_000

    suspend fun parse(sourceId: Long, reader: BufferedReader, onBatch: suspend (List<GtfsStopTimeEntity>) -> Unit): Int {
        // Read header
        val headerLine = reader.readLine() ?: return 0
        val header = splitGtfsCsvLine(headerLine)
        
        val tripIdIndex = header.indexOf("trip_id")
        val stopIdIndex = header.indexOf("stop_id")
        val stopSequenceIndex = header.indexOf("stop_sequence")
        val departureTimeIndex = header.indexOf("departure_time")
        val arrivalTimeIndex = header.indexOf("arrival_time")

        if (tripIdIndex < 0 || stopIdIndex < 0 || stopSequenceIndex < 0 || (departureTimeIndex < 0 && arrivalTimeIndex < 0)) {
            Log.w(TAG, "stop_times.txt missing a required column")
            return 0
        }

        var count = 0

        // Parsing is CPU-bound - decompress + CSV-split + build entities
        // onBatch is I/O-bound - DB insert
        // Run these as separate coroutines connected by a small bounded channel, so next
        // batch can be parsed while the current one is being written
        coroutineScope {
            val channel = Channel<List<GtfsStopTimeEntity>>(capacity = 2)
            val consumer = launch {
                for (batch in channel) onBatch(batch)
            }

            val batch = ArrayList<GtfsStopTimeEntity>(BATCH_SIZE)
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.isNotEmpty()) {
                    val fields = splitGtfsCsvLine(line)

                    val tripId = fields.getOrNull(tripIdIndex)
                    val stopId = fields.getOrNull(stopIdIndex)
                    val stopSeqStr = fields.getOrNull(stopSequenceIndex)
                    val rawTime = fields.getOrNull(departureTimeIndex)?.ifEmpty { null }
                        ?: fields.getOrNull(arrivalTimeIndex)?.ifEmpty { null }

                    if (!tripId.isNullOrEmpty() && !stopId.isNullOrEmpty() && !stopSeqStr.isNullOrEmpty() && rawTime != null) {
                        val stopSequence = stopSeqStr.toIntOrNull()
                        val departureSeconds = parseGtfsTimeToSecondsFast(rawTime)

                        if (stopSequence != null && departureSeconds != null) {
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
                                channel.send(ArrayList(batch))
                                batch.clear()
                            }
                        }
                    }
                }
                line = reader.readLine()
            }
            if (batch.isNotEmpty()) channel.send(ArrayList(batch))

            channel.close()
            consumer.join()
        }

        return count
    }

    /** Allocation-free HH:MM:SS parsing directly from CharSequence index positions */
    private fun parseGtfsTimeToSecondsFast(time: String): Int? {
        val firstColon = time.indexOf(':')
        if (firstColon <= 0) return null
        val secondColon = time.indexOf(':', firstColon + 1)
        if (secondColon <= 0) return null

        val hours = parseIntFast(time, 0, firstColon) ?: return null
        val minutes = parseIntFast(time, firstColon + 1, secondColon) ?: return null
        val seconds = parseIntFast(time, secondColon + 1, time.length) ?: return null

        return hours * 3600 + minutes * 60 + seconds
    }

    private fun parseIntFast(s: String, start: Int, end: Int): Int? {
        if (start >= end) return null
        var result = 0
        for (i in start until end) {
            val digit = s[i] - '0'
            if (digit !in 0..9) return null
            result = result * 10 + digit
        }
        return result
    }

}

internal object GtfsShapesTxtParser {
    private const val BATCH_SIZE = 5_000

    suspend fun parse(sourceId: Long, reader: BufferedReader, onBatch: suspend (List<GtfsShapePointEntity>) -> Unit): Int {
        val headerLine = reader.readLine() ?: return 0
        val header = splitGtfsCsvLine(headerLine)

        val shapeIdIndex = header.indexOf("shape_id")
        val latIndex = header.indexOf("shape_pt_lat")
        val lonIndex = header.indexOf("shape_pt_lon")
        val sequenceIndex = header.indexOf("shape_pt_sequence")

        if (shapeIdIndex < 0 || latIndex < 0 || lonIndex < 0 || sequenceIndex < 0) {
            Log.w(TAG, "shapes.txt missing a required column")
            return 0
        }

        var count = 0

        coroutineScope {
            val channel = Channel<List<GtfsShapePointEntity>>(capacity = 2)
            val consumer = launch {
                for (batch in channel) onBatch(batch)
            }

            val batch = ArrayList<GtfsShapePointEntity>(BATCH_SIZE)
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.isNotEmpty()) {
                    val fields = splitGtfsCsvLine(line)

                    val shapeId = fields.getOrNull(shapeIdIndex)
                    val lat = fields.getOrNull(latIndex)?.toDoubleOrNull()
                    val lon = fields.getOrNull(lonIndex)?.toDoubleOrNull()
                    val sequence = fields.getOrNull(sequenceIndex)?.toIntOrNull()

                    if (!shapeId.isNullOrEmpty() && lat != null && lon != null && sequence != null) {
                        batch.add(
                            GtfsShapePointEntity(
                                sourceId = sourceId,
                                shapeId = shapeId,
                                sequence = sequence,
                                lat = lat,
                                lon = lon,
                            )
                        )
                        count++

                        if (batch.size >= BATCH_SIZE) {
                            channel.send(ArrayList(batch))
                            batch.clear()
                        }
                    }
                }
                line = reader.readLine()
            }
            if (batch.isNotEmpty()) channel.send(ArrayList(batch))

            channel.close()
            consumer.join()
        }

        return count
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
