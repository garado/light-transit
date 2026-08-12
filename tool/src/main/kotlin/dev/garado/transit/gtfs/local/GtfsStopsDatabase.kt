/** Set up room db for stops imported from downloaded GTFS static feeds (stops.txt) */

package dev.garado.transit.gtfs.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase
import dev.garado.transit.api.models.TripStop

@Entity(
    tableName = "gtfs_stops",
    indices = [Index("lat"), Index("lon"), Index(value = ["source_id", "stop_id"], unique = true)],
)
internal data class GtfsStopEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rtree_id") val rtreeId: Long = 0,
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "stop_id") val stopId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
)

@Dao
internal interface GtfsStopsDao {
    @Insert
    suspend fun insertGtfsStops(entities: List<GtfsStopEntity>): List<Long>

    @Query("INSERT INTO gtfs_stops_rtree (id, minLat, maxLat, minLon, maxLon) VALUES (:rtreeId, :lat, :lat, :lon, :lon)")
    suspend fun insertRtreeEntry(rtreeId: Long, lat: Double, lon: Double)

    @Transaction
    suspend fun insertAll(entities: List<GtfsStopEntity>) {
        val generatedIds = insertGtfsStops(entities)
        entities.forEachIndexed { index, entity ->
            insertRtreeEntry(generatedIds[index], entity.lat, entity.lon)
        }
    }

    @Query("DELETE FROM gtfs_stops WHERE source_id = :sourceId")
    suspend fun deleteBySource(sourceId: Long)

    @Query("DELETE FROM gtfs_stops")
    suspend fun clear()

    @Query("SELECT * FROM gtfs_stops WHERE lat BETWEEN :minLat AND :maxLat AND lon BETWEEN :minLon AND :maxLon")
    suspend fun nearby(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<GtfsStopEntity>
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
                .also {
                    // Room has no annotation support for rtree virtual tables, so it's created here directly
                    it.openHelper.writableDatabase.execSQL(
                        "CREATE VIRTUAL TABLE IF NOT EXISTS gtfs_stops_rtree USING rtree(id, minLat, maxLat, minLon, maxLon)"
                    )
                    instance = it
                }
        }
}

/** Local GTFS stops are id-namespaced by source so they never collide with live API's global_stop_id */
internal fun GtfsStopEntity.toTripStop() = TripStop(
    globalStopId = "gtfs:$sourceId:$stopId",
    name = name,
    lat = lat,
    lon = lon,
)
