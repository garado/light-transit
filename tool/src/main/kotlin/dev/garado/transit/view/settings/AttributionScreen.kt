package dev.garado.transit.view.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import dev.garado.transit.view.home.StatusBar

private data class Attribution(val name: String, val description: String)

private val ATTRIBUTIONS = listOf(
    Attribution(
        name = "OpenStreetMap",
        description = "Map and geocoding data © OpenStreetMap contributors, openstreetmap.org/copyright",
    ),
    Attribution(
        name = "Carto",
        description = "Map tiles by Carto, carto.com/attributions",
    ),
    Attribution(
        name = "Nominatim",
        description = "Location search powered by Nominatim, nominatim.org",
    ),
    Attribution(
        name = "Transitous",
        description = "Trip planning and GTFS feed catalog by Transitous, transitous.org",
    ),
    Attribution(
        name = "TransitAPI",
        description = "Trip planning and live transit data by Transit, transitapp.com",
    ),
    Attribution(
        name = "GTFS feed providers",
        description = "Schedule data provided by the individual transit agencies whose GTFS feeds are downloaded",
    ),
)

class AttributionScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

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
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Attribution"),
                )

                LightScrollView(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    ATTRIBUTIONS.forEach { attribution ->
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            LightText(text = attribution.name, variant = LightTextVariant.Copy)
                            LightText(
                                text = attribution.description,
                                variant = LightTextVariant.Detail,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
