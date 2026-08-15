package dev.garado.transit.data.database.searchcache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface SearchCacheDao {
    @Query("SELECT * FROM search_cache WHERE key = :key AND cached_at >= :minCachedAt")
    suspend fun get(key: String, minCachedAt: Long): SearchCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SearchCacheEntity)

    @Query("DELETE FROM search_cache WHERE cached_at < :minCachedAt")
    suspend fun deleteExpired(minCachedAt: Long)
}
