package dev.garado.transit.gtfs

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GtfsManagerViewModel(lightContext: SealedLightContext) : LightViewModel<Unit>() {
    private val store = GtfsSourceStore(GtfsSourceDatabaseHolder.get(lightContext))

    val sources: StateFlow<List<GtfsSource>> =
        store.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(dataset: GtfsDataset) {
        viewModelScope.launch { store.add(dataset) }
    }

    fun delete(source: GtfsSource) {
        viewModelScope.launch { store.delete(source) }
    }
}
