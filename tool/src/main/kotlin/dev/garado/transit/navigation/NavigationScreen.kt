package dev.garado.transit.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
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
import dev.garado.transit.LegIcon
import dev.garado.transit.StatusBar
import dev.garado.transit.api.transit.models.TripLeg
import dev.garado.transit.api.transit.models.TripPlan
import dev.garado.transit.formatClockTime
import dev.garado.transit.formatDuration
import dev.garado.transit.map.RasterTileSource
import dev.garado.transit.map.TileCacheDatabase
import dev.garado.transit.map.TransitMapView
import dev.garado.transit.route.centroid
import dev.garado.transit.route.toOverlays

private enum class NavigationViewMode { DIRECTIONS, MAP }

class NavigationScreen(
    sealedActivity: SealedLightActivity,
    private val plan: TripPlan,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var viewMode by remember { mutableStateOf(NavigationViewMode.DIRECTIONS) }

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

        LightTheme(colors = themeColors) {
            val walkLegColor = LightThemeTokens.colors.content
            val overlays = remember(plan, walkLegColor) { plan.toOverlays(walkLegColor) }
            val initialCenter = remember(overlays) { overlays.centroid() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                StatusBar(onCancel = { goBack() })

                // Main content area
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (viewMode) {
                        NavigationViewMode.DIRECTIONS -> DirectionsList(plan)
                        NavigationViewMode.MAP -> TransitMapView(
                            isDarkTheme = LightThemeController.isDarkTheme,
                            tileSource = tileSource,
                            initialCenter = initialCenter,
                            overlays = overlays,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                NavigationBottomBar(
                    viewMode = viewMode,
                    eta = formatClockTime(plan.endTime),
                    onRecenter = { /* TODO: recenter to live gps location */ },
                    onSwitchView = {
                        viewMode = when (viewMode) {
                            NavigationViewMode.DIRECTIONS -> NavigationViewMode.MAP
                            NavigationViewMode.MAP -> NavigationViewMode.DIRECTIONS
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DirectionsList(plan: TripPlan) {
    LightScrollView(modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 8.dp)) {
        DirectionsSummaryHeader(plan)
        plan.legs.forEach { leg -> DirectionsRow(leg) }
    }
}

@Composable
private fun DirectionsSummaryHeader(plan: TripPlan) {
    val minutesUntilDeparture = remember(plan) {
        ((plan.startTime - System.currentTimeMillis() / 1000) / 60).coerceAtLeast(0)
    }
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)) {
        LightText(
            text = "Leave at ${formatClockTime(plan.startTime)} (in $minutesUntilDeparture minutes)",
            variant = LightTextVariant.Copy,
        )
        LightText(
            text = "Arrive at ${formatClockTime(plan.endTime)}",
            variant = LightTextVariant.Detail,
            lighten = true,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun DirectionsRow(leg: TripLeg) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        LegIcon(leg = leg.shortened(), modifier = Modifier.padding(top = 2.dp), minWidth = 32.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            when (leg) {
                is TripLeg.Walk -> LightText(
                    text = formatDuration(leg.duration),
                    variant = LightTextVariant.Detail,
                    lighten = true,
                )
                is TripLeg.Transit -> TransitLegDetail(leg)
            }
        }
    }
}

@Composable
private fun NavigationBottomBar(
    viewMode: NavigationViewMode,
    eta: String,
    onRecenter: () -> Unit,
    onSwitchView: () -> Unit,
) {
    val items = when (viewMode) {
        NavigationViewMode.DIRECTIONS -> listOf(
            null,
            LightBarButton.Text(text = eta, onClick = null),
            LightBarButton.LightIcon(icon = LightIcons.MAP, onClick = onSwitchView),
        )
        NavigationViewMode.MAP -> listOf(
            LightBarButton.LightIcon(icon = LightIcons.CROSSHAIR, onClick = onRecenter),
            LightBarButton.Text(text = eta, onClick = null),
            LightBarButton.LightIcon(icon = LightIcons.LIST, onClick = onSwitchView),
        )
    }
    LightBottomBar(items = items)
}
