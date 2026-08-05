package dev.garado.transit.search

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchState {
    private val _fromLocation = MutableStateFlow<LocationResult?>(null)
    val fromLocation: StateFlow<LocationResult?> = _fromLocation.asStateFlow()

    private val _toLocation = MutableStateFlow<LocationResult?>(null)
    val toLocation: StateFlow<LocationResult?> = _toLocation.asStateFlow()

    fun setFromLocation(value: LocationResult) {
        _fromLocation.value = value
    }

    fun setToLocation(value: LocationResult) {
        _toLocation.value = value
    }

    fun swapLocations() {
        val from = _fromLocation.value
        _fromLocation.value = _toLocation.value
        _toLocation.value = from
    }
}
