/**
 * Fetches raster map tiles from OpenStreetMap.
 *
 * - Tiles fetched from Carto
 * - Cached to a local SQLite database
 */

package dev.garado.transit.view.components.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.geometry.Offset
import dev.garado.transit.data.database.maptiles.TileCacheDatabase
import dev.garado.transit.data.database.maptiles.TileDao
import dev.garado.transit.data.database.maptiles.TileEntity
import dev.garado.transit.util.TILE_SIZE
import dev.garado.transit.util.lonLatToTileFraction
import dev.garado.transit.util.metersPerPixel
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** A raster tile: a decoded bitmap at some integer tile coordinate */
data class RasterMapTile(override val tileX: Int, override val tileY: Int, val bitmap: Bitmap) : MapTile

/** LRU of decoded tile bitmaps, keyed by "style/z/x/y" */
private object TileCache {
    private const val MAX_ENTRIES = 300
    private val entries = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>) = size > MAX_ENTRIES
    }

    @Synchronized
    fun get(key: String): Bitmap? = entries[key]

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        entries[key] = bitmap
    }
}

/** Local tile cache backed by SQLite (via Room), keyed with "style/z/x/y" */
private class DiskTileCache(private val dao: TileDao) {
    suspend fun get(key: String): ByteArray? {
        val bytes = dao.getBytes(key) ?: return null
        dao.touch(key, System.currentTimeMillis())
        return bytes
    }

    suspend fun put(key: String, bytes: ByteArray) {
        dao.upsert(TileEntity(key, bytes, bytes.size.toLong(), System.currentTimeMillis()))
        evictIfOverBudget()
    }

    /** Deletes oldest-accessed tiles in batches until back under [MAX_CACHE_BYTES] */
    private suspend fun evictIfOverBudget() {
        while (dao.totalSize() > MAX_CACHE_BYTES) {
            val oldest = dao.oldestKeys(EVICTION_BATCH_SIZE)
            if (oldest.isEmpty()) break
            dao.deleteByKeys(oldest)
        }
    }

    companion object {
        private const val MAX_CACHE_BYTES = 1_073_741_824L // 1 GiB
        private const val EVICTION_BATCH_SIZE = 50
    }
}

/** [MapTileSource] backed by raster tiles */
class RasterTileSource(database: TileCacheDatabase) : MapTileSource {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
    }
    private val diskCache = DiskTileCache(database.tileDao())

    companion object {
        private const val VOYAGER_BASE_URL = "https://{s}.basemaps.cartocdn.com/rastertiles/voyager"
        private const val DARK_BASE_URL = "https://{s}.basemaps.cartocdn.com/dark_all"
        private const val SUBDOMAINS = "abcd"
        private const val USER_AGENT = "light-transit"
        private const val COVERAGE_MARGIN = 1.15
        private const val CONNECT_TIMEOUT_MS = 5_000L
        private const val REQUEST_TIMEOUT_MS = 10_000L

        private fun subdomainFor(x: Int, y: Int): Char = SUBDOMAINS[Math.floorMod(x + y, SUBDOMAINS.length)]
    }

    override suspend fun fetchTilesAround(
        lat: Double,
        lon: Double,
        zoom: Int,
        halfWidthMeters: Double,
        halfHeightMeters: Double,
        darkMode: Boolean,
        onTileReady: (MapTile) -> Unit,
    ): MapTileBatch = coroutineScope {
        val (centerFracX, centerFracY) = lonLatToTileFraction(lat, lon, zoom)
        val metersPerPx = metersPerPixel(lat, zoom)
        val radiusTilesX = ceil((halfWidthMeters / metersPerPx * COVERAGE_MARGIN) / TILE_SIZE).toInt().coerceAtLeast(1)
        val radiusTilesY = ceil((halfHeightMeters / metersPerPx * COVERAGE_MARGIN) / TILE_SIZE).toInt().coerceAtLeast(1)
        val centerTileX = floor(centerFracX).toInt()
        val centerTileY = floor(centerFracY).toInt()
        val maxTileIndex = (1 shl zoom) - 1

        // antimeridian handling
        val tileCoords = buildList {
            for (tileX in (centerTileX - radiusTilesX)..(centerTileX + radiusTilesX)) {
                for (tileY in (centerTileY - radiusTilesY)..(centerTileY + radiusTilesY)) {
                    if (tileY in 0..maxTileIndex) add(tileX to tileY)
                }
            }
        }

        val tiles = tileCoords
            .map { (tileX, tileY) ->
                async {
                    fetchTile(tileX, tileY, zoom, darkMode)
                        ?.let { RasterMapTile(tileX, tileY, it) }
                        ?.also { onTileReady(it) }
                }
            }
            .mapNotNull { it.await() }

        MapTileBatch(zoom, tiles, tileCoords.toSet())
    }

    /** Fetch an individual tile; [x] is wrapped around the antimeridian before use */
    private suspend fun fetchTile(x: Int, y: Int, zoom: Int, darkMode: Boolean): Bitmap? {
        val wrappedX = Math.floorMod(x, 1 shl zoom)
        val style = if (darkMode) "dark" else "voyager"
        val key = "$style/$zoom/$wrappedX/$y"
        TileCache.get(key)?.let { return it }

        diskCache.get(key)?.let { bytes ->
            return withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }?.also { TileCache.put(key, it) }
        }

        val baseUrl = (if (darkMode) DARK_BASE_URL else VOYAGER_BASE_URL)
            .replace("{s}", subdomainFor(wrappedX, y).toString())
        return try {
            val response = client.get("$baseUrl/$zoom/$wrappedX/$y.png") {
                header("User-Agent", USER_AGENT)
            }
            if (!response.status.isSuccess()) return null
            val bytes: ByteArray = response.body()
            val bitmap = withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } ?: return null
            TileCache.put(key, bitmap)
            diskCache.put(key, bytes)
            bitmap
        } catch (e: Exception) {
            Log.e("RasterTileSource", "Tile fetch failed for $key", e)
            null
        }
    }

    override fun DrawScope.drawTile(tile: MapTile, offset: Offset, sizePx: Int, colorFilter: ColorFilter?) {
        if (tile !is RasterMapTile) return
        drawImage(
            image = tile.bitmap.asImageBitmap(),
            dstOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
            dstSize = IntSize(sizePx, sizePx),
            colorFilter = colorFilter,
        )
    }

    override fun close() {
        client.close()
    }
}
