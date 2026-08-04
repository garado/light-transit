package dev.garado.transit.map

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One cached raster tile, keyed by "style/z/x/y" */
@Entity(tableName = "tiles")
internal data class TileEntity(
    @PrimaryKey val key: String,
    val bytes: ByteArray,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "last_accessed") val lastAccessed: Long,
)
