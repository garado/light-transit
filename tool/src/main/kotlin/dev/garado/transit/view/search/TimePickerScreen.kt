package dev.garado.transit.view.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import dev.garado.transit.view.components.TimePicker
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import dev.garado.transit.view.home.StatusBar

data class TimeSelection(val hour24: Int, val minute: Int)

class TimePickerScreen(
    sealedActivity: SealedLightActivity,
    private val initialHour24: Int? = null,
    private val initialMinute: Int? = null,
) : SimpleLightScreen<TimeSelection>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var hour24 by remember { mutableIntStateOf(initialHour24 ?: 0) }
        var minute by remember { mutableIntStateOf(initialMinute ?: 0) }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                StatusBar()
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Select Time"),
                )

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        onTimeChanged = { newHour24, newMinute -> hour24 = newHour24; minute = newMinute },
                        onConfirm = { goBack(TimeSelection(hour24, minute)) },
                    )
                }
            }
        }
    }
}
