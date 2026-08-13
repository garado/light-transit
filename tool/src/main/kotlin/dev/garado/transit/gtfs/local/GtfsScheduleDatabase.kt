/** Database for trip schedules imported from downloaded GTFS static feeds */

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

@Entity(
    tableName = "gtfs_routes",
    primaryKeys = ["source_id", "route_id"],
)
internal data class GtfsRouteEntity(
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "route_id") val routeId: String,
    val name: String,
    val color: String?,
    @ColumnInfo(name = "text_color") val textColor: String?,
)

@Entity(
    tableName = "gtfs_trips",
    primaryKeys = ["source_id", "trip_id"],
    indices = [Index("source_id", "service_id")],
)
internal data class GtfsTripEntity(
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "trip_id") val tripId: String,
    @ColumnInfo(name = "route_id") val routeId: String,
    @ColumnInfo(name = "service_id") val serviceId: String,
    val headsign: String?,
)

/**
 * [departureSeconds] is seconds since local midnight
 * GTFS allows values >= 86400 for after-midnight trips
 */
@Entity(
    tableName = "gtfs_stop_times",
    primaryKeys = ["source_id", "trip_id", "stop_sequence"],
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

    @Query("DELETE FROM gtfs_routes WHERE source_id = :sourceId")
    suspend fun deleteRoutesBySource(sourceId: Long)

    @Query("DELETE FROM gtfs_trips WHERE source_id = :sourceId")
    suspend fun deleteTripsBySource(sourceId: Long)

    @Query("DELETE FROM gtfs_stop_times WHERE source_id = :sourceId")
    suspend fun deleteStopTimesBySource(sourceId: Long)

    @Query("DELETE FROM gtfs_calendar WHERE source_id = :sourceId")
    suspend fun deleteCalendarsBySource(sourceId: Long)

    suspend fun deleteBySource(sourceId: Long) {
        deleteRoutesBySource(sourceId)
        deleteTripsBySource(sourceId)
        deleteStopTimesBySource(sourceId)
        deleteCalendarsBySource(sourceId)
    }

    @Query("DELETE FROM gtfs_routes")
    suspend fun clearRoutes()

    @Query("DELETE FROM gtfs_trips")
    suspend fun clearTrips()

    @Query("DELETE FROM gtfs_stop_times")
    suspend fun clearStopTimes()

    @Query("DELETE FROM gtfs_calendar")
    suspend fun clearCalendars()

    suspend fun clear() {
        clearRoutes()
        clearTrips()
        clearStopTimes()
        clearCalendars()
    }
}

@Database(
    entities = [GtfsRouteEntity::class, GtfsTripEntity::class, GtfsStopTimeEntity::class, GtfsCalendarEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class GtfsScheduleDatabase : RoomDatabase() {
    abstract fun gtfsScheduleDao(): GtfsScheduleDao
}

/** Process-wide singleton */
internal object GtfsScheduleDatabaseHolder {
    @Volatile
    private var instance: GtfsScheduleDatabase? = null

    fun get(lightContext: SealedLightContext): GtfsScheduleDatabase =
        instance ?: synchronized(this) {
            instance ?: lightContext.buildDatabase(GtfsScheduleDatabase::class.java, "gtfs_schedule.db")
                .also { instance = it }
        }
}
