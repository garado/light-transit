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

enum class NearbyRoutesViewMode { MAP, LIST }

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

    private val _viewMode = MutableStateFlow(NearbyRoutesViewMode.MAP)
    val viewMode: StateFlow<NearbyRoutesViewMode> = _viewMode.asStateFlow()

    /** Where the map is actually centered right now */
    private val _liveCenter = MutableStateFlow(DEMO_LOCATION)
    val liveCenter: StateFlow<LatLon> = _liveCenter.asStateFlow()

    /** Where the map should est a pin (set by the search icon jumping somewhere or by [search] itself) */
    private val _searchedCenter = MutableStateFlow<LatLon?>(null)
    val searchedCenter: StateFlow<LatLon?> = _searchedCenter.asStateFlow()

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            NearbyRoutesViewMode.MAP -> NearbyRoutesViewMode.LIST
            NearbyRoutesViewMode.LIST -> NearbyRoutesViewMode.MAP
        }
    }

    fun onMapCenterChanged(location: LatLon) {
        _liveCenter.value = location
    }

    fun jumpTo(location: LatLon) {
        _searchedCenter.value = location
        _liveCenter.value = location
        _viewMode.value = NearbyRoutesViewMode.MAP
        _routes.value = emptyList()
        _hasSearched.value = false
    }

    fun search() {
        if (_isSearching.value || _hasSearched.value) return
        val location = _liveCenter.value
        viewModelScope.launch {
            _isSearching.value = true
            try {
                _routes.value = localRoutesProvider.nearbyRoutes(location.lat, location.lon)
                _searchedCenter.value = location
                _hasSearched.value = true
                _viewMode.value = NearbyRoutesViewMode.LIST
            } finally {
                _isSearching.value = false
            }
        }
    }
}
