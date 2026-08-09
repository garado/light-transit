package dev.garado.transit.gtfs

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GtfsBrowserViewModel(
    lightContext: SealedLightContext,
    private val fetcher: TransitousFeedFetcher = TransitousFeedFetcher(),
) : LightViewModel<List<GtfsDataset>>() {
    private val dao = GtfsCatalogDatabaseHolder.get(lightContext).gtfsCatalogDao()

    /**
     * The GTFS source structure is fetched from Transitous as a YAML file.
     * It's given as a flat list. Parsing it takes a while, so on fetch, this is
     * parsed and reorganized into a db where feeds are sorted by country -> region.
     * Future calls read from the DB, unless a refresh is explicitly requested by
     * the user.
     */
    val datasetsByRegion: StateFlow<Map<String, List<GtfsDataset>>> = dao.getAll()
        .map { entities -> entities.map { it.toGtfsDataset() }.groupBy { it.regionCode }.toSortedMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** True only while the catalog has never been populated at all. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** True while a forced refresh (fetch + parse) is in flight. */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            if (dao.count() == 0) {
                refresh()
            } else {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val (yaml, index) = coroutineScope {
                val yamlDeferred = async { fetcher.fetchRawConfig() }
                val indexDeferred = async { fetcher.fetchRawIndex() }
                yamlDeferred.await() to indexDeferred.await()
            }
            if (yaml != null && index != null) {
                val datasets = withContext(Dispatchers.Default) { fetcher.parseDatasets(yaml, index) }
                dao.replaceAll(datasets.map { it.toEntity() })
            }
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }
}
