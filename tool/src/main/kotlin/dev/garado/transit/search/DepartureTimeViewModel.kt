package dev.garado.transit.search

import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DepartureMode(val label: String) {
    LEAVE_AT("Leave at"),
    ARRIVE_BY("Arrive by"),
    LEAVE_NOW("Leave now"),
}

class DepartureTimeViewModel : LightViewModel<Unit>() {
    private val _mode = MutableStateFlow(DepartureMode.LEAVE_AT)
    val mode: StateFlow<DepartureMode> = _mode.asStateFlow()

    private val _selectedTime = MutableStateFlow<TimeSelection?>(null)
    val selectedTime: StateFlow<TimeSelection?> = _selectedTime.asStateFlow()

    fun selectMode(value: DepartureMode) {
        _mode.value = value
    }

    fun setSelectedTime(value: TimeSelection) {
        _selectedTime.value = value
    }
}
