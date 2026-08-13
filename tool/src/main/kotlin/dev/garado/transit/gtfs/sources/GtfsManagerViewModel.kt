package dev.garado.transit.gtfs.sources

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.gtfs.GtfsDataset
import dev.garado.transit.gtfs.local.GtfsDatabaseHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GtfsManagerViewModel(lightContext: SealedLightContext) : LightViewModel<Unit>() {
    private val store = GtfsSourceStore(
        GtfsDatabaseHolder.get(lightContext),
        lightContext.filesDir,
    )

    /** True only until sources below has produced its first result. */
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded: StateFlow<Boolean> = _hasLoaded.asStateFlow()

    val sources: StateFlow<List<GtfsSource>> = store.all
        .onEach { _hasLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Downloads gtfs feeds sequentially */
    fun addAll(datasets: List<GtfsDataset>) {
        viewModelScope.launch { datasets.forEach { store.add(it) } }
    }

    fun deleteAll(sources: List<GtfsSource>) {
        viewModelScope.launch { store.deleteAll(sources) }
    }
}
