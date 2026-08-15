package dev.garado.transit.data.database.maptiles

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TileEntity::class], version = 1, exportSchema = false)
abstract class TileCacheDatabase : RoomDatabase() {
    internal abstract fun tileDao(): TileDao
}
