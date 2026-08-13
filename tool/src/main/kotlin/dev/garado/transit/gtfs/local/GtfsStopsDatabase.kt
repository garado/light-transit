/** Set up room db for stops imported from downloaded GTFS static feeds (stops.txt) */

package dev.garado.transit.gtfs.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase
import dev.garado.transit.api.models.TripStop
import kotlin.math.ceil
import kotlin.math.floor

/** ~0.6km x 1.2km */
private const val GEOHASH_PRECISION = 6

@Entity(
    tableName = "gtfs_stops",
    primaryKeys = ["source_id", "stop_id"],
    indices = [Index("lat"), Index("lon"), Index("geohash")],
)
internal data class GtfsStopEntity(
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "stop_id") val stopId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
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

@Database(entities = [GtfsStopEntity::class], version = 1, exportSchema = false)
abstract class GtfsStopsDatabase : RoomDatabase() {
    internal abstract fun gtfsStopsDao(): GtfsStopsDao
}

/** Process-wide singleton database */
object GtfsStopsDatabaseHolder {
    @Volatile
    private var instance: GtfsStopsDatabase? = null

    fun get(lightContext: SealedLightContext): GtfsStopsDatabase =
        instance ?: synchronized(this) {
            instance ?: lightContext.buildDatabase(GtfsStopsDatabase::class.java, "gtfs_stops.db")
                .also { instance = it }
        }
}

/** Local GTFS stops are id-namespaced by source to avoid collisions with live API's global_stop_id */
internal fun GtfsStopEntity.toTripStop() = TripStop(
    globalStopId = "gtfs:$sourceId:$stopId",
    name = name,
    lat = lat,
    lon = lon,
)

/** Inverse of [toTripStop]'s globalStopId. Null if [globalStopId] isn't a local-GTFS id. */
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

        // use grid-aligned indices
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
