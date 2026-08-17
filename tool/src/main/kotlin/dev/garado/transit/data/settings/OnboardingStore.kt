package dev.garado.transit.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Whether the user has finished the initial onboarding flow */
class OnboardingStore(private val dataStore: DataStore<Preferences>) {

    val complete: Flow<Boolean> = dataStore.data.map { prefs -> prefs[ONBOARDING_COMPLETE_KEY] ?: true }

    suspend fun setComplete(complete: Boolean) {
        dataStore.edit { prefs -> prefs[ONBOARDING_COMPLETE_KEY] = complete }
    }

    private companion object {
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
    }
}
