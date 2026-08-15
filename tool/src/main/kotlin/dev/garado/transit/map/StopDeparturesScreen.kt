package dev.garado.transit.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import dev.garado.transit.StatusBar
import dev.garado.transit.api.models.StopDeparture
import dev.garado.transit.api.models.TripRoute
import dev.garado.transit.formatClockTime
import dev.garado.transit.parseHexColor

class StopDeparturesScreen(
    sealedActivity: SealedLightActivity,
    private val stopName: String,
    private val departures: List<StopDeparture>,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                StatusBar()
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, sizeUnits = 1.5f, onClick = { goBack() }),
                    center = LightTopBarCenter.TwoLineDetail(line1 = stopName, line2 = "Upcoming Departures"),
                )

                if (departures.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = "No upcoming departures found",
                            variant = LightTextVariant.Paragraph,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
                        LightScrollView(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                            departures
                                .groupBy { it.routeName to it.headsign }
                                .values
                                .sortedBy { section -> section.minOf { it.departureTime } }
                                .forEach { routeDepartures ->
                                    RouteDeparturesSection(
                                        routeDepartures,
                                        onRouteClick = { route ->
                                            // replace screen instead of pushing new screen
                                            // to prevent endless backstack
                                            goBack()
                                            navigateTo({ activity -> RouteMapScreen(activity, route) })
                                        },
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}

/** Show route badge/name + its departure times*/
@Composable
private fun RouteDeparturesSection(departures: List<StopDeparture>, onRouteClick: (TripRoute) -> Unit) {
    val first = departures.first()
    val route = remember(first.globalRouteId, first.routeName, first.routeColor, first.routeTextColor) {
        TripRoute(
            globalRouteId = first.globalRouteId,
            name = first.routeName,
            longName = null,
            color = first.routeColor,
            textColor = first.routeTextColor,
        )
    }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, start = 16.dp)) {
        // route badge and name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.lightClickable(onClick = { onRouteClick(route) }),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = parseHexColor(first.routeColor, fallback = LightThemeTokens.colors.content),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                LightText(
                    text = first.routeName,
                    variant = LightTextVariant.Detail,
                    color = parseHexColor(first.routeTextColor, fallback = LightThemeTokens.colors.background),
                )
            }
            if (first.headsign != null) {
                LightText(
                    text = first.headsign,
                    variant = LightTextVariant.Paragraph,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        DepartureTimesGrid(departures, modifier = Modifier.padding(top = 8.dp))
    }
}

private const val GRID_COLUMNS = 4
private const val COLLAPSED_ROWS = 2
private const val COLLAPSED_COUNT = GRID_COLUMNS * COLLAPSED_ROWS

/** Departure times in a 4-column grid. Past 2 rows, collapses to "+N more" until tapped to expand. */
@Composable
private fun DepartureTimesGrid(departures: List<StopDeparture>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val isTruncatable = departures.size > COLLAPSED_COUNT
    val visible = if (expanded || !isTruncatable) departures else departures.take(COLLAPSED_COUNT)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .let { if (isTruncatable) it.lightClickable(onClick = { expanded = !expanded }) else it },
    ) {
        visible.chunked(GRID_COLUMNS).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                repeat(GRID_COLUMNS) { column ->
                    val departure = row.getOrNull(column)
                    if (departure != null) {
                        LightText(
                            text = formatClockTime(departure.departureTime),
                            variant = LightTextVariant.Paragraph,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        if (isTruncatable && !expanded) {
            LightText(
                text = "+ ${departures.size - COLLAPSED_COUNT} more",
                variant = LightTextVariant.Detail,
                lighten = true,
            )
        }
    }
}
