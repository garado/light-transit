package dev.garado.transit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** Standard Web Mercator "slippy map" tile size (pixels per tile at any zoom). */
const val TILE_SIZE = 256.0

/** Fractional (x, y) tile coordinates for (lat, lon) at the given integer zoom. */
fun lonLatToTileFraction(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
    val n = Math.pow(2.0, zoom.toDouble())
    val x = (lon + 180.0) / 360.0 * n
    val latRad = Math.toRadians(lat)
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
    return x to y
}

/**
 * Ground resolution (meters/pixel) at [lat] and [zoom] -- [zoom] may be fractional (e.g. mid-pinch,
 * between two integer tile zooms); the formula is continuous, so there's no need to round it first.
 */
fun metersPerPixel(lat: Double, zoom: Double): Double {
    val earthCircumferenceMeters = 40_075_016.686
    val latRad = Math.toRadians(lat)
    return (earthCircumferenceMeters * cos(latRad)) / (TILE_SIZE * Math.pow(2.0, zoom))
}

/** Integer-zoom convenience overload -- see the [Double] version for the actual formula. */
fun metersPerPixel(lat: Double, zoom: Int): Double = metersPerPixel(lat, zoom.toDouble())

/** One fetched map tile and its integer tile coordinates at the map's zoom level. */
data class FetchedTile(val tileX: Int, val tileY: Int, val bitmap: Bitmap)

/**
 * Every tile fetched to cover the requested area around one center point. Each tile is drawn
 * independently at its own screen offset (computed live from the current center, since that can
 * keep panning/zooming between fetches) -- there's no pre-stitched composite bitmap, so a handful
 * of individually-failed tiles just leave that one patch blank instead of requiring a grid size
 * guessed to be "big enough" up front.
 */
data class MapTiles(val zoom: Int, val tiles: List<FetchedTile>)

/**
 * A small in-process LRU of decoded tile bitmaps, keyed by "style/z/x/y", shared by every
 * [MapTileClient] instance -- panning back over an already-fetched area reuses tiles already
 * downloaded instead of refetching them. Capacity is a tile count, not a byte budget, since every
 * raster tile is the same 256x256 size.
 */
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
 * Fetches individual raster tiles from one of CARTO's free basemaps (built on OpenStreetMap data),
 * caching decoded bitmaps in [TileCache]. A real, descriptive User-Agent is sent on every request,
 * and on-screen "© OpenStreetMap contributors © CARTO" attribution is required wherever these
 * tiles are displayed -- see TransitMapView's Content().
 */
class MapTileClient {
    private val client = HttpClient(OkHttp)

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

    /**
     * Every tile needed to cover a [halfWidthMeters] x [halfHeightMeters] rectangle around (lat, lon)
     * at [zoom], fetched concurrently. Individual tile failures are logged and simply omitted from
     * the result -- never fail the whole map for one bad tile. Sized to the actual viewport rectangle
     * (rather than a square built from the larger of the two dimensions) so a screen fetches only the
     * tiles it can actually show, plus [COVERAGE_MARGIN] headroom.
     */
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

    private suspend fun fetchTile(x: Int, y: Int, zoom: Int, darkMode: Boolean): Bitmap? {
        val style = if (darkMode) "dark" else "voyager"
        val key = "$style/$zoom/$x/$y"
        TileCache.get(key)?.let { return it }

        val baseUrl = (if (darkMode) DARK_BASE_URL else VOYAGER_BASE_URL)
            .replace("{s}", subdomainFor(x, y).toString())
        return try {
            val response = client.get("$baseUrl/$zoom/$x/$y.png") {
                header("User-Agent", USER_AGENT)
            }
            if (!response.status.isSuccess()) return null
            val bytes: ByteArray = response.body()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.also { TileCache.put(key, it) }
        } catch (e: Exception) {
            Log.e("MapTileClient", "Tile fetch failed for $key", e)
            null
        }
    }

    fun close() {
        client.close()
    }
}
