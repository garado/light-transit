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
) : LightViewModel<GtfsDataset>() {
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
            val cached = cache.cachedYaml.first()
            if (cached != null) {
                applyYaml(cached)
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
            if (yaml != null) {
                applyYaml(yaml)
                cache.save(yaml)
            }
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    private fun applyYaml(yaml: String) {
        _datasetsByRegion.value = fetcher.parseDatasets(yaml).groupBy { it.regionCode }.toSortedMap()
    }
}
