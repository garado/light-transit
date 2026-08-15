package dev.garado.transit.view.route

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.api.models.TripPlan
import dev.garado.transit.interfaces.plan.PlanProvider
import dev.garado.transit.interfaces.plan.TransitApiPlanProvider
import dev.garado.transit.search.DepartureSelection
import dev.garado.transit.search.LocationResult
import dev.garado.transit.search.toApiTimeParams
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
    departureSelection: DepartureSelection,
    private val planProvider: PlanProvider = TransitApiPlanProvider(lightContext),
) : LightViewModel<Unit>() {
    /** Null while loading, empty once loaded with no results. */
    private val _tripPlans = MutableStateFlow<List<TripPlan>?>(null)
    val tripPlans: StateFlow<List<TripPlan>?> = _tripPlans.asStateFlow()

    init {
        viewModelScope.launch {
            val (leaveTime, arrivalTime) = departureSelection.toApiTimeParams()
            _tripPlans.value = planProvider.plan(
                fromLat = from.lat,
                fromLon = from.lon,
                toLat = to.lat,
                toLon = to.lon,
                leaveTime = leaveTime,
                arrivalTime = arrivalTime,
            )
        }
    }
}
