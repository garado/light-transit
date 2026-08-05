package dev.garado.transit.route

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.api.transit.TransitClient
import dev.garado.transit.api.transit.models.TripPlan
import dev.garado.transit.search.LocationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Fetch trip plans
 * - Trip plans stored for lifetime of this screen
 */
class RouteSelectViewModel(
    lightContext: SealedLightContext,
    from: LocationResult,
    to: LocationResult,
) : LightViewModel<Unit>() {
    private val transitClient = TransitClient(lightContext)

    /** Null while loading, empty once loaded with no results. */
    private val _tripPlans = MutableStateFlow<List<TripPlan>?>(null)
    val tripPlans: StateFlow<List<TripPlan>?> = _tripPlans.asStateFlow()

    init {
        viewModelScope.launch {
            _tripPlans.value = transitClient.plan(
                fromLat = from.lat,
                fromLon = from.lon,
                toLat = to.lat,
                toLon = to.lon,
            )
        }
    }
}
