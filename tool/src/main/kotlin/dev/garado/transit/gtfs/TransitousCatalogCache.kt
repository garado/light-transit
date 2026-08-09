package dev.garado.transit.gtfs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Disk cache for Transitous's raw config.yml */
class TransitousCatalogCache(private val dataStore: DataStore<Preferences>) {

    val cachedYaml: Flow<String?> = dataStore.data.map { prefs -> prefs[CACHED_YAML_KEY] }

    suspend fun save(yaml: String) {
        dataStore.edit { prefs -> prefs[CACHED_YAML_KEY] = yaml }
    }

    private companion object {
        val CACHED_YAML_KEY = stringPreferencesKey("transitous_config_yaml")
    }
}
