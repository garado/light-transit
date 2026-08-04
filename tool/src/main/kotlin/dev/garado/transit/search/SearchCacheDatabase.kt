package dev.garado.transit.search

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SearchCacheEntity::class], version = 1, exportSchema = false)
abstract class SearchCacheDatabase : RoomDatabase() {
    internal abstract fun searchCacheDao(): SearchCacheDao
}
