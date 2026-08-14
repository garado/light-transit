/** Developer settings (accessible by tapping 'Settings' header 3 times) */

package dev.garado.transit.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.garado.transit.StatusBar
import dev.garado.transit.api.transit.MockTransitApiSettings
import dev.garado.transit.gtfs.sources.GtfsClearCacheConfirmScreen
import dev.garado.transit.gtfs.sources.clearGtfsDownloadCache
import dev.garado.transit.location.UserLocationSettings
import kotlinx.coroutines.launch

class DeveloperSettingsScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        val mockSettings = remember { MockTransitApiSettings(lightContext.dataStore) }
        val useMockedResponses by mockSettings.useMockedResponses.collectAsState(initial = false)
        val simulateApiFailure by mockSettings.simulateApiFailure.collectAsState(initial = false)

        val locationSettings = remember { UserLocationSettings(lightContext.dataStore) }
        val liveLocationEnabled by locationSettings.liveLocationEnabled.collectAsState(initial = false)

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                StatusBar()
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Developer Settings"),
                )

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DevToggleRow(
                        label = "Mock API responses",
                        enabled = useMockedResponses,
                        onClick = {
                            coroutineScope.launch { mockSettings.setUseMockedResponses(!useMockedResponses) }
                        },
                    )
                    DevToggleRow(
                        label = "Simulate API failures",
                        enabled = simulateApiFailure,
                        onClick = {
                            coroutineScope.launch { mockSettings.setSimulateApiFailure(!simulateApiFailure) }
                        },
                    )
                    DevToggleRow(
                        label = "Enable live location ",
                        enabled = liveLocationEnabled,
                        onClick = {
                            coroutineScope.launch { locationSettings.setLiveLocationEnabled(!liveLocationEnabled) }
                        },
                    )
                    DevActionRow(
                        label = "Clear GTFS cache and downloads",
                        onClick = {
                            navigateTo(::GtfsClearCacheConfirmScreen) { confirmed ->
                                if (confirmed) {
                                    coroutineScope.launch { clearGtfsDownloadCache(lightContext) }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DevToggleRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 12.dp),
    ) {
        LightIcon(
            icon = if (enabled) LightIcons.TOGGLE_STATE_ON else LightIcons.TOGGLE_STATE_OFF,
            modifier = Modifier.padding(end = 16.dp),
        )
        LightText(text = label, variant = LightTextVariant.Copy)
    }
}

@Composable
private fun DevActionRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 12.dp),
    ) {
        LightText(text = label, variant = LightTextVariant.Copy)
    }
}
