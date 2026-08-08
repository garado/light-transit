/**
 * Core map UI implementation; tile-format-agnostic
 */

package dev.garado.transit.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
private val MARKER_TOUCH_TARGET_RADIUS = 24.dp

private val DEFAULT_CENTER = LatLon(lat = 40.7128, lon = -74.0060) // NYC

@Composable
fun TransitMapView(
    isDarkTheme: Boolean,
    tileSource: MapTileSource,
    initialCenter: LatLon = DEFAULT_CENTER,
    overlays: List<MapOverlay> = emptyList(),
    fitBounds: LatLonBounds? = null,
    onMarkerClick: ((MapOverlay.Marker) -> Unit)? = null,
    onCenterChanged: ((LatLon) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var centerLat by remember(initialCenter) { mutableStateOf(initialCenter.lat) }
    var centerLon by remember(initialCenter) { mutableStateOf(initialCenter.lon) }
    var zoom by remember { mutableStateOf(DEFAULT_ZOOM) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var tileZoomLevel by remember { mutableStateOf<Int?>(null) }

    // Report map's current center position
    LaunchedEffect(onCenterChanged) {
        if (onCenterChanged == null) return@LaunchedEffect
        snapshotFlow { LatLon(lat = centerLat, lon = centerLon) }
            .debounce(REFETCH_DEBOUNCE_MS)
            .collect { onCenterChanged(it) }
    }

    // snap to fit bounds on init
    LaunchedEffect(fitBounds, canvasSize) {
        if (fitBounds == null || canvasSize == IntSize.Zero) return@LaunchedEffect
        centerLat = fitBounds.center.lat
        centerLon = fitBounds.center.lon
        zoom = zoomToFit(fitBounds, canvasSize).toFloat().coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    // currently displayed tiles
    val liveTiles = remember { mutableStateMapOf<Pair<Int, Int>, MapTile>() }

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

        val result = tileSource.fetchTilesAround(
            centerLat, centerLon, tileZoom, halfWidthMeters.toDouble(), halfHeightMeters.toDouble(), isDarkTheme,
        ) { tile -> liveTiles[tile.tileX to tile.tileY] = tile }

        // prune stale tiles
        liveTiles.keys.retainAll(result.requestedKeys)
    }

    // fetch on theme change + canvas init
    LaunchedEffect(canvasSize, isDarkTheme) {
        liveTiles.clear()
        tileZoomLevel = null
        refetch()
    }

    // debounced refetch during pan/zoom
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(centerLat, centerLon, zoom) }
            .debounce(REFETCH_DEBOUNCE_MS)
            .collect { refetch() }
    }

    fun screenOffsetFor(lat: Double, lon: Double): Offset? {
        val tilesZoom = tileZoomLevel ?: return null
        val (liveFracX, liveFracY) = lonLatToTileFraction(centerLat, centerLon, tilesZoom)
        val scale = 2.0.pow((zoom - tilesZoom).toDouble()).toFloat()
        val relative = lonLatToOffset(lat, lon, tilesZoom, liveFracX, liveFracY, scale)
        return Offset(relative.x + canvasSize.width / 2f, relative.y + canvasSize.height / 2f)
    }

    val touchTargetPx = with(LocalDensity.current) { MARKER_TOUCH_TARGET_RADIUS.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
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
                }
                .pointerInput(onMarkerClick, overlays, touchTargetPx) {
                    if (onMarkerClick == null) return@pointerInput
                    detectTapGestures { tapOffset ->
                        overlays.filterIsInstance<MapOverlay.Marker>()
                            .mapNotNull { marker -> screenOffsetFor(marker.point.lat, marker.point.lon)?.let { marker to (it - tapOffset).getDistance() } }
                            .minByOrNull { (_, distance) -> distance }
                            ?.takeIf { (_, distance) -> distance <= touchTargetPx }
                            ?.let { (marker, _) -> onMarkerClick(marker) }
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
                    val offset = tileFractionToOffset(
                        tile.tileX.toDouble(), tile.tileY.toDouble(), liveFracX, liveFracY, scale,
                    )
                    with(tileSource) { drawTile(tile, offset, drawSizeInt) }
                }

                for (overlay in overlays) {
                    when (overlay) {
                        is MapOverlay.Polyline -> drawPolyline(overlay, tilesZoom, liveFracX, liveFracY, scale)
                        is MapOverlay.Marker -> drawMarker(overlay, tilesZoom, liveFracX, liveFracY, scale)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPolyline(
    polyline: MapOverlay.Polyline,
    tilesZoom: Int,
    liveFracX: Double,
    liveFracY: Double,
    scale: Float,
) {
    if (polyline.points.isEmpty()) return
    val path = Path()
    polyline.points.forEachIndexed { index, point ->
        val offset = lonLatToOffset(point.lat, point.lon, tilesZoom, liveFracX, liveFracY, scale)
        if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
    }
    drawPath(
        path = path,
        color = polyline.color,
        style = Stroke(width = polyline.widthDp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawMarker(
    marker: MapOverlay.Marker,
    tilesZoom: Int,
    liveFracX: Double,
    liveFracY: Double,
    scale: Float,
) {
    val offset = lonLatToOffset(marker.point.lat, marker.point.lon, tilesZoom, liveFracX, liveFracY, scale)
    drawCircle(color = marker.color, radius = marker.radiusDp.toPx(), center = offset)
}
