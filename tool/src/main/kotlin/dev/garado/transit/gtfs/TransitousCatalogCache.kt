package dev.garado.transit.gtfs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Disk cache for Transitous's raw config.yml + directory index, so browsing doesn't re-download every time. */
class TransitousCatalogCache(private val dataStore: DataStore<Preferences>) {

    private val cachedYaml: Flow<String?> = dataStore.data.map { prefs -> prefs[CACHED_YAML_KEY] }
    private val cachedIndex: Flow<String?> = dataStore.data.map { prefs -> prefs[CACHED_INDEX_KEY] }

    /** Non-null only once both pieces are cached. */
    val cached: Flow<Pair<String, String>?> = cachedYaml.combine(cachedIndex) { yaml, index ->
        if (yaml != null && index != null) yaml to index else null
    }

    suspend fun save(yaml: String, index: String) {
        dataStore.edit { prefs ->
            prefs[CACHED_YAML_KEY] = yaml
            prefs[CACHED_INDEX_KEY] = index
        }
    }

    private companion object {
        val CACHED_YAML_KEY = stringPreferencesKey("transitous_config_yaml")
        val CACHED_INDEX_KEY = stringPreferencesKey("transitous_gtfs_index_html")
    }
}
