/** Set up room db for the (parsed) Transitous dataset catalog */

package dev.garado.transit.gtfs.browse

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase
import dev.garado.transit.gtfs.GtfsDataset
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "gtfs_catalog_datasets")
internal data class GtfsCatalogEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "region_code") val regionCode: String,
    val path: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long?,
)

@Dao
internal interface GtfsCatalogDao {
    @Query("SELECT * FROM gtfs_catalog_datasets")
    fun getAll(): Flow<List<GtfsCatalogEntity>>

    @Query("SELECT COUNT(*) FROM gtfs_catalog_datasets")
    suspend fun count(): Int

    @Query("DELETE FROM gtfs_catalog_datasets")
    suspend fun clear()

    @Insert
    suspend fun insertAll(entities: List<GtfsCatalogEntity>)

    @Transaction
    suspend fun replaceAll(entities: List<GtfsCatalogEntity>) {
        clear()
        insertAll(entities)
    }
}

@Database(entities = [GtfsCatalogEntity::class], version = 1, exportSchema = false)
abstract class GtfsCatalogDatabase : RoomDatabase() {
    internal abstract fun gtfsCatalogDao(): GtfsCatalogDao
}

/** Process-wide singleton database */
object GtfsCatalogDatabaseHolder {
    @Volatile
    private var instance: GtfsCatalogDatabase? = null

    fun get(lightContext: SealedLightContext): GtfsCatalogDatabase =
        instance ?: synchronized(this) {
            instance ?: lightContext.buildDatabase(GtfsCatalogDatabase::class.java, "gtfs_catalog.db")
                .also { instance = it }
        }
}

internal fun GtfsCatalogEntity.toGtfsDataset() = GtfsDataset(key = key, regionCode = regionCode, path = path, sizeBytes = sizeBytes)

internal fun GtfsDataset.toEntity() = GtfsCatalogEntity(key = key, regionCode = regionCode, path = path, sizeBytes = sizeBytes)
