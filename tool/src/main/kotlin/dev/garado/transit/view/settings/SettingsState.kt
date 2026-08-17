package dev.garado.transit.view.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsOption(val label: String, val enabled: Boolean)

class SettingsState {
    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _isEditingName = MutableStateFlow(false)
    val isEditingName: StateFlow<Boolean> = _isEditingName.asStateFlow()

    // LightTextInputEditor caches its embedded keyboard's ViewModel by editorKey;
    // bump this each time editing starts so a stale keyboard/TextFieldState pairing
    // from a previous session isnt reused
    private val _editSessionId = MutableStateFlow(0)
    val editSessionId: StateFlow<Int> = _editSessionId.asStateFlow()

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
