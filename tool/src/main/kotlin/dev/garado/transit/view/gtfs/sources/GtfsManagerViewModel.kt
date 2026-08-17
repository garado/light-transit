package dev.garado.transit.view.gtfs.sources

import dev.garado.transit.data.gtfs.sources.GtfsSource
import dev.garado.transit.data.gtfs.sources.GtfsSourceStore
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.data.gtfs.local.GtfsDatabaseHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class GtfsManagerViewModel(lightContext: SealedLightContext) : LightViewModel<Unit>() {
    private val store = GtfsSourceStore(
        GtfsDatabaseHolder.get(lightContext),
        lightContext.filesDir,
    )

    /** True only until sources below has produced its first result. */
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded: StateFlow<Boolean> = _hasLoaded.asStateFlow()

    var savedScrollOffset: Int = 0

    val sources: StateFlow<List<GtfsSource>> = store.all
        .onEach { _hasLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun deleteAll(sources: List<GtfsSource>) {
        store.deleteAllDetached(sources)
    }
}
