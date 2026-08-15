package dev.garado.transit.data.database.searchcache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SearchCacheEntity::class], version = 1, exportSchema = false)
abstract class SearchCacheDatabase : RoomDatabase() {
    internal abstract fun searchCacheDao(): SearchCacheDao
}
