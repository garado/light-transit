package dev.garado.transit.view.map

import dev.garado.transit.models.LatLon
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.data.settings.DEFAULT_LOCATION_FALLBACK
import dev.garado.transit.data.settings.DefaultLocationStore
import dev.garado.transit.interfaces.nearbyroutes.NearbyRoutesInterface
import dev.garado.transit.models.TripRoute
import dev.garado.transit.interfaces.nearbyroutes.GtfsLocalNearbyRoutesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class NearbyRoutesViewMode { MAP, LIST }

class NearbyRoutesViewModel(
    lightContext: SealedLightContext,
    private val localRoutesProvider: NearbyRoutesInterface = GtfsLocalNearbyRoutesProvider(lightContext),
) : LightViewModel<Unit>() {
    private val _routes = MutableStateFlow<List<TripRoute>>(emptyList())
    val routes: StateFlow<List<TripRoute>> = _routes.asStateFlow()

    var savedScrollOffset: Int = 0

    /** Whether [search] has completed at least once */
    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    /** Whether a [search] call is currently in flight */
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _viewMode = MutableStateFlow(NearbyRoutesViewMode.MAP)
    val viewMode: StateFlow<NearbyRoutesViewMode> = _viewMode.asStateFlow()

    private val fallbackCenter = LatLon(lat = DEFAULT_LOCATION_FALLBACK.lat, lon = DEFAULT_LOCATION_FALLBACK.lon)

    /** Where the map is actually centered right now */
    private val _liveCenter = MutableStateFlow(fallbackCenter)
    val liveCenter: StateFlow<LatLon> = _liveCenter.asStateFlow()

    /** Where the map should est a pin (set by the search icon jumping somewhere or by [search] itself) */
    private val _searchedCenter = MutableStateFlow<LatLon?>(null)
    val searchedCenter: StateFlow<LatLon?> = _searchedCenter.asStateFlow()

    private val _defaultCenter = MutableStateFlow(fallbackCenter)
    val defaultCenter: StateFlow<LatLon> = _defaultCenter.asStateFlow()

    init {
        viewModelScope.launch {
            val default = DefaultLocationStore(lightContext.dataStore).location.first()
            val latLon = LatLon(lat = default.lat, lon = default.lon)
            _defaultCenter.value = latLon
            if (_liveCenter.value == fallbackCenter) _liveCenter.value = latLon
        }
    }

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
        savedScrollOffset = 0
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
