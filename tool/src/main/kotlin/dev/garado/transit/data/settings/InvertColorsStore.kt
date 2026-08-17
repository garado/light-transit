package dev.garado.transit.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Determine color of map tiles (dark by default) */
class InvertColorsStore(private val dataStore: DataStore<Preferences>) {

    val enabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[INVERT_COLORS_KEY] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[INVERT_COLORS_KEY] = enabled }
    }

    private companion object {
        val INVERT_COLORS_KEY = booleanPreferencesKey("invert_map_colors")
    }
}
