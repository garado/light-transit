package dev.garado.transit.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import dev.garado.transit.api.models.StopDeparture
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
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, sizeUnits = 1.5f, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(stopName),
                )

                if (departures.isEmpty()) {
                    LightText(
                        text = "No upcoming departures",
                        variant = LightTextVariant.Detail,
                        lighten = true,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                } else {
                    LightScrollView(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        departures.forEach { departure -> DepartureRow(departure) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartureRow(departure: StopDeparture) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .background(
                        color = parseHexColor(departure.routeColor, fallback = LightThemeTokens.colors.content),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                LightText(
                    text = departure.routeName,
                    variant = LightTextVariant.Detail,
                    color = parseHexColor(departure.routeTextColor, fallback = LightThemeTokens.colors.background),
                )
            }
            if (departure.headsign != null) {
                LightText(
                    text = departure.headsign,
                    variant = LightTextVariant.Paragraph,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        LightText(
            text = formatClockTime(departure.departureTime),
            variant = LightTextVariant.Paragraph,
            maxLines = 1,
        )
    }
}
