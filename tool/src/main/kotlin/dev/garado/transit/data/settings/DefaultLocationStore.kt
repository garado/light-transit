package dev.garado.transit.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.garado.transit.models.LocationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val DEFAULT_LOCATION_FALLBACK = LocationResult(title = "Oakland, CA", address = "", lat = 37.8288, lon = -122.2580)

/** User-configured location used to center maps/searches when no GPS is available */
class DefaultLocationStore(private val dataStore: DataStore<Preferences>) {

    val location: Flow<LocationResult> = dataStore.data.map { prefs ->
        val lat = prefs[LAT_KEY] ?: return@map DEFAULT_LOCATION_FALLBACK
        val lon = prefs[LON_KEY] ?: return@map DEFAULT_LOCATION_FALLBACK
        val title = prefs[TITLE_KEY] ?: return@map DEFAULT_LOCATION_FALLBACK
        LocationResult(
            title = title,
            address = prefs[ADDRESS_KEY] ?: "",
            lat = lat,
            lon = lon,
            displayName = prefs[DISPLAY_NAME_KEY],
        )
    }

    suspend fun set(result: LocationResult) {
        dataStore.edit { prefs ->
            prefs[LAT_KEY] = result.lat
            prefs[LON_KEY] = result.lon
            prefs[TITLE_KEY] = result.title
            prefs[ADDRESS_KEY] = result.address
            if (result.displayName != null) prefs[DISPLAY_NAME_KEY] = result.displayName else prefs.remove(DISPLAY_NAME_KEY)
        }
    }

    private companion object {
        val LAT_KEY = doublePreferencesKey("default_location_lat")
        val LON_KEY = doublePreferencesKey("default_location_lon")
        val TITLE_KEY = stringPreferencesKey("default_location_title")
        val ADDRESS_KEY = stringPreferencesKey("default_location_address")
        val DISPLAY_NAME_KEY = stringPreferencesKey("default_location_display_name")
    }
}
