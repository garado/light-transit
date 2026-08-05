/** Set up room db for saved locations */

package dev.garado.transit.search

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
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

    @Delete
    suspend fun delete(entity: SavedLocationEntity)
}

@Database(entities = [SavedLocationEntity::class], version = 1, exportSchema = false)
abstract class SavedLocationDatabase : RoomDatabase() {
    internal abstract fun savedLocationDao(): SavedLocationDao
}
