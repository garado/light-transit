/** Database for locally-stored GTFS data */

package dev.garado.transit.gtfs.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase
import dev.garado.transit.api.models.TripRoute
import dev.garado.transit.api.models.TripStop
import dev.garado.transit.gtfs.sources.GtfsSourceDownloadState
import dev.garado.transit.map.LatLon
import dev.garado.transit.util.encodePolyline
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.flow.Flow

// SOURCES ------------------------

@Entity(tableName = "gtfs_sources")
internal data class GtfsSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    @ColumnInfo(name = "region_code") val regionCode: String,
    val path: String,
    @ColumnInfo(name = "download_state", defaultValue = "NOT_DOWNLOADED") val downloadState: String = GtfsSourceDownloadState.NOT_DOWNLOADED.name,
)

@Dao
internal interface GtfsSourceDao {
    @Query("SELECT * FROM gtfs_sources ORDER BY id ASC")
    fun getAll(): Flow<List<GtfsSourceEntity>>

    @Query("SELECT * FROM gtfs_sources WHERE key = :key AND region_code = :regionCode LIMIT 1")
    suspend fun findByKeyAndRegion(key: String, regionCode: String): GtfsSourceEntity?

    @Insert
    suspend fun insert(entity: GtfsSourceEntity): Long

    @Query("DELETE FROM gtfs_sources WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM gtfs_sources WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE gtfs_sources SET download_state = :state WHERE id = :id")
    suspend fun updateDownloadState(id: Long, state: String)

    @Query("UPDATE gtfs_sources SET download_state = :state")
    suspend fun resetAllDownloadStates(state: String)
}

// STOPS ------------------------

private const val GEOHASH_PRECISION = 6  // ~0.6km x 1.2km

@Entity(
    tableName = "gtfs_stops",
    primaryKeys = ["source_id", "stop_id"],
    foreignKeys = [
        ForeignKey(
            entity = GtfsSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("lat"), Index("lon"), Index("geohash")],
)
internal data class GtfsStopEntity(
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "stop_id") val stopId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    @ColumnInfo(name = "location_type") val locationType: String? = null,
    @ColumnInfo(name = "parent_station") val parentStation: String? = null,
    val geohash: String = Geohash.encode(lat, lon, GEOHASH_PRECISION),
)

