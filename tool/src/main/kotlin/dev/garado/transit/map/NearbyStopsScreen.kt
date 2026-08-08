package dev.garado.transit.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.api.models.TripStop

class NearbyStopsScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, NearbyStopsViewModel>(sealedActivity) {

    override val viewModelClass = NearbyStopsViewModel::class.java
    override fun createViewModel() = NearbyStopsViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val stops by viewModel.stops.collectAsState()
        val viewMode by viewModel.viewMode.collectAsState()

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
        val markers = remember(stops, markerColor) {
            stops.map { stop ->
                MapOverlay.Marker(
                    point = LatLon(lat = stop.lat, lon = stop.lon),
                    color = markerColor,
                    id = stop.globalStopId,
                )
            }
        }
        val fitBounds = remember(stops) { stops.map { LatLon(lat = it.lat, lon = it.lon) }.boundingBox() }

        fun onStopSelected(stop: TripStop) {
            navigateTo({ activity -> StopDeparturesScreen(activity, stop.name, viewModel.departuresFor(stop)) })
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, sizeUnits = 1.5f, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Nearby Stops"),
                    rightButton = LightBarButton.LightIcon(
                        icon = if (viewMode == NearbyStopsViewMode.MAP) LightIcons.LIST else LightIcons.MAP,
                        sizeUnits = 1.5f,
                        onClick = { viewModel.toggleViewMode() },
                    ),
                )

                when (viewMode) {
                    NearbyStopsViewMode.MAP -> TransitMapView(
                        isDarkTheme = LightThemeController.isDarkTheme,
                        tileSource = tileSource,
                        initialCenter = fitBounds?.center ?: DEMO_LOCATION,
                        overlays = markers,
                        fitBounds = fitBounds,
                        onMarkerClick = { marker ->
                            stops.find { it.globalStopId == marker.id }?.let(::onStopSelected)
                        },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                    NearbyStopsViewMode.LIST -> NearbyStopsList(
                        stops = stops,
                        modifier = Modifier.weight(1f),
                        onStopClick = ::onStopSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun NearbyStopsList(stops: List<TripStop>, modifier: Modifier = Modifier, onStopClick: (TripStop) -> Unit) {
    LightScrollView(modifier = modifier.padding(horizontal = 8.dp)) {
        stops.forEach { stop ->
            LightText(
                text = stop.name,
                variant = LightTextVariant.Copy,
                modifier = Modifier
                    .lightClickable(onClick = { onStopClick(stop) })
                    .padding(vertical = 12.dp, horizontal = 16.dp),
            )
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
