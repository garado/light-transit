package dev.garado.transit.map

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.api.NearbyRoutesProvider
import dev.garado.transit.api.models.TripRoute
import dev.garado.transit.gtfs.local.GtfsLocalNearbyRoutesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NearbyRoutesViewModel(
    lightContext: SealedLightContext,
    private val localRoutesProvider: NearbyRoutesProvider = GtfsLocalNearbyRoutesProvider(lightContext),
) : LightViewModel<Unit>() {
    private val _routes = MutableStateFlow<List<TripRoute>>(emptyList())
    val routes: StateFlow<List<TripRoute>> = _routes.asStateFlow()

    /** Whether [search] has completed at least once */
    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    /** Whether a [search] call is currently in flight */
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun search() {
        if (_isSearching.value || _hasSearched.value) return
        val location = DEMO_LOCATION
        viewModelScope.launch {
            _isSearching.value = true
            try {
                _routes.value = localRoutesProvider.nearbyRoutes(location.lat, location.lon)
                _hasSearched.value = true
            } finally {
                _isSearching.value = false
            }
        }
    }
}
