package dev.garado.transit.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
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
import dev.garado.transit.api.transit.models.TripPlan
import dev.garado.transit.formatDurationLines
import dev.garado.transit.formatTimeRange
import dev.garado.transit.search.DepartureSelection
import dev.garado.transit.search.LocationResult

private val SUMMARY_COLUMN_WIDTH = 56.dp

class RouteSelectScreen(
    sealedActivity: SealedLightActivity,
    private val fromLocation: LocationResult,
    private val toLocation: LocationResult,
    private val departureSelection: DepartureSelection,
) : LightScreen<Unit, RouteSelectViewModel>(sealedActivity) {

    override val viewModelClass = RouteSelectViewModel::class.java
    override fun createViewModel() =
        RouteSelectViewModel(lightContext, fromLocation, toLocation, departureSelection)

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
                        plans.forEachIndexed { index, plan ->
                            RouteOptionRow(
                                plan = plan,
                                onClick = {
                                    navigateTo({ activity -> RoutePreviewScreen(activity, plans, index, toLocation) })
                                },
                            )
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

            TripLegsRow(legs = plan.legs, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
