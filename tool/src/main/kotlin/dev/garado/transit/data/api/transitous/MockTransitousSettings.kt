package dev.garado.transit.data.api.transitous

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Whether Transitous calls should use cached test results instead of real API results */
class MockTransitousSettings(private val dataStore: DataStore<Preferences>) {

    val useMockedResponses: Flow<Boolean> = dataStore.data.map { prefs -> prefs[USE_MOCKED_RESPONSES_KEY] ?: false }

    suspend fun setUseMockedResponses(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[USE_MOCKED_RESPONSES_KEY] = enabled }
    }

    /** Force every API call to fail immediately without being sent (mock or real) */
    val simulateApiFailure: Flow<Boolean> = dataStore.data.map { prefs -> prefs[SIMULATE_API_FAILURE_KEY] ?: false }

    suspend fun setSimulateApiFailure(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SIMULATE_API_FAILURE_KEY] = enabled }
    }

    private companion object {
        val USE_MOCKED_RESPONSES_KEY = booleanPreferencesKey("use_mocked_transitous_responses")
        val SIMULATE_API_FAILURE_KEY = booleanPreferencesKey("simulate_transitous_failure")
    }
}
