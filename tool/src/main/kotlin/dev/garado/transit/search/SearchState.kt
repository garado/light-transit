package dev.garado.transit.search

import dev.garado.transit.MinuteTimer
import dev.garado.transit.view.search.TimeSelection
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchState {
    private val _fromLocation = MutableStateFlow<LocationResult?>(null)
    val fromLocation: StateFlow<LocationResult?> = _fromLocation.asStateFlow()

    private val _toLocation = MutableStateFlow<LocationResult?>(null)
    val toLocation: StateFlow<LocationResult?> = _toLocation.asStateFlow()

    private val _departureSelection = MutableStateFlow<DepartureSelection>(DepartureSelection.Now)
    val departureSelection: StateFlow<DepartureSelection> = _departureSelection.asStateFlow()

    // single source of truth for currently configured departure time
    // - reset to current value on startup or whenever "leave now" is picked
    private val _departureTime = MutableStateFlow(currentTimeSelection())
    val departureTime: StateFlow<TimeSelection> = _departureTime.asStateFlow()

    fun setFromLocation(value: LocationResult) {
        _fromLocation.value = value
    }

    fun setToLocation(value: LocationResult) {
        _toLocation.value = value
    }

    fun setDepartureSelection(value: DepartureSelection) {
        _departureSelection.value = value
        _departureTime.value = when (value) {
            is DepartureSelection.LeaveAt -> TimeSelection(value.hour24, value.minute)
            is DepartureSelection.ArriveBy -> TimeSelection(value.hour24, value.minute)
            DepartureSelection.Now -> currentTimeSelection()
        }
    }

    fun swapLocations() {
        val from = _fromLocation.value
        _fromLocation.value = _toLocation.value
        _toLocation.value = from
    }
}

private fun currentTimeSelection(): TimeSelection {
    val zoned = Instant.ofEpochSecond(MinuteTimer.currentTimeSeconds.value).atZone(ZoneId.systemDefault())
    return TimeSelection(hour24 = zoned.hour, minute = zoned.minute)
}
