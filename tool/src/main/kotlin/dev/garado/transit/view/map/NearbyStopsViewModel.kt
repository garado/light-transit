package dev.garado.transit.view.map

import dev.garado.transit.models.LatLon
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.models.StopDeparture
import dev.garado.transit.models.TripStop
import dev.garado.transit.interfaces.nearbystops.GtfsLocalNearbyStopsProvider
import dev.garado.transit.interfaces.stopdepartures.GtfsLocalStopDeparturesProvider
import dev.garado.transit.interfaces.nearbystops.NearbyStopsInterface
import dev.garado.transit.interfaces.nearbystops.TransitApiNearbyStopsProvider
import dev.garado.transit.interfaces.stopdepartures.StopDeparturesInterface
import dev.garado.transit.interfaces.stopdepartures.TransitApiStopDeparturesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// TODO
internal val DEMO_LOCATION = LatLon(lat = 37.8288, lon = -122.2673)

enum class NearbyStopsViewMode { MAP, LIST }

/** A stops backend and its matching departures backend */
private data class StopsBackend(val stops: NearbyStopsInterface, val departures: StopDeparturesInterface)

class NearbyStopsViewModel(
    lightContext: SealedLightContext,
    private val backends: List<StopsBackend> = listOf(
        StopsBackend(GtfsLocalNearbyStopsProvider(lightContext), GtfsLocalStopDeparturesProvider(lightContext)),
        StopsBackend(TransitApiNearbyStopsProvider(lightContext), TransitApiStopDeparturesProvider(lightContext)),
    ),
) : LightViewModel<Unit>() {
    private val _stops = MutableStateFlow<List<TripStop>>(emptyList())
    val stops: StateFlow<List<TripStop>> = _stops.asStateFlow()

    /** Whether [search] has completed at least once */
    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    /** Whether a [search] call is currently in flight */
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _departuresByStop = MutableStateFlow<Map<String, List<StopDeparture>>>(emptyMap())
    val departuresByStop: StateFlow<Map<String, List<StopDeparture>>> = _departuresByStop.asStateFlow()

    private val _viewMode = MutableStateFlow(NearbyStopsViewMode.MAP)
    val viewMode: StateFlow<NearbyStopsViewMode> = _viewMode.asStateFlow()

    /** Where the map is actually centered right now */
    private val _liveCenter = MutableStateFlow(DEMO_LOCATION)
    val liveCenter: StateFlow<LatLon> = _liveCenter.asStateFlow()

    /** Set only when the header's search icon should move the map somewhere */
    private val _searchedCenter = MutableStateFlow<LatLon?>(null)
    val searchedCenter: StateFlow<LatLon?> = _searchedCenter.asStateFlow()

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            NearbyStopsViewMode.MAP -> NearbyStopsViewMode.LIST
            NearbyStopsViewMode.LIST -> NearbyStopsViewMode.MAP
        }
    }

    fun onMapCenterChanged(location: LatLon) {
        _liveCenter.value = location
    }

    fun jumpTo(location: LatLon) {
        _searchedCenter.value = location
        _liveCenter.value = location
        _viewMode.value = NearbyStopsViewMode.MAP
        _stops.value = emptyList()
        _departuresByStop.value = emptyMap()
        _hasSearched.value = false
    }

    /** Fetch NearbyStops and StopDepartures centered on wherever the map is now */
    fun search() {
        if (_isSearching.value) return
        val location = _liveCenter.value
        viewModelScope.launch {
            _isSearching.value = true
            try {
                var stops = emptyList<TripStop>()
                var departuresProvider: StopDeparturesInterface? = null
                for (backend in backends) {
                    stops = backend.stops.nearbyStops(location.lat, location.lon)
                    if (stops.isNotEmpty()) {
                        departuresProvider = backend.departures
                        break
                    }
                }
                _stops.value = stops
                _departuresByStop.value = departuresProvider?.departures(stops.flatMap { it.groupedStopIds }) ?: emptyMap()
                _hasSearched.value = true
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun departuresFor(stop: TripStop): List<StopDeparture> =
        stop.groupedStopIds
            .flatMap { departuresByStop.value[it].orEmpty() }
            .sortedBy { it.departureTime }
}
