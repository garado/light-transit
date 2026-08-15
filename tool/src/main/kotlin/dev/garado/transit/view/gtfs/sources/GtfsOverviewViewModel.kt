package dev.garado.transit.view.gtfs.sources

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.data.gtfs.local.GtfsDatabaseHolder
import dev.garado.transit.data.gtfs.sources.GtfsSource
import dev.garado.transit.data.gtfs.sources.GtfsSourceStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GtfsOverviewViewModel(lightContext: SealedLightContext) : LightViewModel<Unit>() {
    private val store = GtfsSourceStore(GtfsDatabaseHolder.get(lightContext), lightContext.filesDir)

    val sources: StateFlow<List<GtfsSource>> = store.all
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Total disk space used by downloaded .gtfs.zip files + the imported local schedule database */
    private val _spaceUsedBytes = MutableStateFlow<Long?>(null)
    val spaceUsedBytes: StateFlow<Long?> = _spaceUsedBytes.asStateFlow()

    init {
        viewModelScope.launch {
            _spaceUsedBytes.value = withContext(Dispatchers.IO) {
                val zipBytes = File(lightContext.filesDir, "gtfs").listFiles()?.sumOf { it.length() } ?: 0L
                // Room's default database directory: <app-private-data-dir>/databases/
                val dbDir = File(lightContext.filesDir.parentFile, "databases")
                val dbBytes = listOf("gtfs.db", "gtfs.db-wal", "gtfs.db-shm")
                    .sumOf { File(dbDir, it).let { f -> if (f.exists()) f.length() else 0L } }
                zipBytes + dbBytes
            }
        }
    }
}
