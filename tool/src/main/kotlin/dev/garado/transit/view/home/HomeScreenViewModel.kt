package dev.garado.transit.view.home

import com.thelightphone.sdk.LightViewModel
import dev.garado.transit.view.search.SearchState
import dev.garado.transit.view.settings.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeScreenViewModel : LightViewModel<Unit>() {
    val search = SearchState()
    val settings = SettingsState()

    private val _selectedTab = MutableStateFlow(HomeTab.SEARCH)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }
}
