package dev.garado.transit.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.garado.transit.StatusBar
import dev.garado.transit.api.models.TripRoute
import dev.garado.transit.location.AutoJumpToUserLocation
import dev.garado.transit.parseHexColor
import dev.garado.transit.search.LocationSearchScreen

class NearbyRoutesScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, NearbyRoutesViewModel>(sealedActivity) {

    override val viewModelClass = NearbyRoutesViewModel::class.java
    override fun createViewModel() = NearbyRoutesViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val routes by viewModel.routes.collectAsState()
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

        fun onRouteSelected(route: TripRoute) {
            navigateTo({ activity -> RouteMapScreen(activity, route) })
        }

        AutoJumpToUserLocation(lightContext, onLocationFound = viewModel::jumpTo)

        val markerColor = LightThemeTokens.colors.content
        val searchMarkers = remember(searchedCenter, markerColor) {
            searchedCenter?.let { listOf(MapOverlay.Marker(point = it, color = markerColor)) }.orEmpty()
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
                    center = LightTopBarCenter.Text("Nearby Routes"),
                )

                if (isSearching) {
                    CenteredMessage("Searching...", modifier = Modifier.weight(1f))
                } else {
                    when (viewMode) {
                        NearbyRoutesViewMode.MAP -> TransitMapView(
                            isDarkTheme = LightThemeController.isDarkTheme,
                            tileSource = tileSource,
                            initialCenter = searchedCenter ?: DEMO_LOCATION,
                            overlays = searchMarkers,
                            onCenterChanged = viewModel::onMapCenterChanged,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                        NearbyRoutesViewMode.LIST -> when {
                            !hasSearched -> CenteredMessage("Searching...", modifier = Modifier.weight(1f))
                            routes.isEmpty() -> CenteredMessage("No nearby routes found", modifier = Modifier.weight(1f))
                            else -> LightScrollView(modifier = Modifier.weight(1f)) {
                                routes.forEach { route -> RouteRow(route, onClick = { onRouteSelected(route) }) }
                            }
                        }
                    }

                    NearbyRoutesBottomBar(
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
private fun NearbyRoutesBottomBar(
    viewMode: NearbyRoutesViewMode,
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
                sizeUnits = 1.5f,
            ),
            if (viewMode == NearbyRoutesViewMode.MAP) {
                LightBarButton.Text(text = "SEARCH", onClick = onSearchClick)
            } else {
                null
            },
            if (showToggle) {
                LightBarButton.LightIcon(
                    icon = if (viewMode == NearbyRoutesViewMode.MAP) LightIcons.LIST else LightIcons.MAP,
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
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        LightText(text = text, variant = LightTextVariant.Paragraph)
    }
}

@Composable
private fun RouteRow(route: TripRoute, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 12.dp, start = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = parseHexColor(route.color, fallback = LightThemeTokens.colors.content),
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            LightText(
                text = route.name,
                variant = LightTextVariant.Detail,
                color = parseHexColor(route.textColor, fallback = LightThemeTokens.colors.background),
            )
        }
        if (route.longName != null) {
            LightText(
                text = route.longName,
                variant = LightTextVariant.Paragraph,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
