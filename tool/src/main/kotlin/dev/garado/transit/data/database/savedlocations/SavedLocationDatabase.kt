/** Set up room db for saved locations */

package dev.garado.transit.data.database.savedlocations

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

@Entity(tableName = "saved_locations")
internal data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "display_name") val displayName: String,
    val title: String,
    val address: String,
    val lat: Double,
    val lon: Double,
)

@Dao
internal interface SavedLocationDao {
    @Query("SELECT * FROM saved_locations ORDER BY id ASC")
    fun getAll(): Flow<List<SavedLocationEntity>>

    @Insert
    suspend fun insert(entity: SavedLocationEntity): Long

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [SavedLocationEntity::class], version = 1, exportSchema = false)
abstract class SavedLocationDatabase : RoomDatabase() {
    internal abstract fun savedLocationDao(): SavedLocationDao
}

/** Process-wide singleton database */
object SavedLocationDatabaseHolder {
    @Volatile
    private var instance: SavedLocationDatabase? = null

    fun get(lightContext: SealedLightContext): SavedLocationDatabase =
        instance ?: synchronized(this) {
            instance ?: lightContext.buildDatabase(SavedLocationDatabase::class.java, "saved_locations.db")
                .also { instance = it }
        }
}
