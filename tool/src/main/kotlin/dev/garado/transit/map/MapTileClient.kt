/**
 * Fetches raster map tiles from OpenStreetMap.
 *
 * - Tiles fetched from Carto
 * - Cached to disk
 */

package dev.garado.transit.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor

/** One fetched map tile and its integer tile coordinates at the map's zoom level */
data class FetchedTile(val tileX: Int, val tileY: Int, val bitmap: Bitmap)

/** Every tile around one center point */
data class MapTiles(val zoom: Int, val tiles: List<FetchedTile>)

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

/**
 * Local tile cache, keyed with "style/z/x/y"
 *
 * @param cacheDir Directory where tiles will be cached.
 */
private class DiskTileCache(cacheDir: File) {
    private val tileDir = File(cacheDir, "tile_cache")

    private fun fileFor(key: String): File = File(tileDir, key.replace('/', '_') + ".png")

    suspend fun get(key: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = fileFor(key)
        if (file.exists()) file.readBytes() else null
    }

    suspend fun put(key: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        tileDir.mkdirs()
        fileFor(key).writeBytes(bytes)
        evictIfOverBudget()
    }

    /** Deletes oldest-modified files first until the directory is back under [MAX_CACHE_BYTES] */
    private fun evictIfOverBudget() {
        val files = tileDir.listFiles() ?: return
        var totalBytes = files.sumOf { it.length() }
        if (totalBytes <= MAX_CACHE_BYTES) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (totalBytes <= MAX_CACHE_BYTES) break
            totalBytes -= file.length()
            file.delete()
        }
    }

    companion object {
        private const val MAX_CACHE_BYTES = 1_073_741_824L // 1 GiB
    }
}

/** Handles fetching raster tiles */
class MapTileClient(cacheDir: File) {
    private val client = HttpClient(OkHttp)
    private val diskCache = DiskTileCache(cacheDir)

    companion object {
        // {s} shards requests across CARTO's 4 tile subdomains (see subdomainFor) so a screenful of
        // tiles isn't bottlenecked behind one host's HTTP connection limit.
        private const val VOYAGER_BASE_URL = "https://{s}.basemaps.cartocdn.com/rastertiles/voyager"
        private const val DARK_BASE_URL = "https://{s}.basemaps.cartocdn.com/dark_all"
        private const val SUBDOMAINS = "abcd"
        private const val USER_AGENT = "LightTransitTool/1.0 (+https://github.com/lightphone)"
        // Fetched area is this much larger than the requested half-extents, so a little panning
        // headroom exists beyond exactly what's on screen right now.
        private const val COVERAGE_MARGIN = 1.15

        private fun subdomainFor(x: Int, y: Int): Char = SUBDOMAINS[Math.floorMod(x + y, SUBDOMAINS.length)]
    }

    /** Fetch tiles around a specific point */
    suspend fun fetchTilesAround(
        lat: Double,
        lon: Double,
        zoom: Int,
        halfWidthMeters: Double,
        halfHeightMeters: Double,
        darkMode: Boolean,
    ): MapTiles = coroutineScope {
        val (centerFracX, centerFracY) = lonLatToTileFraction(lat, lon, zoom)
        val metersPerPx = metersPerPixel(lat, zoom)
        val radiusTilesX = ceil((halfWidthMeters / metersPerPx * COVERAGE_MARGIN) / TILE_SIZE).toInt().coerceAtLeast(1)
        val radiusTilesY = ceil((halfHeightMeters / metersPerPx * COVERAGE_MARGIN) / TILE_SIZE).toInt().coerceAtLeast(1)
        val centerTileX = floor(centerFracX).toInt()
        val centerTileY = floor(centerFracY).toInt()

        val tileCoords = buildList {
            for (tileX in (centerTileX - radiusTilesX)..(centerTileX + radiusTilesX)) {
                for (tileY in (centerTileY - radiusTilesY)..(centerTileY + radiusTilesY)) {
                    add(tileX to tileY)
                }
            }
        }
        val tiles = tileCoords
            .map { (tileX, tileY) -> async { fetchTile(tileX, tileY, zoom, darkMode)?.let { FetchedTile(tileX, tileY, it) } } }
            .mapNotNull { it.await() }

        MapTiles(zoom, tiles)
    }

    /** Fetch an individual tile */
    private suspend fun fetchTile(x: Int, y: Int, zoom: Int, darkMode: Boolean): Bitmap? {
        val style = if (darkMode) "dark" else "voyager"
        val key = "$style/$zoom/$x/$y"
        TileCache.get(key)?.let { return it }

        diskCache.get(key)?.let { bytes ->
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.also { TileCache.put(key, it) }
        }

        val baseUrl = (if (darkMode) DARK_BASE_URL else VOYAGER_BASE_URL)
            .replace("{s}", subdomainFor(x, y).toString())
        return try {
            val response = client.get("$baseUrl/$zoom/$x/$y.png") {
                header("User-Agent", USER_AGENT)
            }
            if (!response.status.isSuccess()) return null
            val bytes: ByteArray = response.body()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            TileCache.put(key, bitmap)
            diskCache.put(key, bytes)
            bitmap
        } catch (e: Exception) {
            Log.e("MapTileClient", "Tile fetch failed for $key", e)
            null
        }
    }

    fun close() {
        client.close()
    }
}
