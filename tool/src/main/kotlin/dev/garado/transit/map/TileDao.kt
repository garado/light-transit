package dev.garado.transit.map

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface TileDao {
    @Query("SELECT bytes FROM tiles WHERE key = :key")
    suspend fun getBytes(key: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tile: TileEntity)

    @Query("UPDATE tiles SET last_accessed = :now WHERE key = :key")
    suspend fun touch(key: String, now: Long)

    @Query("SELECT COALESCE(SUM(size_bytes), 0) FROM tiles")
    suspend fun totalSize(): Long

    @Query("SELECT key FROM tiles ORDER BY last_accessed ASC LIMIT :limit")
    suspend fun oldestKeys(limit: Int): List<String>

    @Query("DELETE FROM tiles WHERE key IN (:keys)")
    suspend fun deleteByKeys(keys: List<String>)
}
