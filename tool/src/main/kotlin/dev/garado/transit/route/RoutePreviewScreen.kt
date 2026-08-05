package dev.garado.transit.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.api.transit.models.TripPlan
import dev.garado.transit.formatDuration
import dev.garado.transit.formatTimeRange
import dev.garado.transit.map.RasterTileSource
import dev.garado.transit.map.TileCacheDatabase
import dev.garado.transit.map.TransitMapView
// import dev.garado.transit.navigation.NavigationScreen

class RoutePreviewScreen(
    sealedActivity: SealedLightActivity,
    private val tripPlans: List<TripPlan>,
    private val initialIndex: Int,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var currentIndex by remember { mutableIntStateOf(initialIndex) }

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

        val plan = tripPlans[currentIndex]

        LightTheme(colors = themeColors) {
            val walkLegColor = LightThemeTokens.colors.content
            val overlays = remember(plan, walkLegColor) { plan.toOverlays(walkLegColor) }
            val initialCenter = remember(overlays) { overlays.centroid() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.TwoLineDetail(
                        line1 = formatDuration(plan.duration),
                        line2 = formatTimeRange(plan.startTime, plan.endTime),
                    ),
                )

                TransitMapView(
                    isDarkTheme = LightThemeController.isDarkTheme,
                    tileSource = tileSource,
                    initialCenter = initialCenter,
                    overlays = overlays,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )

                SelectBar(
                    showCycleButtons = tripPlans.size > 1,
                    onPrevious = { currentIndex = (currentIndex - 1 + tripPlans.size) % tripPlans.size },
                    onNext = { currentIndex = (currentIndex + 1) % tripPlans.size },
                    // onSelect = { navigateTo({ activity -> NavigationScreen(activity, plan) }) },
                    onSelect = {},
                )
            }
        }
    }
}

@Composable
private fun SelectBar(
    showCycleButtons: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(LightThemeTokens.colors.background)
            .padding(start = 32.dp, end = 32.dp, top = 2.dp, bottom = 16.dp),
    ) {
        if (showCycleButtons) {
            LightIcon(
                icon = LightIcons.ARROW_RIGHT,
                modifier = Modifier.graphicsLayer(rotationZ = 180f).lightClickable(onClick = onPrevious),
            )
        }

        LightText(
            text = "SELECT",
            variant = LightTextVariant.Button,
            align = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(
                horizontal = 32.dp, vertical = 8.dp).lightClickable(onClick = onSelect),
        )

        if (showCycleButtons) {
            LightIcon(icon = LightIcons.ARROW_RIGHT, modifier = Modifier.lightClickable(onClick = onNext))
        }
    }
}
