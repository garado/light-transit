package dev.garado.transit.view.search

import dev.garado.transit.search.DepartureSelection
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DepartureMode(val label: String) {
    LEAVE_AT("Leave at"),
    ARRIVE_BY("Arrive by"),
    LEAVE_NOW("Leave now"),
}

class DepartureTimeViewModel(
    initialSelection: DepartureSelection,
    val initialTime: TimeSelection,
) : LightViewModel<DepartureSelection>() {
    private val _mode = MutableStateFlow(initialSelection.toMode())
    val mode: StateFlow<DepartureMode> = _mode.asStateFlow()

    fun selectMode(value: DepartureMode) {
        _mode.value = value
    }
}

private fun DepartureSelection.toMode(): DepartureMode = when (this) {
    DepartureSelection.Now -> DepartureMode.LEAVE_NOW
    is DepartureSelection.LeaveAt -> DepartureMode.LEAVE_AT
    is DepartureSelection.ArriveBy -> DepartureMode.ARRIVE_BY
}
