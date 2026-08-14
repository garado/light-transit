package dev.garado.transit.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Whether the app should query the user's live location */
class UserLocationSettings(private val dataStore: DataStore<Preferences>) {

    val liveLocationEnabled: Flow<Boolean> = dataStore.data.map { prefs -> prefs[LIVE_LOCATION_ENABLED_KEY] ?: false }

    suspend fun setLiveLocationEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[LIVE_LOCATION_ENABLED_KEY] = enabled }
    }

    private companion object {
        val LIVE_LOCATION_ENABLED_KEY = booleanPreferencesKey("live_location_enabled")
    }
}
