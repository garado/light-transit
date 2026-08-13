package dev.garado.transit.gtfs.local

import android.util.Log
import java.io.BufferedReader

private const val TAG = "GtfsStopsTxtParser"

/** Parses a GTFS stops.txt into rows ready to store, ignoring every column but the ones we use */
internal object GtfsStopsTxtParser {
    fun parse(sourceId: Long, reader: BufferedReader): List<GtfsStopEntity> {
        val lines = reader.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return emptyList()

        val header = splitCsvLine(lines.next())
        val idIndex = header.indexOf("stop_id")
        val nameIndex = header.indexOf("stop_name")
        val latIndex = header.indexOf("stop_lat")
        val lonIndex = header.indexOf("stop_lon")
        if (idIndex < 0 || nameIndex < 0 || latIndex < 0 || lonIndex < 0) {
            Log.w(TAG, "stops.txt missing a required column (stop_id/stop_name/stop_lat/stop_lon)")
            return emptyList()
        }

        val entities = mutableListOf<GtfsStopEntity>()
        while (lines.hasNext()) {
            val fields = splitCsvLine(lines.next())
            val stopId = fields.getOrNull(idIndex)?.takeIf { it.isNotBlank() } ?: continue
            val name = fields.getOrNull(nameIndex)?.takeIf { it.isNotBlank() } ?: continue
            val lat = fields.getOrNull(latIndex)?.toDoubleOrNull() ?: continue
            val lon = fields.getOrNull(lonIndex)?.toDoubleOrNull() ?: continue
            entities.add(GtfsStopEntity(sourceId = sourceId, stopId = stopId, name = name, lat = lat, lon = lon))
        }
        return entities
    }

    /** RFC4180 split (handle quoted fields with embedded commas/escaped quotes) */
    private fun splitCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields.map { it.trim() }
    }
}
