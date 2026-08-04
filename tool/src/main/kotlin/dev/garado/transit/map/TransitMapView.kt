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
fun TransitMapView(isDarkTheme: Boolean, modifier: Modifier = Modifier) {
    var centerLat by remember { mutableStateOf(DEFAULT_LAT) }
    var centerLon by remember { mutableStateOf(DEFAULT_LON) }
    var zoom by remember { mutableStateOf(DEFAULT_ZOOM) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var mapTiles by remember { mutableStateOf<MapTiles?>(null) }

    val tileClient = remember { MapTileClient() }
    DisposableEffect(Unit) {
        onDispose { tileClient.close() }
    }

    suspend fun refetch() {
        if (canvasSize == IntSize.Zero) return
        val tileZoom = zoom.roundToInt().coerceIn(MIN_ZOOM.toInt(), MAX_ZOOM.toInt())
        val metersPerPx = metersPerPixel(centerLat, tileZoom)
        val halfWidthMeters = (canvasSize.width / 2f) * metersPerPx
        val halfHeightMeters = (canvasSize.height / 2f) * metersPerPx
        mapTiles = tileClient.fetchTilesAround(centerLat, centerLon, tileZoom, halfWidthMeters.toDouble(), halfHeightMeters.toDouble(), isDarkTheme)
    }

    // Initial fetch once the canvas has a real size, and again if the theme (map style) changes.
    LaunchedEffect(canvasSize, isDarkTheme) {
        refetch()
    }

    // Debounced refetch as the user pans/zooms -- the canvas redraws immediately every frame from
    // live centerLat/centerLon/zoom (see the draw loop below); this just keeps the underlying tile
    // set current once a gesture settles, rather than refetching on every intermediate frame.
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
                        // Screen-right/up pans should reveal what's west/south of center, so the
                        // center itself moves the opposite direction of the drag.
                        centerLon -= (pan.x * metersPerPx) / (METERS_PER_DEGREE_LAT * cos(Math.toRadians(centerLat)))
                        centerLat += (pan.y * metersPerPx) / METERS_PER_DEGREE_LAT
                        if (gestureZoom != 1f) {
                            zoom = (zoom + (ln(gestureZoom.toDouble()) / ln(2.0)).toFloat()).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        }
                    }
                },
        ) {
            val tiles = mapTiles ?: return@Canvas
            val (liveFracX, liveFracY) = lonLatToTileFraction(centerLat, centerLon, tiles.zoom)
            val scale = 2.0.pow((zoom - tiles.zoom).toDouble()).toFloat()
            val drawSize = (TILE_SIZE * scale).toFloat()
            val drawSizeInt = drawSize.roundToInt().coerceAtLeast(1)

            translate(left = size.width / 2f, top = size.height / 2f) {
                // (tile.tileX, tile.tileY) is the tile's own top-left corner in tile-space (the
                // slippy-map convention), so this offset -- its corner's screen position relative to
                // the live center -- IS the dstOffset directly; no additional half-tile-size
                // centering is needed (a tile isn't a point-anchored icon).
                for (tile in tiles.tiles) {
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
