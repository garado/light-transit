package dev.garado.transit.view.search

import dev.garado.transit.search.DepartureSelection
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
import dev.garado.transit.view.home.StatusBar
import dev.garado.transit.util.formatClockTime

private val TAB_GAP = 32.dp

class DepartureTimeScreen(
    sealedActivity: SealedLightActivity,
    private val initialSelection: DepartureSelection,
    private val initialTime: TimeSelection,
) : LightScreen<DepartureSelection, DepartureTimeViewModel>(sealedActivity) {

    override val viewModelClass = DepartureTimeViewModel::class.java
    override fun createViewModel() = DepartureTimeViewModel(initialSelection, initialTime)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val mode by viewModel.mode.collectAsState()
        val time = viewModel.initialTime

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
                        onClick = { goBack(mode.toSelection(time)) },
                    ),
                    center = LightTopBarCenter.Text("Departure Settings"),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
                ) {
                    DepartureModeTabs(
                        selected = mode,
                        onSelect = { selected ->
                            if (selected == DepartureMode.LEAVE_NOW) {
                                goBack(DepartureSelection.Now)
                            } else {
                                viewModel.selectMode(selected)
                            }
                        },
                    )

                    if (mode != DepartureMode.LEAVE_NOW) {
                        LightText(
                            text = formatClockTime(time.hour24, time.minute),
                            variant = LightTextVariant.Heading,
                            modifier = Modifier
                                .padding(top = 48.dp)
                                .lightClickable(onClick = {
                                    navigateTo(
                                        { activity -> TimePickerScreen(activity, time.hour24, time.minute) },
                                    ) { result -> goBack(mode.toSelection(result)) }
                                }),
                        )
                    }
                }
            }
        }
    }
}

private fun DepartureMode.toSelection(time: TimeSelection): DepartureSelection = when (this) {
    DepartureMode.LEAVE_AT -> DepartureSelection.LeaveAt(time.hour24, time.minute)
    DepartureMode.ARRIVE_BY -> DepartureSelection.ArriveBy(time.hour24, time.minute)
    DepartureMode.LEAVE_NOW -> DepartureSelection.Now
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
