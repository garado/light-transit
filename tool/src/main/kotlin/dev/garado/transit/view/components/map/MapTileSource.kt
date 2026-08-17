package dev.garado.transit.view.components.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope

/** One fetched tile at some integer tile coordinate */
interface MapTile {
    val tileX: Int
    val tileY: Int
}

/** Every tile fetched for one viewport, plus every (x, y) requested, including failures */
data class MapTileBatch(val zoom: Int, val tiles: List<MapTile>, val requestedKeys: Set<Pair<Int, Int>>)

/**
 * Supplies map tiles and knows how to draw them, independent of tile format.
 * [TransitMapView] only depends on this interface, never on a specific tile format
 */
interface MapTileSource {
    /** Fetch tiles covering a viewport centered at (lat, lon) */
    suspend fun fetchTilesAround(
        lat: Double,
        lon: Double,
        zoom: Int,
        halfWidthMeters: Double,
        halfHeightMeters: Double,
        darkMode: Boolean,
        onTileReady: (MapTile) -> Unit = {},
    ): MapTileBatch

    /** Draw one tile at [offset], scaled to [sizePx] on a side */
    fun DrawScope.drawTile(tile: MapTile, offset: Offset, sizePx: Int, colorFilter: ColorFilter? = null)

    fun close()
}
