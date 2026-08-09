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

    fun addAll(datasets: List<GtfsDataset>) {
        viewModelScope.launch { datasets.forEach { store.add(it) } }
    }

    fun deleteAll(sources: List<GtfsSource>) {
        viewModelScope.launch { store.deleteAll(sources) }
    }
}
