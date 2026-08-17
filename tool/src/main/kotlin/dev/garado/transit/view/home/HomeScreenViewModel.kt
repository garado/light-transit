package dev.garado.transit.view.home

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.data.settings.DEFAULT_LOCATION_FALLBACK
import dev.garado.transit.data.settings.DefaultLocationStore
import dev.garado.transit.data.settings.InvertColorsStore
import dev.garado.transit.models.LocationResult
import dev.garado.transit.view.search.SearchState
import dev.garado.transit.view.settings.SettingsOption
import dev.garado.transit.view.settings.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val INVERT_COLORS_LABEL = "Invert Colors"

class HomeScreenViewModel(lightContext: SealedLightContext) : LightViewModel<Unit>() {
    val search = SearchState()
    val settings = SettingsState()

    private val defaultLocationStore = DefaultLocationStore(lightContext.dataStore)
    val defaultLocation: StateFlow<LocationResult> = defaultLocationStore.location
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_LOCATION_FALLBACK)

    private val invertColorsStore = InvertColorsStore(lightContext.dataStore)
    val settingsOptions: StateFlow<List<SettingsOption>> = invertColorsStore.enabled
        .map { enabled -> listOf(SettingsOption(INVERT_COLORS_LABEL, enabled)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(SettingsOption(INVERT_COLORS_LABEL, enabled = false)))

    private val _selectedTab = MutableStateFlow(HomeTab.SEARCH)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = defaultLocationStore.location.first()
            if (search.fromLocation.value == null) search.setFromLocation(initial)
        }
    }

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    fun setDefaultLocation(result: LocationResult) {
        viewModelScope.launch { defaultLocationStore.set(result) }
    }

    fun toggleSetting(label: String) {
        if (label != INVERT_COLORS_LABEL) return
        viewModelScope.launch { invertColorsStore.setEnabled(!invertColorsStore.enabled.first()) }
    }
}
