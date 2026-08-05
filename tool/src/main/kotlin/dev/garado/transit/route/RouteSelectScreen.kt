package dev.garado.transit.route

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcon
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
import dev.garado.transit.api.transit.models.TripLeg
import dev.garado.transit.api.transit.models.TripPlan
import dev.garado.transit.formatDuration
import dev.garado.transit.formatDurationLines
import dev.garado.transit.formatTimeRange
import dev.garado.transit.search.LocationResult

private val SUMMARY_COLUMN_WIDTH = 56.dp

class RouteSelectScreen(
    sealedActivity: SealedLightActivity,
    private val fromLocation: LocationResult,
    private val toLocation: LocationResult,
) : LightScreen<Unit, RouteSelectViewModel>(sealedActivity) {

    override val viewModelClass = RouteSelectViewModel::class.java
    override fun createViewModel() = RouteSelectViewModel(lightContext, fromLocation, toLocation)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val tripPlans by viewModel.tripPlans.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Route Selection"),
                )

                val plans = tripPlans
                when {
                    plans == null -> StatusMessage("Finding routes...")
                    plans.isEmpty() -> StatusMessage("No routes found")
                    else -> LightScrollView(modifier = Modifier.padding(horizontal = 8.dp)) {
                        plans.forEach { plan ->
                            RouteOptionRow(plan = plan, onClick = { /* TODO: preview route */ })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        LightText(text = text, variant = LightTextVariant.Copy)
    }
}

@Composable
private fun RouteOptionRow(plan: TripPlan, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(SUMMARY_COLUMN_WIDTH),
        ) {
            formatDurationLines(plan.duration).forEach { line ->
                LightText(text = line, variant = LightTextVariant.Copy, align = TextAlign.Center)
            }
        }

        Column {
            LightText(
                text = formatTimeRange(plan.startTime, plan.endTime),
                variant = LightTextVariant.Detail,
                lighten = true,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                plan.legs.forEachIndexed { index, leg ->
                    if (index > 0) {
                        LightIcon(icon = LightIcons.ARROW_RIGHT, size = 1f, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    LegChip(leg = leg, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }
    }
}

@Composable
private fun LegChip(leg: TripLeg, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        when (leg) {
            is TripLeg.Walk -> {
                LightIcon(icon = LightIcons.DIRECTIONS_PEDESTRIAN, size = 1.25f)
            }
            is TripLeg.Transit -> {
                Box(
                    modifier = Modifier
                        .background(
                            color = parseHexColor(leg.routeColor, fallback = LightThemeTokens.colors.content),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    LightText(
                        text = leg.routeName,
                        variant = LightTextVariant.Detail,
                        color = parseHexColor(leg.routeTextColor, fallback = LightThemeTokens.colors.background),
                    )
                }
            }
        }
        LightText(
            text = formatDuration(leg.duration),
            variant = LightTextVariant.Detail,
            lighten = true,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

private fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(AndroidColor.parseColor("#$hex"))
    } catch (e: IllegalArgumentException) {
        fallback
    }
}
