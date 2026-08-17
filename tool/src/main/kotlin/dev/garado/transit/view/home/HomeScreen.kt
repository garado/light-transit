package dev.garado.transit.view.home

import dev.garado.transit.view.gtfs.sources.GtfsOverviewScreen
import dev.garado.transit.util.formatClockTime
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import dev.garado.transit.view.map.MapMenuScreen
import dev.garado.transit.view.route.RouteSelectScreen
import dev.garado.transit.models.DepartureSelection
import dev.garado.transit.view.search.DepartureTimeScreen
import dev.garado.transit.view.search.LocationSearchScreen
import dev.garado.transit.view.search.SearchTabContent
import dev.garado.transit.view.settings.ApiSettingsScreen
import dev.garado.transit.view.settings.AttributionScreen
import dev.garado.transit.view.settings.DeveloperSettingsScreen
import dev.garado.transit.view.settings.NameEditor
import dev.garado.transit.view.settings.SavedLocationsScreen
import dev.garado.transit.view.settings.SettingsTabContent

enum class HomeTab { SEARCH, SETTINGS }

private const val DEVELOPER_SETTINGS_TAP_THRESHOLD = 3

@InitialScreen
class HomeScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, HomeScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<HomeScreenViewModel>
        get() = HomeScreenViewModel::class.java

    override fun createViewModel(): HomeScreenViewModel {
        return HomeScreenViewModel(lightContext)
    }

    @Composable
    override fun Content() {
        val selectedTab by viewModel.selectedTab.collectAsState()
        val settingsOptions by viewModel.settingsOptions.collectAsState()
        val displayName by viewModel.settings.displayName.collectAsState()
        val fromLocation by viewModel.search.fromLocation.collectAsState()
        val toLocation by viewModel.search.toLocation.collectAsState()
        val fromDisplay = fromLocation?.let { it.displayName ?: it.title } ?: ""
        val toDisplay = toLocation?.let { it.displayName ?: it.title } ?: ""
        val departureSelection by viewModel.search.departureSelection.collectAsState()
        val departureTime by viewModel.search.departureTime.collectAsState()
        val defaultLocation by viewModel.defaultLocation.collectAsState()
        val isEditingName by viewModel.settings.isEditingName.collectAsState()
        val editSessionId by viewModel.settings.editSessionId.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()
        var settingsTapCount by remember { mutableStateOf(0) }

        LightTheme(colors = themeColors) {
            if (isEditingName) {
                NameEditor(
                    displayName = displayName,
                    editSessionId = editSessionId,
                    onSubmit = { viewModel.settings.submitName(it) },
                    onBack = { viewModel.settings.cancelEditingName() },
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    StatusBar()

                    if (selectedTab == HomeTab.SETTINGS) {
                        LightTopBar(
                            leftButton = LightBarButton.LightIcon(
                                icon = LightIcons.BACK,
                                onClick = { viewModel.selectTab(HomeTab.SEARCH) },
                            ),
                            center = LightTopBarCenter.Text(
                                text = "Settings",
                                onClick = {
                                    settingsTapCount++
                                    if (settingsTapCount >= DEVELOPER_SETTINGS_TAP_THRESHOLD) {
                                        settingsTapCount = 0
                                        navigateTo(::DeveloperSettingsScreen)
                                    }
                                },
                            ),
                        )
                    }

                    Column(modifier = Modifier.weight(1f).tabContentPadding(selectedTab)) {
                        when (selectedTab) {
                            HomeTab.SEARCH -> SearchTabContent(
                                fromLocation = fromDisplay,
                                toLocation = toDisplay,
                                departureFieldLabel = departureSelection.fieldLabel(),
                                departureFieldValue = departureSelection.fieldValue(),
                                onFromClick = {
                                    navigateTo(::LocationSearchScreen) { result ->
                                        viewModel.search.setFromLocation(result)
                                    }
                                },
                                onToClick = {
                                    navigateTo(::LocationSearchScreen) { result ->
                                        viewModel.search.setToLocation(result)
                                    }
                                },
                                onSwapLocations = viewModel.search::swapLocations,
                                onDepartureTimeClick = {
                                    navigateTo(
                                        { activity ->
                                            DepartureTimeScreen(activity, departureSelection, departureTime)
                                        },
                                    ) { result -> viewModel.search.setDepartureSelection(result) }
                                },
                            )
                            HomeTab.SETTINGS -> SettingsTabContent(
                                options = settingsOptions,
                                displayName = displayName,
                                onToggle = viewModel::toggleSetting,
                                defaultLocation = defaultLocation,
                                onDefaultLocationClick = {
                                    navigateTo(::LocationSearchScreen) { result ->
                                        viewModel.setDefaultLocation(result)
                                    }
                                },
                                onSavedLocationsClick = { navigateTo(::SavedLocationsScreen) },
                                onApiSettingsClick = { navigateTo(::ApiSettingsScreen) },
                                onGtfsManagerClick = { navigateTo(::GtfsOverviewScreen) },
                                onAttributionClick = { navigateTo(::AttributionScreen) },
                                onEditName = { viewModel.settings.startEditingName() },
                            )
                        }
                    }

                    if (selectedTab == HomeTab.SEARCH) {
                        HomeBottomBar(
                            showStart = fromDisplay.isNotBlank() && toDisplay.isNotBlank(),
                            onSettingsClick = { viewModel.selectTab(HomeTab.SETTINGS) },
                            onStartClick = {
                                val from = fromLocation
                                val to = toLocation
                                if (from != null && to != null) {
                                    navigateTo(
                                        { activity -> RouteSelectScreen(activity, from, to, departureSelection) },
                                    )
                                }
                            },
                            onMenuClick = { navigateTo(::MapMenuScreen) },
                        )
                    }
                }
            }
        }
    }
}

private fun DepartureSelection.fieldLabel(): String = when (this) {
    is DepartureSelection.Now -> "Leave at"
    is DepartureSelection.LeaveAt -> "Leave at"
    is DepartureSelection.ArriveBy -> "Arrive by"
}

private fun DepartureSelection.fieldValue(): String = when (this) {
    is DepartureSelection.Now -> "Now"
    is DepartureSelection.LeaveAt -> formatClockTime(hour24, minute)
    is DepartureSelection.ArriveBy -> formatClockTime(hour24, minute)
}

private fun Modifier.tabContentPadding(selectedTab: HomeTab): Modifier = this.padding(horizontal = 16.dp).padding(
    top = if (selectedTab == HomeTab.SETTINGS) 0.dp else 12.dp,
    bottom = 16.dp,
)
