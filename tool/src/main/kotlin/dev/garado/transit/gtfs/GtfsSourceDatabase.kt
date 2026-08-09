/** Set up room db for GTFS sources */

package dev.garado.transit.gtfs

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
)

@Dao
internal interface GtfsSourceDao {
    @Query("SELECT * FROM gtfs_sources ORDER BY id ASC")
    fun getAll(): Flow<List<GtfsSourceEntity>>

    @Insert
    suspend fun insert(entity: GtfsSourceEntity): Long

    @Query("DELETE FROM gtfs_sources WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [GtfsSourceEntity::class], version = 1, exportSchema = false)
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
