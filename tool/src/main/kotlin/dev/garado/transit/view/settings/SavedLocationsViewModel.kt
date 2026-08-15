package dev.garado.transit.view.settings

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.models.LocationResult
import dev.garado.transit.models.SavedLocation
import dev.garado.transit.data.database.savedlocations.SavedLocationDatabaseHolder
import dev.garado.transit.data.database.savedlocations.SavedLocationStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedLocationsViewModel(lightContext: SealedLightContext) : LightViewModel<Unit>() {
    private val store = SavedLocationStore(SavedLocationDatabaseHolder.get(lightContext))

    val savedLocations: StateFlow<List<SavedLocation>> =
        store.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var savedScrollOffset: Int = 0

    fun add(displayName: String, result: LocationResult) {
        viewModelScope.launch { store.add(displayName, result) }
    }

    fun delete(savedLocation: SavedLocation) {
        viewModelScope.launch { store.delete(savedLocation) }
    }
}
