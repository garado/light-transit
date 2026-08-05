package dev.garado.transit.settings

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.search.LocationResult
import dev.garado.transit.search.SavedLocation
import dev.garado.transit.search.SavedLocationDatabaseHolder
import dev.garado.transit.search.SavedLocationStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedLocationsViewModel(lightContext: SealedLightContext) : LightViewModel<Unit>() {
    private val store = SavedLocationStore(SavedLocationDatabaseHolder.get(lightContext))

    val savedLocations: StateFlow<List<SavedLocation>> =
        store.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(displayName: String, result: LocationResult) {
        viewModelScope.launch { store.add(displayName, result) }
    }

    fun delete(savedLocation: SavedLocation) {
        viewModelScope.launch { store.delete(savedLocation) }
    }
}
