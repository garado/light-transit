package dev.garado.transit.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
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
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, sizeUnits = 1.5f, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Map"),
                )

                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)) {
                    MapMenuRow(label = "Search on map", onClick = {})
                    MapMenuRow(label = "Bikeshare", onClick = {})
                    MapMenuRow(label = "Nearby stops", onClick = { navigateTo(::NearbyStopsScreen) })
                    MapMenuRow(label = "Saved routes", onClick = {})
                }
            }
        }
    }
}

@Composable
private fun MapMenuRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(bottom = 24.dp),
    ) {
        LightText(text = label, variant = LightTextVariant.Copy, modifier = Modifier.weight(1f))
        LightIcon(icon = LightIcons.ARROW_RIGHT, size = 1.5f)
    }
}
