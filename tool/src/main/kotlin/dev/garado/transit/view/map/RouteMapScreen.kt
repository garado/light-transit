package dev.garado.transit.view.map

import dev.garado.transit.view.route.boundingBox
import dev.garado.transit.models.LatLon
import dev.garado.transit.models.LatLonBounds
import dev.garado.transit.map.MapOverlay
import dev.garado.transit.map.RasterTileSource
import dev.garado.transit.data.database.maptiles.TileCacheDatabase
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import dev.garado.transit.view.home.StatusBar
import dev.garado.transit.models.TripRoute
import dev.garado.transit.models.TripStop
import dev.garado.transit.interfaces.routestops.GtfsLocalRouteStopsProvider
import dev.garado.transit.interfaces.stopdepartures.GtfsLocalStopDeparturesProvider
import dev.garado.transit.util.parseHexColor
import dev.garado.transit.util.decodePolyline
import kotlinx.coroutines.launch

class RouteMapScreen(
    sealedActivity: SealedLightActivity,
    private val route: TripRoute,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val stopsProvider = remember { GtfsLocalRouteStopsProvider(lightContext) }
        val departuresProvider = remember { GtfsLocalStopDeparturesProvider(lightContext) }
        val coroutineScope = rememberCoroutineScope()
        var stops by remember { mutableStateOf<List<TripStop>>(emptyList()) }
        var shape by remember { mutableStateOf(route.shape) }
        LaunchedEffect(route.globalRouteId) {
            stops = stopsProvider.stopsForRoute(route.globalRouteId)
            if (shape == null) shape = stopsProvider.shapeForRoute(route.globalRouteId)
        }

        fun onStopSelected(stop: TripStop) {
            // replace screen instead of pushing new screen to prevent endless backstack
            coroutineScope.launch {
                val departures = departuresProvider.departures(stop.groupedStopIds)
                    .values
                    .flatten()
                    .sortedBy { it.departureTime }
                goBack()
                navigateTo({ activity -> StopDeparturesScreen(activity, stop.name, departures) })
            }
        }

        val tileCacheDatabase = remember {
            lightContext.buildDatabase(TileCacheDatabase::class.java, "tile_cache.db")
        }
        val tileSource = remember(tileCacheDatabase) { RasterTileSource(tileCacheDatabase) }
        DisposableEffect(tileSource) {
            onDispose {
                tileSource.close()
                tileCacheDatabase.close()
            }
        }

        val markerColor = LightThemeTokens.colors.content
        val routeColor = parseHexColor(route.color, fallback = markerColor)
        val polyline = remember(shape, routeColor) {
            shape?.let { MapOverlay.Polyline(points = decodePolyline(it), color = routeColor) }
        }
        val markers = remember(stops, markerColor) {
            stops.map { stop ->
                MapOverlay.Marker(point = LatLon(lat = stop.lat, lon = stop.lon), color = markerColor, id = stop.globalStopId)
            }
        }
        val overlays = remember(polyline, markers) { listOfNotNull(polyline) + markers }
        val fitBounds = remember(polyline, stops) {
            polyline?.points?.boundingBox() ?: stops.map { LatLon(lat = it.lat, lon = it.lon) }.boundingBox()
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                StatusBar()
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, sizeUnits = 1.5f, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(route.longName ?: route.name),
                )

                TransitMapView(
                    isDarkTheme = LightThemeController.isDarkTheme,
                    tileSource = tileSource,
                    initialCenter = fitBounds?.center ?: DEMO_LOCATION,
                    overlays = overlays,
                    fitBounds = fitBounds,
                    onMarkerClick = { marker -> stops.find { it.globalStopId == marker.id }?.let(::onStopSelected) },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
            }
        }
    }
}

private fun List<LatLon>.boundingBox(): LatLonBounds? {
    if (isEmpty()) return null
    return LatLonBounds(
        minLat = minOf { it.lat },
        maxLat = maxOf { it.lat },
        minLon = minOf { it.lon },
        maxLon = maxOf { it.lon },
    )
}
