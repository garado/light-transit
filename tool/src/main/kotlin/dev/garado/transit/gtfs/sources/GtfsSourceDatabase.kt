/** Set up room db for GTFS sources */

package dev.garado.transit.gtfs.sources

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase
import kotlinx.coroutines.flow.Flow

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

@Database(entities = [GtfsSourceEntity::class], version = 2, exportSchema = false)
abstract class GtfsSourceDatabase : RoomDatabase() {
    internal abstract fun gtfsSourceDao(): GtfsSourceDao
}

/** Process-wide singleton database */
object GtfsSourceDatabaseHolder {
    @Volatile
    private var instance: GtfsSourceDatabase? = null

    fun get(lightContext: SealedLightContext): GtfsSourceDatabase =
        instance ?: synchronized(this) {
            instance ?: lightContext.buildDatabase(GtfsSourceDatabase::class.java, "gtfs_sources.db")
                .also { instance = it }
        }
}
