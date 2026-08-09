package dev.garado.transit.gtfs

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GtfsBrowserViewModel(
    lightContext: SealedLightContext,
    private val fetcher: TransitousFeedFetcher = TransitousFeedFetcher(),
) : LightViewModel<List<GtfsDataset>>() {
    private val cache = TransitousCatalogCache(lightContext.dataStore)

    private val _datasetsByRegion = MutableStateFlow<Map<String, List<GtfsDataset>>>(emptyMap())
    val datasetsByRegion: StateFlow<Map<String, List<GtfsDataset>>> = _datasetsByRegion.asStateFlow()

    /** True only on first load (w/ no cache) */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** true while actively refreshing */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = cache.cached.first()
            if (cached != null) {
                val (yaml, index) = cached
                applyYaml(yaml, index)
                _isLoading.value = false
            } else {
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val yaml = fetcher.fetchRawConfig()
            val index = fetcher.fetchRawIndex()
            if (yaml != null && index != null) {
                applyYaml(yaml, index)
                cache.save(yaml, index)
            }
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    private fun applyYaml(yaml: String, index: String) {
        _datasetsByRegion.value = fetcher.parseDatasets(yaml, index).groupBy { it.regionCode }.toSortedMap()
    }
}
