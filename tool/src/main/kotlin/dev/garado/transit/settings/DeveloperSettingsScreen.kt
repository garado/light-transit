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
import dev.garado.transit.api.transit.MockTransitApiSettings
import kotlinx.coroutines.launch

class DeveloperSettingsScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        val mockSettings = remember { MockTransitApiSettings(lightContext.dataStore) }
        val useMockedResponses by mockSettings.useMockedResponses.collectAsState(initial = false)

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Developer Settings"),
                )

                Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
                    MockResponsesToggleRow(
                        enabled = useMockedResponses,
                        onClick = {
                            coroutineScope.launch { mockSettings.setUseMockedResponses(!useMockedResponses) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MockResponsesToggleRow(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick),
    ) {
        LightIcon(
            icon = if (enabled) LightIcons.TOGGLE_STATE_ON else LightIcons.TOGGLE_STATE_OFF,
            modifier = Modifier.padding(end = 16.dp),
        )
        LightText(text = "Mock API responses", variant = LightTextVariant.Copy)
    }
}
