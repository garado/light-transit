package dev.garado.transit.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
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
import dev.garado.transit.search.LocationSearchScreen

class NearbyStopsScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, NearbyStopsViewModel>(sealedActivity) {

    override val viewModelClass = NearbyStopsViewModel::class.java
    override fun createViewModel() = NearbyStopsViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val stops by viewModel.stops.collectAsState()
        val viewMode by viewModel.viewMode.collectAsState()
        val searchedCenter by viewModel.searchedCenter.collectAsState()
        val hasSearched by viewModel.hasSearched.collectAsState()
        val isSearching by viewModel.isSearching.collectAsState()

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
                )

                if (isSearching) {
                    SearchingOverlay(modifier = Modifier.weight(1f))
                } else {
                    when (viewMode) {
                        NearbyStopsViewMode.MAP -> TransitMapView(
                            isDarkTheme = LightThemeController.isDarkTheme,
                            tileSource = tileSource,
                            initialCenter = searchedCenter ?: DEMO_LOCATION,
                            overlays = markers,
                            fitBounds = fitBounds,
                            onMarkerClick = { marker ->
                                stops.find { it.globalStopId == marker.id }?.let(::onStopSelected)
                            },
                            onCenterChanged = viewModel::onMapCenterChanged,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                        NearbyStopsViewMode.LIST -> NearbyStopsList(
                            stops = stops,
                            modifier = Modifier.weight(1f),
                            onStopClick = ::onStopSelected,
                        )
                    }

                    NearbyStopsBottomBar(
                        viewMode = viewMode,
                        showToggle = hasSearched,
                        onSearchIconClick = {
                            navigateTo(::LocationSearchScreen) { result ->
                                viewModel.jumpTo(LatLon(lat = result.lat, lon = result.lon))
                            }
                        },
                        onSearchClick = { viewModel.search() },
                        onToggleClick = { viewModel.toggleViewMode() },
                    )
                }
            }
        }
    }
}

@Composable
private fun NearbyStopsBottomBar(
    viewMode: NearbyStopsViewMode,
    showToggle: Boolean,
    onSearchIconClick: () -> Unit,
    onSearchClick: () -> Unit,
    onToggleClick: () -> Unit,
) {
    LightBottomBar(
        items = listOf(
            LightBarButton.LightIcon(
                icon = LightIcons.SEARCH,
                contentDescription = "Search location",
                onClick = onSearchIconClick,
            ),
            if (viewMode == NearbyStopsViewMode.MAP) {
                LightBarButton.Text(text = "SEARCH", onClick = onSearchClick)
            } else {
                null
            },
            if (showToggle) {
                LightBarButton.LightIcon(
                    icon = if (viewMode == NearbyStopsViewMode.MAP) LightIcons.LIST else LightIcons.MAP,
                    contentDescription = "Toggle view",
                    onClick = onToggleClick,
                )
            } else {
                null
            },
        ),
    )
}

@Composable
private fun SearchingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        LightText(text = "Searching...", variant = LightTextVariant.Paragraph, color = Color.White)
    }
}

@Composable
private fun NearbyStopsList(stops: List<TripStop>, modifier: Modifier = Modifier, onStopClick: (TripStop) -> Unit) {
    if (stops.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LightText(
                text = "No stops found nearby",
                variant = LightTextVariant.Paragraph,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        return
    }
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
