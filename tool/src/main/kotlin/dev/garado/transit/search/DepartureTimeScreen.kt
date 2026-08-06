package dev.garado.transit.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
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

private val TAB_GAP = 32.dp

class DepartureTimeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, DepartureTimeViewModel>(sealedActivity) {

    override val viewModelClass = DepartureTimeViewModel::class.java
    override fun createViewModel() = DepartureTimeViewModel()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val mode by viewModel.mode.collectAsState()
        val selectedTime by viewModel.selectedTime.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Departure Settings"),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
                ) {
                    DepartureModeTabs(selected = mode, onSelect = viewModel::selectMode)

                    if (mode != DepartureMode.LEAVE_NOW) {
                        LightText(
                            text = selectedTime?.let { formatTime(it.hour24, it.minute) } ?: "<Time>",
                            variant = LightTextVariant.Heading,
                            modifier = Modifier
                                .padding(top = 48.dp)
                                .lightClickable(onClick = {
                                    navigateTo(
                                        { activity ->
                                            TimePickerScreen(activity, selectedTime?.hour24, selectedTime?.minute)
                                        },
                                    ) { result -> viewModel.setSelectedTime(result) }
                                }),
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(hour24: Int, minute: Int): String {
    val isPm = hour24 >= 12
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "$hour12:${minute.toString().padStart(2, '0')} ${if (isPm) "PM" else "AM"}"
}

@Composable
private fun DepartureModeTabs(selected: DepartureMode, onSelect: (DepartureMode) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TAB_GAP, alignment = Alignment.CenterVertically),
        modifier = Modifier.fillMaxWidth(),
    ) {
        DepartureMode.entries.forEach { mode ->
            LightText(
                text = mode.label,
                variant = LightTextVariant.Copy,
                underline = mode == selected,
                modifier = Modifier.lightClickable(onClick = { onSelect(mode) }),
            )
        }
    }
}
