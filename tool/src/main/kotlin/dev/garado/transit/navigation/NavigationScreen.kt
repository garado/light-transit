package dev.garado.transit.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.LegModeIcon
import dev.garado.transit.MinuteTimer
import dev.garado.transit.StatusBar
import dev.garado.transit.api.models.TripLeg
import dev.garado.transit.api.models.TripPlan
import dev.garado.transit.formatClockTime
import dev.garado.transit.formatDuration
import dev.garado.transit.map.RasterTileSource
import dev.garado.transit.map.TileCacheDatabase
import dev.garado.transit.map.TransitMapView
import dev.garado.transit.route.centroid
import dev.garado.transit.route.toOverlays
import dev.garado.transit.search.LocationResult

private enum class NavigationViewMode { DIRECTIONS, MAP }

private val LEG_ICON_MIN_WIDTH = 32.dp

class NavigationScreen(
    sealedActivity: SealedLightActivity,
    private val plan: TripPlan,
    private val toLocation: LocationResult,
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
                        NavigationViewMode.DIRECTIONS -> DirectionsList(plan, toLocation)
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
                    stepSummary = plan.stepSummary(index = 0, toLocation = toLocation),
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
private fun DirectionsList(plan: TripPlan, toLocation: LocationResult) {
    LightScrollView(modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 8.dp)) {
        DirectionsSummaryHeader(plan)
        plan.legs.forEach { leg -> DirectionsRow(leg) }
        DestinationRow(toLocation, eta = formatClockTime(plan.endTime))
    }
}

@Composable
private fun DirectionsSummaryHeader(plan: TripPlan) {
    val currentTimeSeconds by MinuteTimer.currentTimeSeconds.collectAsState()
    val minutesUntilDeparture = ((plan.startTime - currentTimeSeconds) / 60).coerceAtLeast(0)
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
        LegModeIcon(leg = leg, modifier = Modifier.padding(top = 2.dp), minWidth = LEG_ICON_MIN_WIDTH)
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
private fun DestinationRow(toLocation: LocationResult, eta: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Box(modifier = Modifier.widthIn(min = LEG_ICON_MIN_WIDTH), contentAlignment = Alignment.Center) {
            LightIcon(icon = LightIcons.DIRECTIONS_ARRIVAL, size = 1.25f)
        }
        Column(modifier = Modifier.weight(1f)) {
            LightText(text = toLocation.title, variant = LightTextVariant.Paragraph)
            if (toLocation.address.isNotBlank()) {
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    LightText(
                        text = toLocation.address,
                        variant = LightTextVariant.Detail,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    LightText(text = eta, variant = LightTextVariant.Detail, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun NavigationBottomBar(
    viewMode: NavigationViewMode,
    stepSummary: String,
    onRecenter: () -> Unit,
    onSwitchView: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .height(4f.gridUnitsAsDp())
            .padding(horizontal = 2f.gridUnitsAsDp()),
    ) {
        when (viewMode) {
            NavigationViewMode.DIRECTIONS -> {
                LightIcon(icon = LightIcons.SPACER)
                LightText(text = stepSummary, variant = LightTextVariant.Detail)
                LightIcon(icon = LightIcons.MAP, modifier = Modifier.lightClickable(onClick = onSwitchView))
            }
            NavigationViewMode.MAP -> {
                LightIcon(icon = LightIcons.CROSSHAIR, modifier = Modifier.lightClickable(onClick = onRecenter))
                LightText(text = stepSummary, variant = LightTextVariant.Detail)
                LightIcon(icon = LightIcons.LIST, modifier = Modifier.lightClickable(onClick = onSwitchView))
            }
        }
    }
}

private fun TripPlan.stepSummary(index: Int, toLocation: LocationResult): String =
    when (val leg = legs.getOrNull(index) ?: return "") {
        is TripLeg.Walk -> "Walk to ${legs.getOrNull(index + 1)?.startLocationName(toLocation) ?: toLocation.title}"
        is TripLeg.Transit -> "Board ${leg.routeName}"
    }

private fun TripLeg.startLocationName(toLocation: LocationResult): String = when (this) {
    is TripLeg.Transit -> stops.firstOrNull()?.name ?: toLocation.title
    is TripLeg.Walk -> toLocation.title
}
