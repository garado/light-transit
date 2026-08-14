package dev.garado.transit.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.StatusBar

class MapMenuScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

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
                    leftButton = LightBarButton.LightIcon(
                      icon = LightIcons.BACK,
                      sizeUnits = 1.5f,
                      onClick = { goBack() }
                    ),
                    center = LightTopBarCenter.Text("Map"),
                )

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MapMenuRow(label = "Search on map", onClick = {}, lighten = true)
                    MapMenuRow(label = "Saved routes", onClick = {}, lighten = true)
                    MapMenuRow(label = "Nearby stops", onClick = { navigateTo(::NearbyStopsScreen) })
                    MapMenuRow(label = "Nearby routes", onClick = { navigateTo(::NearbyRoutesScreen) })
                    MapMenuRow(label = "Bikeshare", onClick = {}, lighten = true)
                }
            }
        }
    }
}

@Composable
private fun MapMenuRow(label: String, onClick: () -> Unit, lighten: Boolean = false) {
    LightText(
        text = label,
        variant = LightTextVariant.Copy,
        lighten = lighten,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 12.dp),
    )
}
