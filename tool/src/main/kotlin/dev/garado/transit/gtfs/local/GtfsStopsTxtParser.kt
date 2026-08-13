package dev.garado.transit.gtfs.local

import android.util.Log
import java.io.BufferedReader

private const val TAG = "GtfsStopsTxtParser"

/** Parses a GTFS stops.txt into rows ready to store, ignoring every column but the ones we use */
internal object GtfsStopsTxtParser {
    fun parse(sourceId: Long, reader: BufferedReader): List<GtfsStopEntity> {
        val lines = reader.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return emptyList()

        val header = splitGtfsCsvLine(lines.next())
        val idIndex = header.indexOf("stop_id")
        val nameIndex = header.indexOf("stop_name")
        val latIndex = header.indexOf("stop_lat")
        val lonIndex = header.indexOf("stop_lon")
        val locationTypeIndex = header.indexOf("location_type")
        val parentStationIndex = header.indexOf("parent_station")
        if (idIndex < 0 || nameIndex < 0 || latIndex < 0 || lonIndex < 0) {
            Log.w(TAG, "stops.txt missing a required column (stop_id/stop_name/stop_lat/stop_lon)")
            return emptyList()
        }

        val entities = mutableListOf<GtfsStopEntity>()
        while (lines.hasNext()) {
            val fields = splitGtfsCsvLine(lines.next())
            val stopId = fields.getOrNull(idIndex)?.takeIf { it.isNotBlank() } ?: continue
            val name = fields.getOrNull(nameIndex)?.takeIf { it.isNotBlank() } ?: continue
            val lat = fields.getOrNull(latIndex)?.toDoubleOrNull() ?: continue
            val lon = fields.getOrNull(lonIndex)?.toDoubleOrNull() ?: continue
            val locationType = fields.getOrNull(locationTypeIndex)?.takeIf { it.isNotBlank() }
            val parentStation = fields.getOrNull(parentStationIndex)?.takeIf { it.isNotBlank() }
            entities.add(
                GtfsStopEntity(
                    sourceId = sourceId,
                    stopId = stopId,
                    name = name,
                    lat = lat,
                    lon = lon,
                    locationType = locationType,
                    parentStation = parentStation,
                )
            )
        }
        return entities
    }
}
