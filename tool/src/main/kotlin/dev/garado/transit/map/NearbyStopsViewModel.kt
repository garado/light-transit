package dev.garado.transit.map

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.api.models.StopDeparture
import dev.garado.transit.api.models.TripStop
import dev.garado.transit.api.transit.TransitApiPlanProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// TODO
internal val DEMO_LOCATION = LatLon(lat = 37.8288, lon = -122.2673)

enum class NearbyStopsViewMode { MAP, LIST }

class NearbyStopsViewModel(
    lightContext: SealedLightContext,
    private val transitApiProvider: TransitApiPlanProvider = TransitApiPlanProvider(lightContext),
) : LightViewModel<Unit>() {
    private val _stops = MutableStateFlow<List<TripStop>>(emptyList())
    val stops: StateFlow<List<TripStop>> = _stops.asStateFlow()

    private val _departuresByStop = MutableStateFlow<Map<String, List<StopDeparture>>>(emptyMap())
    val departuresByStop: StateFlow<Map<String, List<StopDeparture>>> = _departuresByStop.asStateFlow()

    private val _viewMode = MutableStateFlow(NearbyStopsViewMode.MAP)
    val viewMode: StateFlow<NearbyStopsViewMode> = _viewMode.asStateFlow()

    init {
        viewModelScope.launch {
            val nearby = transitApiProvider.nearbyStops(DEMO_LOCATION.lat, DEMO_LOCATION.lon)
            _stops.value = nearby
            _departuresByStop.value = transitApiProvider.departures(nearby.map { it.globalStopId })
        }
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            NearbyStopsViewMode.MAP -> NearbyStopsViewMode.LIST
            NearbyStopsViewMode.LIST -> NearbyStopsViewMode.MAP
        }
    }

    fun departuresFor(stop: TripStop): List<StopDeparture> = departuresByStop.value[stop.globalStopId] ?: emptyList()
}
