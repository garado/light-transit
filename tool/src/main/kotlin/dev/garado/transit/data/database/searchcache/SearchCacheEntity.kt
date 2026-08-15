package dev.garado.transit.data.database.searchcache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One cached Nominatim search response, keyed by normalized query+origin. */
@Entity(tableName = "search_cache")
internal data class SearchCacheEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "results_json") val resultsJson: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long,
)
