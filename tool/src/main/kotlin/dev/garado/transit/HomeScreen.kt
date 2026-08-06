package dev.garado.transit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import dev.garado.transit.map.MapTabContent
import dev.garado.transit.map.TileCacheDatabase
import dev.garado.transit.route.RouteSelectScreen
import dev.garado.transit.search.DepartureSelection
import dev.garado.transit.search.DepartureTimeScreen
import dev.garado.transit.search.LocationSearchScreen
import dev.garado.transit.search.SearchTabContent
import dev.garado.transit.settings.ApiSettingsScreen
import dev.garado.transit.settings.NameEditor
import dev.garado.transit.settings.SavedLocationsScreen
import dev.garado.transit.settings.SettingsTabContent

enum class HomeTab { SEARCH, MAP, SETTINGS }

@InitialScreen
class HomeScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, HomeScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<HomeScreenViewModel>
        get() = HomeScreenViewModel::class.java

    override fun createViewModel(): HomeScreenViewModel {
        return HomeScreenViewModel()
    }

    @Composable
    override fun Content() {
        val selectedTab by viewModel.selectedTab.collectAsState()
        val settingsOptions by viewModel.settings.settingsOptions.collectAsState()
        val displayName by viewModel.settings.displayName.collectAsState()
        val fromLocation by viewModel.search.fromLocation.collectAsState()
        val toLocation by viewModel.search.toLocation.collectAsState()
        val fromDisplay = fromLocation?.let { it.displayName ?: it.title } ?: ""
        val toDisplay = toLocation?.let { it.displayName ?: it.title } ?: ""
        val departureSelection by viewModel.search.departureSelection.collectAsState()
        val departureTime by viewModel.search.departureTime.collectAsState()
        val isEditingName by viewModel.settings.isEditingName.collectAsState()
        val editSessionId by viewModel.settings.editSessionId.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()
        val tileCacheDatabase = remember {
            lightContext.buildDatabase(TileCacheDatabase::class.java, "tile_cache.db")
        }
        DisposableEffect(tileCacheDatabase) {
            onDispose { tileCacheDatabase.close() }
        }

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
                    if (selectedTab == HomeTab.SEARCH) {
                        StatusBar()
                    }

                    if (selectedTab != HomeTab.SEARCH) {
                        LightTopBar(
                            leftButton = LightBarButton.LightIcon(
                                icon = LightIcons.BACK,
                                onClick = { viewModel.selectTab(HomeTab.SEARCH) },
                            ),
                            center = LightTopBarCenter.Text(
                                if (selectedTab == HomeTab.SETTINGS) "Settings" else "Map",
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
                            HomeTab.MAP -> MapTabContent(
                                isDarkTheme = LightThemeController.isDarkTheme,
                                database = tileCacheDatabase,
                            )
                            HomeTab.SETTINGS -> SettingsTabContent(
                                options = settingsOptions,
                                displayName = displayName,
                                onToggle = viewModel.settings::toggleSetting,
                                onAboutClick = { navigateTo(::AboutScreen) },
                                onSavedLocationsClick = { navigateTo(::SavedLocationsScreen) },
                                onApiSettingsClick = { navigateTo(::ApiSettingsScreen) },
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
                            onMapClick = { viewModel.selectTab(HomeTab.MAP) },
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

private fun Modifier.tabContentPadding(selectedTab: HomeTab): Modifier =
    if (selectedTab == HomeTab.MAP) {
        this
    } else {
        this.padding(horizontal = 32.dp).padding(
            top = if (selectedTab == HomeTab.SETTINGS) 0.dp else 12.dp,
            bottom = 16.dp,
        )
    }