@Dao
internal interface GtfsStopsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<GtfsStopEntity>)

    @Query("DELETE FROM gtfs_stops WHERE source_id = :sourceId")
    suspend fun deleteBySource(sourceId: Long)

    @Query("DELETE FROM gtfs_stops")
    suspend fun clear()

    @Query(
        """
        SELECT * FROM gtfs_stops
        WHERE geohash IN (:cells)
          AND lat BETWEEN :minLat AND :maxLat
          AND lon BETWEEN :minLon AND :maxLon
          AND (location_type IS NULL OR location_type = '0')
        """
    )
    suspend fun nearbyInCells(
        cells: List<String>,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<GtfsStopEntity>

    /**
     * Geohash narrows candidates down to a handful of indexed cells.
     * lat/lon BETWEEN is a final exact filter to drop false positives
     */
    suspend fun nearby(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<GtfsStopEntity> {
        val cells = Geohash.cellsCovering(minLat, maxLat, minLon, maxLon, GEOHASH_PRECISION)
        return nearbyInCells(cells, minLat, maxLat, minLon, maxLon)
    }
}

/**
 * Local GTFS stops are id-namespaced by source to avoid collisions with live API's global_stop_id.
 * Merges platforms that share a parent_station into one [TripStop].
 */
internal fun List<GtfsStopEntity>.toGroupedTripStop(): TripStop {
    val first = first()
    return TripStop(
        globalStopId = "gtfs:${first.sourceId}:${first.stopId}",
        name = first.name,
        lat = first.lat,
        lon = first.lon,
        groupedStopIds = map { "gtfs:${it.sourceId}:${it.stopId}" },
    )
}

/** Inverse of [toGroupedTripStop]'s globalStopId; null if [globalStopId] isn't a local-GTFS id */
internal fun parseGtfsGlobalStopId(globalStopId: String): Pair<Long, String>? {
    val parts = globalStopId.split(":", limit = 3)
    if (parts.size != 3 || parts[0] != "gtfs") return null
    val sourceId = parts[1].toLongOrNull() ?: return null
    return sourceId to parts[2]
}

/**
 * Basic geohash implementation: encodes a point to a base32 string identifying its grid cell,
 * and enumerates the cells (at a given precision) that a bounding box overlaps.
 * (used as alternative to rtree, since light's sqlite build doesn't include rtree. sadge)
 */
internal object Geohash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(lat: Double, lon: Double, precision: Int): String {
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0
        val hash = StringBuilder()
        var isEvenBit = true
        var bit = 0
        var ch = 0
        while (hash.length < precision) {
            if (isEvenBit) {
                val mid = (lonMin + lonMax) / 2
                if (lon >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    lonMin = mid
                } else {
                    lonMax = mid
                }
            } else {
                val mid = (latMin + latMax) / 2
                if (lat >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    latMin = mid
                } else {
                    latMax = mid
                }
            }
            isEvenBit = !isEvenBit
            if (bit < 4) {
                bit++
            } else {
                hash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return hash.toString()
    }

    /** Every cell (at [precision]) that overlaps the given bounding box. */
    fun cellsCovering(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, precision: Int): List<String> {
        val totalBits = precision * 5
        val lonBits = ceil(totalBits / 2.0).toInt()
        val latBits = totalBits / 2
        val cellWidth = 360.0 / (1L shl lonBits)
        val cellHeight = 180.0 / (1L shl latBits)

        // grid-aligned indices
        val maxLatIndex = (1L shl latBits) - 1
        val maxLonIndex = (1L shl lonBits) - 1
        fun latIndex(lat: Double) =
            floor((lat.coerceIn(-90.0, 90.0) + 90.0) / cellHeight).toLong().coerceIn(0, maxLatIndex)
        fun lonIndex(lon: Double) =
            floor((lon.coerceIn(-180.0, 180.0) + 180.0) / cellWidth).toLong().coerceIn(0, maxLonIndex)

        val cells = LinkedHashSet<String>()
        for (latIdx in latIndex(minLat)..latIndex(maxLat)) {
            val lat = (-90.0 + (latIdx + 0.5) * cellHeight).coerceIn(-90.0, 90.0)
            for (lonIdx in lonIndex(minLon)..lonIndex(maxLon)) {
                val lon = (-180.0 + (lonIdx + 0.5) * cellWidth).coerceIn(-180.0, 180.0)
                cells.add(encode(lat, lon, precision))
            }
        }
        return cells.toList()
    }
}

// SCHEDULE ------------------------

@Entity(
    tableName = "gtfs_routes",
    primaryKeys = ["source_id", "route_id"],
    foreignKeys = [
        ForeignKey(
            entity = GtfsSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class GtfsRouteEntity(
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "route_id") val routeId: String,
    /** badge text: route_short_name, falling back to route_long_name, falling back to route_id */
    val name: String,
    /** route_long_name, when present and distinct from [name] - the descriptive text next to the badge */
    @ColumnInfo(name = "long_name") val longName: String?,
    val color: String?,
    @ColumnInfo(name = "text_color") val textColor: String?,
)

internal fun GtfsRouteEntity.toTripRoute() = TripRoute(
    globalRouteId = "gtfs:$sourceId:$routeId",
    name = name,
    longName = longName,
    color = color,
    textColor = textColor,
)

/** Inverse of [toTripRoute]'s globalRouteId; null if [globalRouteId] isn't a local-GTFS id */
internal fun parseGtfsGlobalRouteId(globalRouteId: String): Pair<Long, String>? {
    val parts = globalRouteId.split(":", limit = 3)
    if (parts.size != 3 || parts[0] != "gtfs") return null
    val sourceId = parts[1].toLongOrNull() ?: return null
    return sourceId to parts[2]
}

@Entity(
    tableName = "gtfs_trips",
    primaryKeys = ["source_id", "trip_id"],
    foreignKeys = [
        ForeignKey(
            entity = GtfsSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("source_id", "service_id"), Index("source_id", "route_id")],
)
internal data class GtfsTripEntity(
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "trip_id") val tripId: String,
    @ColumnInfo(name = "route_id") val routeId: String,
    @ColumnInfo(name = "service_id") val serviceId: String,
    val headsign: String?,
    @ColumnInfo(name = "shape_id") val shapeId: String?,
)

/**
 * [departureSeconds] is seconds since local midnight
 * GTFS allows values >= 86400 for after-midnight trips
 */
@Entity(
    tableName = "gtfs_stop_times",
    primaryKeys = ["source_id", "trip_id", "stop_sequence"],
    foreignKeys = [
        ForeignKey(
            entity = GtfsSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("source_id", "stop_id")],
)
internal data class GtfsStopTimeEntity(
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "trip_id") val tripId: String,
    @ColumnInfo(name = "stop_sequence") val stopSequence: Int,
    @ColumnInfo(name = "stop_id") val stopId: String,
    @ColumnInfo(name = "departure_seconds") val departureSeconds: Int,
)

/** [startDate]/[endDate] are YYYYMMDD ints. Day flags are true when service runs that weekday */
@Entity(
    tableName = "gtfs_calendar",
    primaryKeys = ["source_id", "service_id"],
    foreignKeys = [
        ForeignKey(
            entity = GtfsSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class GtfsCalendarEntity(
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "service_id") val serviceId: String,
    val monday: Boolean,
    val tuesday: Boolean,
    val wednesday: Boolean,
    val thursday: Boolean,
    val friday: Boolean,
    val saturday: Boolean,
    val sunday: Boolean,
    @ColumnInfo(name = "start_date") val startDate: Int,
    @ColumnInfo(name = "end_date") val endDate: Int,
)

/** One point along a route's shape polyline, ordered by [sequence] */
@Entity(
    tableName = "gtfs_shape_points",
    primaryKeys = ["source_id", "shape_id", "sequence"],
    foreignKeys = [
        ForeignKey(
            entity = GtfsSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("source_id", "shape_id")],
)
internal data class GtfsShapePointEntity(
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "shape_id") val shapeId: String,
    val sequence: Int,
    val lat: Double,
    val lon: Double,
)

internal data class GtfsDepartureRow(
    @ColumnInfo(name = "stop_id") val stopId: String,
    @ColumnInfo(name = "departure_seconds") val departureSeconds: Int,
    val headsign: String?,
    @ColumnInfo(name = "route_id") val routeId: String,
    @ColumnInfo(name = "route_name") val routeName: String,
    @ColumnInfo(name = "route_color") val routeColor: String?,
    @ColumnInfo(name = "route_text_color") val routeTextColor: String?,
)

@Dao
internal interface GtfsScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(entities: List<GtfsRouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(entities: List<GtfsTripEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopTimes(entities: List<GtfsStopTimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendars(entities: List<GtfsCalendarEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShapePoints(entities: List<GtfsShapePointEntity>)

    @Query("DELETE FROM gtfs_routes WHERE source_id = :sourceId")
    suspend fun deleteRoutesBySource(sourceId: Long)

    @Query("DELETE FROM gtfs_trips WHERE source_id = :sourceId")
    suspend fun deleteTripsBySource(sourceId: Long)

    @Query("DELETE FROM gtfs_stop_times WHERE source_id = :sourceId")
    suspend fun deleteStopTimesBySource(sourceId: Long)

    @Query("DELETE FROM gtfs_calendar WHERE source_id = :sourceId")
    suspend fun deleteCalendarsBySource(sourceId: Long)

    @Query("DELETE FROM gtfs_shape_points WHERE source_id = :sourceId")
    suspend fun deleteShapesBySource(sourceId: Long)

    suspend fun deleteBySource(sourceId: Long) {
        deleteRoutesBySource(sourceId)
        deleteTripsBySource(sourceId)
        deleteStopTimesBySource(sourceId)
        deleteCalendarsBySource(sourceId)
        deleteShapesBySource(sourceId)
    }

    @Query("DELETE FROM gtfs_routes")
    suspend fun clearRoutes()

    @Query("DELETE FROM gtfs_trips")
    suspend fun clearTrips()

    @Query("DELETE FROM gtfs_stop_times")
    suspend fun clearStopTimes()

    @Query("DELETE FROM gtfs_calendar")
    suspend fun clearCalendars()

    @Query("DELETE FROM gtfs_shape_points")
    suspend fun clearShapes()

    suspend fun clear() {
        clearRoutes()
        clearTrips()
        clearStopTimes()
        clearCalendars()
        clearShapes()
    }

    /** Runs [block] as one DB transaction, so many small suspend calls inside it (e.g. batched inserts) commit once. */
    @Transaction
    suspend fun insertStopTimesInOneTransaction(block: suspend () -> Int): Int = block()

    /** Runs [block] as one DB transaction, so many small suspend calls inside it (e.g. batched inserts) commit once. */
    @Transaction
    suspend fun insertShapePointsInOneTransaction(block: suspend () -> Int): Int = block()

    @Query("SELECT * FROM gtfs_calendar WHERE source_id = :sourceId AND start_date <= :today AND end_date >= :today")
    suspend fun calendarsActiveOn(sourceId: Long, today: Int): List<GtfsCalendarEntity>

    @Query(
        """
        SELECT
            st.stop_id AS stop_id,
            st.departure_seconds AS departure_seconds,
            t.headsign AS headsign,
            r.route_id AS route_id,
            r.name AS route_name,
            r.color AS route_color,
            r.text_color AS route_text_color
        FROM gtfs_stop_times st
        INNER JOIN gtfs_trips t ON t.source_id = st.source_id AND t.trip_id = st.trip_id
        INNER JOIN gtfs_routes r ON r.source_id = t.source_id AND r.route_id = t.route_id
        WHERE st.source_id = :sourceId
          AND st.stop_id IN (:stopIds)
          AND st.departure_seconds >= :afterSeconds
          AND t.service_id IN (:serviceIds)
        ORDER BY st.departure_seconds ASC
        """
    )
    suspend fun departures(
        sourceId: Long,
        stopIds: List<String>,
        afterSeconds: Int,
        serviceIds: List<String>,
    ): List<GtfsDepartureRow>

    /** Every distinct route that stops at any of [stopIds] */
    @Query(
        """
        SELECT DISTINCT r.*
        FROM gtfs_stop_times st
        INNER JOIN gtfs_trips t ON t.source_id = st.source_id AND t.trip_id = st.trip_id
        INNER JOIN gtfs_routes r ON r.source_id = t.source_id AND r.route_id = t.route_id
        WHERE st.source_id = :sourceId
          AND st.stop_id IN (:stopIds)
        """
    )
    suspend fun routesServing(sourceId: Long, stopIds: List<String>): List<GtfsRouteEntity>

    /**
     * routes can have multiple trips with slightly different shape IDs
     * Pick an arbitrary one for [routeId] to be the representative path for the route
     */
    @Query("SELECT shape_id FROM gtfs_trips WHERE source_id = :sourceId AND route_id = :routeId AND shape_id IS NOT NULL LIMIT 1")
    suspend fun representativeShapeId(sourceId: Long, routeId: String): String?

    @Query("SELECT * FROM gtfs_shape_points WHERE source_id = :sourceId AND shape_id = :shapeId ORDER BY sequence ASC")
    suspend fun shapePoints(sourceId: Long, shapeId: String): List<GtfsShapePointEntity>

    /** Arbitrary trip's trip_id for [routeId], as a representative stopping pattern for the route */
    @Query("SELECT trip_id FROM gtfs_trips WHERE source_id = :sourceId AND route_id = :routeId LIMIT 1")
    suspend fun representativeTripId(sourceId: Long, routeId: String): String?

    /** Stops served by the representative trip on [routeId], in visiting order */
    @Query(
        """
        SELECT s.*
        FROM gtfs_stop_times st
        INNER JOIN gtfs_stops s ON s.source_id = st.source_id AND s.stop_id = st.stop_id
        WHERE st.source_id = :sourceId AND st.trip_id = :tripId
        ORDER BY st.stop_sequence ASC
        """
    )
    suspend fun stopsForTrip(sourceId: Long, tripId: String): List<GtfsStopEntity>
}

/** Encoded polyline for [routeId]'s representative shape, or null if unavailable */
internal suspend fun GtfsScheduleDao.routeShape(sourceId: Long, routeId: String): String? {
    val shapeId = representativeShapeId(sourceId, routeId) ?: return null
    val points = shapePoints(sourceId, shapeId)
    if (points.isEmpty()) return null
    return encodePolyline(points.map { LatLon(lat = it.lat, lon = it.lon) })
}

// DATABASE ------------------------

@Database(
    entities = [
        GtfsSourceEntity::class,
        GtfsStopEntity::class,
        GtfsRouteEntity::class,
        GtfsTripEntity::class,
        GtfsStopTimeEntity::class,
        GtfsCalendarEntity::class,
        GtfsShapePointEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
internal abstract class GtfsDatabase : RoomDatabase() {
    abstract fun gtfsSourceDao(): GtfsSourceDao
    abstract fun gtfsStopsDao(): GtfsStopsDao
    abstract fun gtfsScheduleDao(): GtfsScheduleDao
}

/** Process-wide singleton database */
internal object GtfsDatabaseHolder {
    @Volatile
    private var instance: GtfsDatabase? = null

    fun get(lightContext: SealedLightContext): GtfsDatabase =
        instance ?: synchronized(this) {
            instance ?: lightContext.buildDatabase(GtfsDatabase::class.java, "gtfs.db")
                .also { instance = it }
        }
}
