/**
 * Core map UI implementation.
 */

package dev.garado.transit.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.io.File
import kotlinx.coroutines.flow.debounce
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MIN_ZOOM = 3f
private const val MAX_ZOOM = 19f
private const val DEFAULT_ZOOM = 14f
private const val REFETCH_DEBOUNCE_MS = 400L
private const val METERS_PER_DEGREE_LAT = 111_320.0

private const val DEFAULT_LAT = 40.7128 // NYC
private const val DEFAULT_LON = -74.0060

@Composable
fun TransitMapView(isDarkTheme: Boolean, cacheDir: File, modifier: Modifier = Modifier) {
    var centerLat by remember { mutableStateOf(DEFAULT_LAT) }
    var centerLon by remember { mutableStateOf(DEFAULT_LON) }
    var zoom by remember { mutableStateOf(DEFAULT_ZOOM) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var tileZoomLevel by remember { mutableStateOf<Int?>(null) }

    // currently displayed tiles
    val liveTiles = remember { mutableStateMapOf<Pair<Int, Int>, FetchedTile>() }

    val tileClient = remember { MapTileClient(cacheDir) }
    DisposableEffect(Unit) {
        onDispose { tileClient.close() }
    }

    suspend fun refetch() {
        if (canvasSize == IntSize.Zero) return
        val tileZoom = zoom.roundToInt().coerceIn(MIN_ZOOM.toInt(), MAX_ZOOM.toInt())
        val metersPerPx = metersPerPixel(centerLat, tileZoom)
        val halfWidthMeters = (canvasSize.width / 2f) * metersPerPx
        val halfHeightMeters = (canvasSize.height / 2f) * metersPerPx

        if (tileZoomLevel != tileZoom) {
            liveTiles.clear()
            tileZoomLevel = tileZoom
        }

        val result = tileClient.fetchTilesAround(
            centerLat, centerLon, tileZoom, halfWidthMeters.toDouble(), halfHeightMeters.toDouble(), isDarkTheme,
        ) { tile -> liveTiles[tile.tileX to tile.tileY] = tile }

        // prune stale tiles
        liveTiles.keys.retainAll(result.requestedKeys)
    }

    // fetch on theme change + canvas init
    LaunchedEffect(canvasSize, isDarkTheme) {
        refetch()
    }

    // debounced refetch during pan/zoom
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(centerLat, centerLon, zoom) }
            .debounce(REFETCH_DEBOUNCE_MS)
            .collect { refetch() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        val metersPerPx = metersPerPixel(centerLat, zoom.toDouble())
                        centerLon -= (pan.x * metersPerPx) / (METERS_PER_DEGREE_LAT * cos(Math.toRadians(centerLat)))
                        centerLat += (pan.y * metersPerPx) / METERS_PER_DEGREE_LAT
                        if (gestureZoom != 1f) {
                            zoom = (zoom + (ln(gestureZoom.toDouble()) / ln(2.0)).toFloat()).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        }
                    }
                },
        ) {
            val tilesZoom = tileZoomLevel ?: return@Canvas
            val (liveFracX, liveFracY) = lonLatToTileFraction(centerLat, centerLon, tilesZoom)
            val scale = 2.0.pow((zoom - tilesZoom).toDouble()).toFloat()
            val drawSize = (TILE_SIZE * scale).toFloat()
            val drawSizeInt = drawSize.roundToInt().coerceAtLeast(1)

            translate(left = size.width / 2f, top = size.height / 2f) {
                for (tile in liveTiles.values) {
                    val offsetX = ((tile.tileX - liveFracX) * TILE_SIZE).toFloat() * scale
                    val offsetY = ((tile.tileY - liveFracY) * TILE_SIZE).toFloat() * scale
                    drawImage(
                        image = tile.bitmap.asImageBitmap(),
                        dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                        dstSize = IntSize(drawSizeInt, drawSizeInt),
                    )
                }
            }
        }
    }
}
