package dev.garado.transit

import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsOption(val label: String, val enabled: Boolean)

class HomeScreenViewModel : LightViewModel<Unit>() {
    private val _selectedTab = MutableStateFlow(HomeTab.SEARCH)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    private val _settingsOptions = MutableStateFlow(
        listOf(
            SettingsOption("Invert Colors", enabled = false),
        )
    )
    val settingsOptions: StateFlow<List<SettingsOption>> = _settingsOptions.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _fromLocation = MutableStateFlow("19th St BART Station")
    val fromLocation: StateFlow<String> = _fromLocation.asStateFlow()

    private val _toLocation = MutableStateFlow("")
    val toLocation: StateFlow<String> = _toLocation.asStateFlow()

    fun swapLocations() {
        val from = _fromLocation.value
        _fromLocation.value = _toLocation.value
        _toLocation.value = from
    }

    fun setFromLocation(value: String) {
        _fromLocation.value = value
    }

    fun setToLocation(value: String) {
        _toLocation.value = value
    }

    private val _isEditingName = MutableStateFlow(false)
    val isEditingName: StateFlow<Boolean> = _isEditingName.asStateFlow()

    // LightTextInputEditor caches its embedded keyboard's ViewModel by editorKey;
    // bump this each time editing starts so a stale keyboard/TextFieldState pairing
    // from a previous session isnt reused
    private val _editSessionId = MutableStateFlow(0)
    val editSessionId: StateFlow<Int> = _editSessionId.asStateFlow()

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    fun toggleSetting(label: String) {
        _settingsOptions.value = _settingsOptions.value.map {
            if (it.label == label) it.copy(enabled = !it.enabled) else it
        }
    }

    fun startEditingName() {
        _editSessionId.value += 1
        _isEditingName.value = true
    }

    fun submitName(value: CharSequence) {
        _displayName.value = value.toString()
        _isEditingName.value = false
    }

    fun cancelEditingName() {
        _isEditingName.value = false
    }
}
